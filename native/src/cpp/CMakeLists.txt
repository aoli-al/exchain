FILE(GLOB Source *.cc)
add_library(native SHARED ${Source})

target_include_directories(native PUBLIC ${PROJECT_SOURCE_DIR}/headers)
target_include_directories(native PUBLIC ${JNI_INCLUDE_DIRS})
target_include_directories(native PUBLIC ${plog_SOURCE_DIR}/include)