cmake_minimum_required (VERSION 3.16.3)

project(native)
set (CMAKE_CXX_STANDARD 20)

include(cmake/CPM.cmake)
CPMAddPackage("gh:SergiusTheBest/plog#1.1.9")

find_package(Java REQUIRED)

set(JAVA_AWT_LIBRARY NotNeeded)
set(JAVA_JVM_LIBRARY NotNeeded)
find_package(JNI REQUIRED)


add_subdirectory(cpp)
