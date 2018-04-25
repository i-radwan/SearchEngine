cd Modules
cd CUDAPageRank
mkdir build
cd build
cmake .. 
make 
./PageRank $1 >> log.txt


