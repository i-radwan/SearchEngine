/*
 * Kernel.h
 *
 *  Created on: Feb 27, 2018
 *      Author: ahmedsamir
 */

#ifndef KERNEL_H_
#define KERNEL_H_

#include <iostream>
#include <string>
#include <stdio.h>

using namespace std;

#define MAX_ITERATIONS 100
#define ALPHA 0.85
#define MAX_BLOCK_SIZE 16


typedef double* Matrix;

namespace PageRank {

class Kernel {
private:
	int n;
	Matrix d_a;
    Matrix d_b;
    Matrix d_c;
    double* d_sum;

public:
	Kernel(int n) : n(n) {}
	virtual ~Kernel() {}

	void allocate_matrices(Matrix h_a, Matrix h_b);

	void run_kernel();

	Matrix get_result();
};

} /* namespace PageRank */

#endif /* KERNEL_H_ */
