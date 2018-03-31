#include "Timer/GPUTimer.hpp"
#include "Kernel/Kernel.hpp"
#include "GraphReader/GraphReader.hpp"

using namespace PageRank;

int main(int argc, char **argv) {
	if (argc <= 1) return 0;

	string path = argv[1];

	GPUTimer gpu_timer;

	// Read graph file.
	GraphReader::read_graph(path);

	Page *h_pages;
	double *h_pages_probs;
	int *h_edges_list;
	int h_dangling_nodes_count;

	int nodes_count = GraphReader::get_pages(h_pages, h_pages_probs, h_edges_list, h_dangling_nodes_count);

	cout << "Pages count: " << GraphReader::pages_count << " Edges count: " << GraphReader::edges_count << " Dangling nodes count: " << h_dangling_nodes_count  << endl;

	// Create page rank kernel object
	Kernel page_rank(GraphReader::pages_count, GraphReader::edges_count);

	// Allocate matrices in the gpu memory
	page_rank.allocate_data(h_pages, h_pages_probs, h_edges_list);

	// Run PageRank algorithm
	gpu_timer.start();

	page_rank.run_kernel();

	gpu_timer.stop();

	// Save Result in output.txt file
	ofstream file;
	file.open("output.txt");

	double *res = page_rank.get_result(), check_sum = 0.0;
	for (int i = 0; i < nodes_count; i++) {
		file << i << " = " << setprecision(20) << res[i] << endl;
		check_sum += res[i];
	}
	file.close();


	// Print Elapsed time
	cout << "Elapsed PageRank time in gpu " << gpu_timer.elapsed() << " ms." << endl;
	return 0;
}

