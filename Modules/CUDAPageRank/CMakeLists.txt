# Thanks to Ashwin Nanjappa for his gist that shows how to inlcude CUDA and nvcc
# Link: https://gist.github.com/ashwin/6547060

cmake_minimum_required(VERSION 2.8)
find_package(CUDA QUIET REQUIRED)

# Pass options to NVCC
set(
    CUDA_NVCC_FLAGS
    ${CUDA_NVCC_FLAGS};
    -O3 -gencode arch=compute_61,code=sm_61
    )

# Specify include directories
include_directories(
    src/GraphReader
    src/Kernel
    src/Page
    src/Timer
    src/Utils
    )

# For compilation ...
# Specify target & source files to compile it from
cuda_add_executable(
    PageRank
    src/Main.cpp
    src/GraphReader/GraphReader.hpp
    src/GraphReader/GraphReader.cpp
    src/Kernel/Kernel.cu
    src/Page/Page.hpp
    src/Timer/GPUTimer.hpp
    src/Utils/utils.hpp
    )

# For linking ...
# Specify target & libraries to link it with
target_link_libraries(
    PageRank
    )
