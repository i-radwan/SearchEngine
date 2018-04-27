package search.engine.ranker;

import search.engine.indexer.Indexer;
import search.engine.indexer.WebPage;
import search.engine.utils.Constants;
import search.engine.utils.WebUtilities;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


public class PageRanker {

    //
    // Member variables
    //

    /**
     * Indexer object
     */
    Indexer mIndexer;

    /**
     * All web pages in the database
     */
    Map<String, WebPage> graphNodes;

    /**
     * Map web pages to their host
     */
    Map<String, String> hostWebPagesMap;

    /**
     * The number of pages in the graph.
     */
    private Integer pagesCount;

    /**
     * The graph adjacency list used in ranking iterations.
     */
    private HashMap<Integer, ArrayList<Integer>> inList = new HashMap<>();

    /**
     * The out degrees of each page.
     */
    private ArrayList<Integer> outDegrees;

    /**
     * The id assigned to each page URL
     * (from 0 to N, where N is the number of web pages "nodes")
     */
    private Integer nextWebPageID;

    /**
     * Mapping pageURL to an id
     */
    private Map<String, Integer> pagesIDS;

    /**
     * The ranks of the pages.
     */
    private ArrayList<Double> pagesRank;

    /**
     * The dumping factor.
     */
    private static final Double ALPHA = 0.85;

    /**
     * The maximum number iterations.
     */
    private static final Integer MAX_ITERATIONS = 100;

    //
    // Member methods
    //

    /**
     * Constructor.
     *
     * @param indexer indexer object needed to get the web pages graph and save new ranks.
     */
    public PageRanker(Indexer indexer) {
        mIndexer = indexer;
    }

    /**
     * Add an arc to the graph
     *
     * @param from a node
     * @param to   another node
     */
    private void addArc(int from, int to) {
        inList.get(to).add(from);

        outDegrees.set(from, outDegrees.get(from) + 1);
    }

    /**
     * Initialize all vectors and variables
     */
    private void initialize() {
        nextWebPageID = 0;

        outDegrees = new ArrayList<>();
        pagesRank = new ArrayList<>();
        pagesIDS = new HashMap<>();
        hostWebPagesMap = new HashMap<>();
    }


    private void initializePageRankLists() {
        for (int i = 0; i < pagesCount; i++) {

            // Create a new list for each page
            inList.put(i, new ArrayList<>());

            outDegrees.add(0);
            pagesRank.add(1.0 / pagesCount); // Initialize at first with 1/n prob
        }
    }

    /**
     * Get web pages graph
     */
    public void getGraph() {
        // Get the web pages in the graph (all nodes)
        graphNodes = mIndexer.getWebGraph();

        this.pagesCount = graphNodes.keySet().size();

        // Initialize
        initialize();
        initializePageRankLists();

        // add arcs
        for (Map.Entry<String, WebPage> webPageNode : graphNodes.entrySet()) {
            if (!pagesIDS.containsKey(webPageNode.getKey()))
                pagesIDS.put(webPageNode.getKey(), nextWebPageID++);

            // Loop over all links and write arcs
            for (String to : webPageNode.getValue().outLinks) {
                if (graphNodes.containsKey(to)) {
                    if (!pagesIDS.containsKey(to))
                        pagesIDS.put(to, nextWebPageID++);

                    this.addArc(pagesIDS.get(webPageNode.getKey()), pagesIDS.get(to));
                }
            }
        }
    }


    /**
     * Get host to host web pages graph
     */
    public void getHostToHostGraph() {
        // Get the web pages in the graph (all nodes)
        graphNodes = mIndexer.getWebGraph();

        this.pagesCount = 0;

        // Initialize
        initialize();

        // calculate pages count and get unique host pages ids.
        for (Map.Entry<String, WebPage> webPageNode : graphNodes.entrySet()) {
            // Get the host web page url.
            String webPageHostURL = WebUtilities.getHostName(webPageNode.getKey());
            // Map this url to its host url.
            hostWebPagesMap.put(webPageNode.getKey(), webPageHostURL);

            if (!pagesIDS.containsKey(webPageHostURL)) {
                this.pagesCount++;
//                System.out.println("New Host " + webPageHostURL + " ID: " + nextWebPageID);
                pagesIDS.put(webPageHostURL, nextWebPageID++);
            }

            for (String to : webPageNode.getValue().outLinks) {
                String toHostURL = WebUtilities.getHostName(to);

                if (graphNodes.containsKey(to)) { // Check if this out link page is currently indexed in the database.
                    // Map this url to its host url.
                    hostWebPagesMap.put(to, toHostURL);

                    if (!pagesIDS.containsKey(toHostURL)) {
//                        System.out.println("New Host " + toHostURL + " ID: " + nextWebPageID);
                        this.pagesCount++;
                        pagesIDS.put(toHostURL, nextWebPageID++);
                    }
                }
            }
        }

        // Initialize pageRank lists after getting pages count.
        initializePageRankLists();

        // add arcs.
        for (Map.Entry<String, WebPage> webPageNode : graphNodes.entrySet()) {
            // Get the host web page url.
            String webPageHostURL = WebUtilities.getHostName(webPageNode.getKey());

            // Loop over all links and write arcs
            for (String to : webPageNode.getValue().outLinks) {
                String toHostURL = WebUtilities.getHostName(to);

                if (graphNodes.containsKey(to)) { // Check if this out link page is currently indexed in the database.
                    this.addArc(pagesIDS.get(webPageHostURL), pagesIDS.get(toHostURL));
                }
            }
        }

        // TODO REMOVE THIS DEBUGGING LINE.
        System.out.println("Number of unique hosts is " + this.pagesCount);
    }

