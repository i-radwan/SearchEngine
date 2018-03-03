/*
 * GraphReader.h
 *
 *  Created on: Feb 27, 2018
 *      Author: ahmedsamir
 */

#ifndef GRAPHREADER_H_
#define GRAPHREADER_H_

#include <vector>
#include <iostream>
#include <string>
#include <fstream>
#include <cstring>
#include <iomanip>

using namespace std;
typedef double* Matrix;

namespace PageRank {

class GraphReader {
public:
	GraphReader();

	static int *out_degrees;

	static vector< vector<int> > read_graph(string path);

	static Matrix construct_h_matrix(vector< vector<int> > &edgesList, Matrix& g_matrix, Matrix& i_vector, int* &out_degrees);

	static void free_resources();

	virtual ~GraphReader();
};

} /* namespace PageRank */

#endif /* GRAPHREADER_H_ */
