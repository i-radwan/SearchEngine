package search.engine.ranker;

import search.engine.indexer.Indexer;
import search.engine.indexer.WebPage;
import search.engine.utils.Constants;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class PageRanker {
    /* The number of pages in the graph*/
    private Integer pagesCount;

    /* The Graph adjacency list */
    private HashMap<Integer, ArrayList<Integer>> inList = new HashMap<Integer, ArrayList<Integer>>();

    /* The out degrees of each page */
    private ArrayList<Integer> outDegrees;

    /* The ranks of the pages */
    private ArrayList<Double> pagesRank;

    /* The dumping factor */
    private final Double ALPHA = 0.85;

    /* The maximum number iterations */
    private final Integer MAX_ITERATIONS = 100;

    /**
     * Add an arc to the graph
     *
     * @param from a node
     * @param to another node
     */
    private void addArc(int from, int to) {
        if (inList.containsKey(to))
            inList.get(to).add(from);
        outDegrees.set(from, outDegrees.get(from) + 1);
    }

    /**
     * Initialize all vectors
     *
     */
    private void initializeLists() {
        outDegrees = new ArrayList<Integer>();
        pagesRank = new ArrayList<Double>();

        for (int i = 0; i < pagesCount; i++) {

            // Create a new list for each page
            ArrayList<Integer> newList = new ArrayList<Integer>();
            inList.put(i, newList);

            outDegrees.add(0);
            pagesRank.add(1.0 / pagesCount); // Initialize at first with 1/n prob
        }
    }

    /**
     * Save edges list from a file
     */
    public void saveGraph() {
        Indexer indexer = new Indexer();

        // The web pages ids (from 0 to N, where N is the number of web pages "nodes")
        Integer nextWebPageID = 0;
        Map<String, Integer> pagesIDS = new HashMap<>();

        // Get the web pages in the graph (all nodes)
        Map<String, WebPage> graphWebPages = indexer.getWebGraph();

        // Write to the edges file
        try (PrintWriter out = new PrintWriter(Constants.GRPAH_FILE_NAME)) {
            // Write the number of nodes
            out.println(graphWebPages.size());

            // Write edges
            for (Map.Entry<String, WebPage> webPageNode : graphWebPages.entrySet()) {
                if (!pagesIDS.containsKey(webPageNode.getKey()))
                    pagesIDS.put(webPageNode.getKey(), nextWebPageID++);

                // Loop over all links and write edges to the out file
                for (String to : webPageNode.getValue().outLinks) {
                    if (graphWebPages.containsKey(to)) {
                        if (!pagesIDS.containsKey(to))
                            pagesIDS.put(to, nextWebPageID++);

                        out.println(pagesIDS.get(webPageNode.getKey()) + " " + pagesIDS.get(to));
                    }
                }
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        // Write nodes to file TODO @Samir55 Ask about spaces in URL
        try (PrintWriter out = new PrintWriter(Constants.NODES_FILE_NAME)) {
            for (Map.Entry<String, WebPage> webPageNode : graphWebPages.entrySet()) {
                out.println(webPageNode.getKey() + " " + pagesIDS.get(webPageNode.getKey()));
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

    }

    /**
     * Read edges list from a file
     *
     * @param filePath
     */
    private void readFile(String filePath) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String strLine;

            this.pagesCount = Integer.parseInt(strLine = br.readLine().trim().split(" ")[0]);

            initializeLists();

            // Read file line by line
            while ((strLine = br.readLine()) != null) {

                String[] strs = strLine.trim().split(" ");
                Integer u = Integer.parseInt(strs[0]);
                Integer v = Integer.parseInt(strs[1]);

                // Add arcs
                this.addArc(u, v);
            }
        } catch (Exception e) {
            //Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * PageRanker calculations
     *
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
            ArrayList<Double> newPagesRank = new ArrayList<Double>();
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
     * Print page ranks on console
     *
     */
    private void printPR() {
        Double checkSum = 0.0;
        for (Integer page = 0; page < pagesCount; page++) {
            checkSum += pagesRank.get(page);
            System.out.println(page.toString() + " = " + pagesRank.get(page));
        }
        System.out.println("checkSum = " + checkSum.toString());
    }

    /**
     * Save page ranks to the output pageRanks.txt file
     *
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

    public void run() {
        readFile(Constants.GRPAH_FILE_NAME);

        rankPages();

        savePR();
    }
}
