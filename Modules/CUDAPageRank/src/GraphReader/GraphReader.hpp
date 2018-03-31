/*
 * GraphReader.h
 *
 * 	Created on: Feb 27, 2018
 * 		Author: ahmedsamir
 */

#ifndef GRAPHREADER_H_
#define GRAPHREADER_H_

#include <vector>
#include <iostream>
#include <string>
#include <fstream>
#include <cstring>
#include <iomanip>
#include "../Utils/utils.hpp"
#include "../Page/Page.hpp"

using namespace std;
typedef double* I;

namespace PageRank {

class GraphReader {
public:

	static int pages_count;
	static int edges_count;

	static vector<int> out_degrees;
	static vector< vector<int> > edges_list;

	static void read_graph(string path);

	static int get_pages(Page* &pages, double* &pages_probs, int* &edges_list, int& dangling_nodes_count);

};

} /* namespace PageRank */

#endif /* GRAPHREADER_H_ */
