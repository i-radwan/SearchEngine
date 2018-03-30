/*
 * Kernel.h
 *
 *  Created on: Feb 27, 2018
 *      Author: ahmedsamir
 */

#ifndef KERNEL_H_
#define KERNEL_H_

#include "../Utils/utils.hpp"
#include "../Page/Page.hpp"

#define MAX_ITERATIONS 100
#define ALPHA 0.85
#define MAX_BLOCK_SIZE 32

namespace PageRank {

class Kernel {
private:

	int pages_count;
	int edges_count;

	Page *d_pages;
	double *d_pages_probs;
	int *d_edges_list;
	float *d_pages_ranks_sum;
	float *d_dangling_probs_sum;

public:
	Kernel(int n, int e) : pages_count(n), edges_count(e), d_pages(NULL), d_pages_probs(NULL), d_edges_list(NULL) {}

	virtual ~Kernel() {}

	void allocate_data(Page *h_pages, double *h_pages_probs, int *edges_list);

	void run_kernel();

	double *get_result();
};

} /* namespace PageRank */

#endif /* KERNEL_H_ */