    /**
     * save web pages graph to a file
     */
    private void saveGraph() {
        // Write to the edges file
        try (PrintWriter out = new PrintWriter(Constants.GRAPH_FILE_NAME)) {
            // Write the number of nodes
            out.println(this.pagesCount);

            // Write arcs
            for (int to = 0; to < pagesCount; to++) {

                if (inList.containsKey(to)) {
                    for (int from : inList.get(to)) {
                        out.println(from + " " + to);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * PageRanker calculations
     */
    private void rankPages() {
        Double danglingSum, pagesRankSum = 1.0;

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            danglingSum = 0.0;

            // Normalize the PR(i) needed for the power method calculations
            if (iteration > 0) {
                for (int page = 0; page < pagesCount; page++) {
                    Double rank = pagesRank.get(page) * 1.0 / pagesRankSum;
                    pagesRank.set(page, rank);
                    if (outDegrees.get(page) == 0) {
                        danglingSum += rank;
                    }
                }
            }

            pagesRankSum = 0.0;

            Double aPage = ALPHA * danglingSum * (1.0 / pagesCount); // Same for all pages
            Double oneProb = (1.0 - ALPHA) * (1.0 / pagesCount) * 1.0; // Same for all pages

            // Loop over all pages
            ArrayList<Double> newPagesRank = new ArrayList<>();
            for (int page = 0; page < pagesCount; page++) {

                Double hPage = 0.0;

                if (inList.containsKey(page)) {
                    for (Integer from : inList.get(page)) {
                        hPage += (1.0 * pagesRank.get(from) / (1.0 * outDegrees.get(from)));
                    }
                    hPage *= ALPHA; // Multiply by dumping factor.
                }

                newPagesRank.add(hPage + aPage + oneProb);
            }

            // Update new ranks
            for (int page = 0; page < pagesCount; page++) {
                pagesRank.set(page, newPagesRank.get(page));
                pagesRankSum += newPagesRank.get(page);
            }
        }
    }

    /**
     * Read page ranks file, used in case of running on CUDA.
     *
     * @param inputPath
     */
    private void readPagesRanks(String inputPath) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(inputPath));
            String strLine;

            // Read file line by line
            while ((strLine = br.readLine()) != null) {

                String[] strs = strLine.trim().split(" ");
                Integer pageID = Integer.parseInt(strs[0]);
                Double rank = Double.parseDouble(strs[2]);

                pagesRank.set(pageID, rank);

            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Updates pages ranks in the database
     */
    private void updatePagesRanks(boolean cudaInput) {
        if (cudaInput) readPagesRanks(Constants.CUDA_PAGE_RANKS_FILE_NAME);
        else {
            // Reverse a map
            Map<Integer, String> pagesURL = pagesIDS.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

            for (int id = 0; id < this.pagesCount; id++) {
                graphNodes.get(pagesURL.get(id)).rank = pagesRank.get(id);
            }

            mIndexer.updatePageRanks(graphNodes.values());
        }
    }

    /**
     * Updates pages ranks in the database in case of host to host graph.
     */
    private void updateHostToHostPagesRanks(boolean cudaInput) {
        if (cudaInput) readPagesRanks(Constants.CUDA_PAGE_RANKS_FILE_NAME);
        else {
            // Loop over the Map and update
            for (String webPageURL : hostWebPagesMap.keySet()) {
                // Get its host URl.
                String webPageHostURL = hostWebPagesMap.get(webPageURL);

                // update web page rank.
                graphNodes.get(webPageURL).rank = pagesRank.get(pagesIDS.get(webPageHostURL));
            }

            mIndexer.updatePageRanks(graphNodes.values());
        }
    }

    /**
     * Print page ranks on console (Left for Debugging)
     */
    private void printPR(boolean checkSumOnly) {
        Double checkSum = 0.0;
        for (Integer page = 0; page < pagesCount; page++) {
            checkSum += pagesRank.get(page);
            if (!checkSumOnly)
                System.out.println("Page Ranker: " + page.toString() + " = " + pagesRank.get(page));
        }
        System.out.println("Page Ranker: check sum = " + checkSum.toString());
    }

    /**
     * Save page ranks to the output ranks.txt file (Left for Debugging)
     */
    private void savePR() {
        try (PrintWriter out = new PrintWriter(Constants.PAGE_RANKS_FILE_NAME)) {
            for (Integer page = 0; page < pagesCount; page++) {
                out.println(page.toString() + " = " + pagesRank.get(page));
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Read edges list from a file (Deprecated)
     *
     * @param filePath
     */
    private void readGraphFile(String filePath) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String strLine;

            this.pagesCount = Integer.parseInt(strLine = br.readLine().trim().split(" ")[0]);

            initialize();

            // Read file line by line
            while ((strLine = br.readLine()) != null) {

                String[] strs = strLine.trim().split(" ");
                Integer u = Integer.parseInt(strs[0]);
                Integer v = Integer.parseInt(strs[1]);

                // Add arcs
                this.addArc(u, v);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Start page ranking algorithm
     */
    public void start(boolean hostPagesOnly) {
        System.out.println("Start page ranking...");

        // Get the graph and save it
        if (hostPagesOnly) getHostToHostGraph();
        else getGraph();
        saveGraph();

        rankPages();
        updateHostToHostPagesRanks(false);

        printPR(true);
        savePR();

        System.out.println("Finish page ranking");
    }

    /**
     * Start CUDA page ranking algorithm
     */
    public void startCUDAPageRank() {
        try {
            // Get the graph and save it
            getGraph();
            saveGraph();

            // Compile and run cuda.
            String[] cmd = {Constants.CUDA_SCRIPT_PATH, "../../../" + Constants.GRAPH_FILE_NAME};
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor();

            // Update Ranks
            this.updatePagesRanks(true);

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
