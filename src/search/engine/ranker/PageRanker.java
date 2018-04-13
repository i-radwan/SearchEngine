package search.engine.ranker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;


public class PageRanker {

    //
    // Member variables
    //

    /**
     * The number of pages in the graph.
     */
    private Integer pagesCount;

    /**
     * The graph adjacency list.
     */
    private HashMap<Integer, ArrayList<Integer>> inList = new HashMap<>();

    /**
     * The out degrees of each page.
     */
    private ArrayList<Integer> outDegrees;

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
     * Add an arc to the graph
     *
     * @param from a node
     * @param to   another node
     */
    private void addArc(int from, int to) {
        if (inList.containsKey(to))
            inList.get(to).add(from);
        outDegrees.set(from, outDegrees.get(from) + 1);
    }

    /**
     * Initialize all vectors
     */
    private void initializeLists() {
        outDegrees = new ArrayList<>();
        pagesRank = new ArrayList<>();

        for (int i = 0; i < pagesCount; i++) {

            // Create a new list for each page
            ArrayList<Integer> newList = new ArrayList<>();
            inList.put(i, newList);

            outDegrees.add(0);
            pagesRank.add(1.0 / pagesCount); // Initialize at first with 1/n prob
        }
    }

    /**
     * Save edges list from a file
     *
     * @param filepath
     */
    public void saveFile(String filepath) {

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
            System.err.println("Error: " + e.getMessage());
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
     * Print page ranks on console
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
     */
    private void savePR() {
        try (PrintWriter out = new PrintWriter("Output/pageRanks.txt")) {
            for (Integer page = 0; page < pagesCount; page++) {
                out.println(page.toString() + " = " + pagesRank.get(page));
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public void run(String filePath) {
        readFile(filePath);
        rankPages();
        printPR();
        savePR();
    }
}
