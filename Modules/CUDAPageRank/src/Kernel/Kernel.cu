/*
 * Kernel.cpp
 *
 *  Created on: Feb 27, 2018
 *      Author: Ahmed Samir
 */

#include "Kernel.hpp"

namespace PageRank {

__global__ void initialize_dangling_sum(float *d_dangling_probs_sum) {
	d_dangling_probs_sum[0] = 0.0;
}

__global__ void initialize_pages_ranks_sum(float *d_page_ranks_sum) {
	d_page_ranks_sum[0] = 0.0;
}

__global__ void calculate_dangling_sum_and_normalize (Page *d_pages, double *d_page_probs, int pages_count, float *d_page_ranks_sum, float *d_dangling_probs_sum) {
	// Calculate page index from thread address
	int idx = blockIdx.y * blockDim.y + threadIdx.y;

	if (idx < pages_count) {
		d_page_probs[idx] /= d_page_ranks_sum[0];
		if (d_pages[idx].dangling_node) {
			atomicAdd(d_dangling_probs_sum, float(d_page_probs[idx]));
		}
	}
}

__global__ void run_page_rank_iteration(Page *d_pages,
		double *d_page_probs,
		int *d_edges_list,
		int pages_count,
		float *d_pages_ranks_sum,
		float *d_dangling_probs_sum,
		double alpha) {

	int idx = blockIdx.y * blockDim.y + threadIdx.y;
	double new_rank = 0.0;

	if (idx < pages_count) {
		double c_element = 0.0;
		int i_start = d_pages[idx].start_idx;
		int i_end = d_pages[idx].end_idx;

		for (int i = i_start; i < i_end; i++) {
			int from = d_edges_list[i];
			c_element += d_page_probs[from] / ( 1.0 * d_pages[from].out_links_count);
		}

		new_rank = ((1 - alpha) * 1.0 / pages_count)+
				(alpha * c_element) +
				(alpha * 1.0 / pages_count * d_dangling_probs_sum[0]);
	}

	__syncthreads();

	if (idx < pages_count) {
		d_page_probs[idx] = new_rank;
		atomicAdd(d_pages_ranks_sum, new_rank);
	}
}

void Kernel::run_kernel() {
	// Calculate the grid and block sizes.
	int grid_size = int(ceil(1.0 * pages_count / MAX_BLOCK_SIZE));
	int block_size = MAX_BLOCK_SIZE;

	if (block_size < 1024) {
		dim3 dimGrid(1, grid_size);
		dim3 dimBlock(1, block_size);

		for (int i = 0; i < MAX_ITERATIONS; ++i) {
			if (i > 0) {
				initialize_dangling_sum<<<1, 1>>>(d_dangling_probs_sum);
				calculate_dangling_sum_and_normalize << <dimGrid, dimBlock>> >(d_pages, d_pages_probs, pages_count, d_pages_ranks_sum, d_dangling_probs_sum);
			}

			initialize_pages_ranks_sum<<<1, 1>>>(d_pages_ranks_sum);
			run_page_rank_iteration << < dimGrid, dimBlock >> > (d_pages, d_pages_probs, d_edges_list, pages_count, d_pages_ranks_sum, d_dangling_probs_sum, ALPHA);
		}
	} else {
		cout << "Error exceeded the maximum value for threads in a block 1024" << endl;
	}
}

void Kernel::allocate_data(Page *h_pages, double *h_pages_probs, int *h_edges_list) {
	float one = 1.0, zero = 0.0;

	// Allocate memory at the gpu device
	cudaMalloc((void **) &d_pages, sizeof(Page) * pages_count);
	cudaMalloc((void **) &d_pages_probs, sizeof(double) * pages_count);
	cudaMalloc((void **) &d_edges_list, sizeof(int) * edges_count);
	cudaMalloc((void **) &d_pages_ranks_sum, sizeof(float));
	cudaMalloc((void **) &d_dangling_probs_sum, sizeof(float));

	// Copy data from host (cpu) to the gpu
	cudaMemcpy(d_pages, h_pages, sizeof(Page) * pages_count, cudaMemcpyHostToDevice);
	cudaMemcpy(d_pages_probs, h_pages_probs, sizeof(double) * pages_count, cudaMemcpyHostToDevice);
	cudaMemcpy(d_edges_list, h_edges_list, sizeof(int) * edges_count, cudaMemcpyHostToDevice);
	cudaMemcpy(d_pages_ranks_sum, &one, sizeof(float), cudaMemcpyHostToDevice);
	cudaMemcpy(d_dangling_probs_sum, &zero, sizeof(float), cudaMemcpyHostToDevice);
}

double *Kernel::get_result() {
	double *pages_probs = new double[pages_count];

	cudaMemcpy(pages_probs, d_pages_probs, sizeof(double) * pages_count, cudaMemcpyDeviceToHost);
	cudaError_t e = cudaGetLastError();
	if (e != cudaSuccess) {
		printf("MemCpy (R): CUDA failure %s:%d: '%s'\n", __FILE__, __LINE__, cudaGetErrorString(e));
		exit(0);
	}

	return pages_probs;
}

} /* namespace PageRank */
