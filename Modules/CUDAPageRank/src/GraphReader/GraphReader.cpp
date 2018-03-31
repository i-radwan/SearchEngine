/*
 * GraphReader.cpp
 *
 *  Created on: Feb 27, 2018
 *      Author: ahmedsamir
 */

#include "GraphReader.hpp"

namespace PageRank {

int GraphReader::pages_count = 0;
int GraphReader::edges_count = 0;

vector<int> GraphReader::out_degrees = vector<int>();
vector< vector<int> > GraphReader::edges_list = vector< vector<int> >();

void GraphReader::read_graph(string path) {
	ifstream file;
	file.open(path.c_str());

	int n, to, from;
	file >> pages_count;

	edges_list = vector<vector<int> >(pages_count);
	out_degrees = vector<int>(pages_count), 0;

	while (file >> from >> to) {
		edges_list[to].push_back(from);
		out_degrees[from]++;

		edges_count++;
	}

	file.close();
}

int GraphReader::get_pages(Page* &pages, double* &pages_probs, int* &in_nodes, int& dangling_nodes_count) {
	in_nodes = new int[edges_count];
	pages_probs = new double[pages_count];
	pages = new Page[pages_count];

	dangling_nodes_count = 0;

	// Initialize the pages_probs (I) vector with 1/n values
	for (int i = 0; i < pages_count; ++i) {
		pages_probs[i] = 1.0 / pages_count;
	}

	int next_idx = 0;
	for (int i = 0; i < pages_count; ++i) {
		pages[i].out_links_count = out_degrees[i];
		pages[i].in_links_count = edges_list[i].size();

		if (edges_list[i].size() > 0) {
			pages[i].start_idx = next_idx;
			pages[i].end_idx =  int(edges_list[i].size()) + next_idx;
		}

		for (int j = 0; j < edges_list[i].size(); j++) {
			int auto from = edges_list[i][j];
			in_nodes[next_idx++] = from;
		} 

		if (out_degrees[i] == 0) {
			pages[i].dangling_node = true;
			dangling_nodes_count++;
		}
	}

	return pages_count;
}

} /* namespace PageRank */
