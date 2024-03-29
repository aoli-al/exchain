#pragma once

#include <condition_variable>
#include <exception>
#include <future>
#include <memory>
#include <mutex>
#include <queue>
#include <thread>
#include <vector>
#include <chrono>

#include "jni.h"
#include "utils.hpp"
#include "plog/Log.h"

using namespace std::chrono_literals;

namespace exchain {
class ThreadPool {
    int count_ = 10;
    JavaVM *jvm_;
    std::condition_variable task_available_cv_ = {};
    std::condition_variable task_done_cv_ = {};
    mutable std::mutex tasks_mutex = {};
    mutable std::mutex processing_threads_mutex_ = {};

    std::queue<std::function<void(JNIEnv *)>> tasks_ = {};
    std::vector<std::thread> threads_;
    std::set<jlong> processing_threads_;
    std::chrono::time_point<std::chrono::system_clock> last_updated_time_ = std::chrono::system_clock::now();
    bool shutdown_ = false;


   public:
    ThreadPool(int count, JavaVM *jvm) : count_(count), jvm_(jvm) {
        SpawnThreadAndStart();
    }

    void SpawnThreadAndStart() {
        threads_ = std::vector<std::thread>(count_);
        for (int i = 0; i < count_; i++) {
            threads_[i] = std::thread(&ThreadPool::Worker, this);
        }
    }

    void Push(std::function<void(JNIEnv *)> &&task) {
        last_updated_time_ = std::chrono::system_clock::now();
        {
            std::unique_lock<std::mutex> lock(tasks_mutex);
            tasks_.push(task);
        }

        task_available_cv_.notify_one();
    }

    std::future<void> Submit(std::function<void(JNIEnv *)> &&task) {
        std::shared_ptr<std::promise<void>> task_promise =
            std::make_shared<std::promise<void>>();
        Push([task, task_promise](JNIEnv *jni) {
            try {
                task(jni);
                task_promise->set_value();
            } catch (...) {
                try {
                    task_promise->set_exception(std::current_exception());
                } catch (...) {
                }
            }
        });
        return task_promise->get_future();
    }

    bool IsProcessingThread(jlong thread_id) {
        return processing_threads_.find(thread_id) != processing_threads_.end();
    }

    void Shutdown() {
        shutdown_ = true;
        task_available_cv_.notify_all();
        for (auto&& t: threads_) {
            t.join();
        }
    }

   private:
    bool ShouldStopWorker() {
        return last_updated_time_ + 10s < std::chrono::system_clock::now();
    }



    void Worker() {
        JNIEnv *jni;
        if (auto result = jvm_->AttachCurrentThreadAsDaemon((void **)&jni, NULL) != 0) {
            PLOG_ERROR << "Failed to attach current thread: " << result;
            return;
        }

        auto cls = jni->FindClass("java/lang/Thread");
        auto mid = jni->GetStaticMethodID(cls, "currentThread",
                                          "()Ljava/lang/Thread;");
        jthread current_thread = jni->CallStaticObjectMethod(cls, mid);
        jlong current_thread_id = exchain::GetThreadId(current_thread, jni);
        jni->DeleteLocalRef(current_thread);
        {
            std::unique_lock<std::mutex> lock(processing_threads_mutex_);
            processing_threads_.insert(current_thread_id);
        }
        PLOG_INFO << "Attached thread with id: " << current_thread_id;

        while (true) {
            std::function<void(JNIEnv *)> task;
            std::unique_lock<std::mutex> lock(tasks_mutex);
            task_available_cv_.wait(lock, [this] { return !tasks_.empty(); });
            if (shutdown_) {
                break;
            }
            task = std::move(tasks_.front());
            tasks_.pop();
            lock.unlock();
            task(jni);
        }
        jvm_->DetachCurrentThread();
    }
};
}  // namespace exchain