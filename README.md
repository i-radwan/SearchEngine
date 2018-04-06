# Search Engine
A Simple Search Engine written in Java

### Used libraries, frameworkd, ..etc.
- MongoDB
- Spark
- Jsoup
- Snowball
- CUDA

### How to use?
- Clone this repository.
- Make sure you have MongoDB server running on `localhost:27017`.
- Compile and run the project using at least JDK 1.8.
- Choose `Clear Database` in order to setup the database and collections.
- Finally choose `Crawler` and enter the needed number of threads to start crawling the web.

### How to the search GUI?
- Make sure you have python installed (for light webserver).
- Apply the `How to use` section described above but choose `Test Query Processor`.
- Make sure that the server is listening to `http://0.0.0.0:8080` (change the code in both client and server sides to use different port)
- Open terminal window.
- Navigate to the client module folder.
- Execute `python -m SimpleHTTPServer 8000`
- Open your browser and visit `http://0.0.0.0:8000`

