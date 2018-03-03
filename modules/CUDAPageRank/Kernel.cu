/*
 * Kernel.cpp
 *
 *  Created on: Feb 27, 2018
 *      Author: Ahmed Samir
 */

#include "Kernel.h"

namespace PageRank {

// A Kernel for multiplying square matrices.
    __global__ void page_rank_iteration(Matrix d_a, Matrix d_b, Matrix d_c, double* d_sum, int n, double alpha) {
        double c_element = 0.0;

        int idx = blockIdx.y * blockDim.y + threadIdx.y;
        if (idx < n) {
            for (int i = 0; i < n; i++) {
                c_element += (d_a[idx * n + i] * d_b[i]);
            }
            d_c[idx] = (alpha * c_element) + (1.0 - alpha) * 1.0/n;
        }
    }

    __global__ void update_i_vector(Matrix d_b, Matrix d_c) {
        int idx = blockIdx.y * blockDim.y + threadIdx.y;

        // Normalize the vector.
        // copy the resulted vector from c to b for re multiplying and final result of course
        // after the final iteration will be stored in d_c
        d_b[idx] = d_c[idx];
    }

    void Kernel::run_kernel() {
        // Calculate the grid and block sizes.
        int grid_size = int(ceil(1.0 * n / MAX_BLOCK_SIZE));
        int block_size = int(ceil(1.0 * n / grid_size));

        if (block_size < 1024) {
            dim3 dimGrid(1, grid_size);
            dim3 dimBlock(1, block_size);

            for (int i = 0; i < MAX_ITERATIONS; ++i)
            {
                page_rank_iteration<<<dimGrid, dimBlock>>>(d_a, d_b, d_c, d_sum, n, ALPHA);
                update_i_vector<<<dimGrid, dimBlock>>>(d_b, d_c);
            }
        } else {
            cout << "Error exceeded the maximum value for threads in a block 1024" << endl;
        }
    }

    void Kernel::allocate_matrices(Matrix h_a, Matrix h_b) {
        long long matirx_bytes = sizeof(double) * n * n;
        long long vector_bytes = sizeof(double) * n;

        double* h_sum = new double;
        *h_sum = 1.0;


        // Allocate memory at the device for matrices a, b, and the result c
        cudaMalloc((void **) &d_a, matirx_bytes);
        cudaMalloc((void **) &d_b, vector_bytes);
        cudaMalloc((void **) &d_c, vector_bytes);
        cudaMalloc((void **) &d_sum, sizeof(double));

        // Copy matrices a & b to the device
        cudaMemcpy(d_a, h_a, matirx_bytes, cudaMemcpyHostToDevice);
        cudaError_t e=cudaGetLastError();
        if(e!=cudaSuccess) {
            printf("MemCpy (A): CUDA failure %s:%d: '%s'\n",__FILE__,__LINE__,cudaGetErrorString(e));
            exit(0);
        }

        cudaMemcpy(d_b, h_b, vector_bytes, cudaMemcpyHostToDevice);
        e =cudaGetLastError();
        if(e!=cudaSuccess) {
            printf("MemCpy (B): CUDA failure %s:%d: '%s'\n",__FILE__,__LINE__,cudaGetErrorString(e));
            exit(0);
        }

        cudaMemcpy(d_sum, h_sum, sizeof(double), cudaMemcpyHostToDevice);
        e =cudaGetLastError();
        if(e!=cudaSuccess) {
            printf("MemCpy (B): CUDA failure %s:%d: '%s'\n",__FILE__,__LINE__,cudaGetErrorString(e));
            exit(0);
        }
        delete h_sum;

    }

    Matrix Kernel::get_result() {

        Matrix h_c = new double[n];

        int vector_bytes = sizeof(double) * n;

        cudaMemcpy(h_c, d_c, vector_bytes, cudaMemcpyDeviceToHost);
        cudaError_t e=cudaGetLastError();
        if(e!=cudaSuccess) {
            printf("MemCpy (R): CUDA failure %s:%d: '%s'\n",__FILE__,__LINE__,cudaGetErrorString(e));
            exit(0);
        }

        return h_c;
    }

} /* namespace PageRank */
