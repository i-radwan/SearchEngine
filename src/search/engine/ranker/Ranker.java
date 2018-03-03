package search.engine.ranker;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class Ranker {

    /* The number of pages in the graph*/
    private Integer pagesCount;

    /* The Graph adjacency list */
    private HashMap<Integer, ArrayList<Integer>> inList = new HashMap<Integer, ArrayList<Integer>>();

    /* The out degrees of each page */
    private ArrayList<Integer> outDegrees;

    /* The ranks of the pages */
    private ArrayList<Double> pagesRank;

    /* The dumping factor */
    private final Double alpha = 0.85;

    /* The maximum number iterations */
    private final Integer maxIterations = 100;

    /* Add an arc to the graph */
    private void addArc(int from, int to) {
        if (inList.containsKey(to))
            inList.get(to).add(from);
        outDegrees.set(from, outDegrees.get(from) + 1);
    }

    /* Initialize all vectors */
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

    /* Read edges list from a file */
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

    /* PageRank calculations */
    private void rankPages() {
        Double danglingSum, pagesRankSum = 1.0;

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            danglingSum = 0.0;

            // Normalize the PR(i) needed for the power method calculations
            if (iteration > 0)
                for (int page = 0; page < pagesCount; page++) {
                    Double rank = pagesRank.get(page) * 1.0 / pagesRankSum;
                    pagesRank.set(page, rank);
                    if (outDegrees.get(page) == 0) {
                        danglingSum += rank;
                    }
                }

            pagesRankSum = 0.0;

            Double aPage = alpha * danglingSum * (1.0 / pagesCount); // Same for all pages
            Double oneProb = (1.0 - alpha) * (1.0 / pagesCount) * 1.0; // Same for all pages

            // Loop over all pages
            for (int page = 0; page < pagesCount; page++) {

                Double hPage = 0.0;

                if (inList.containsKey(page)) {
                    for (Integer from : inList.get(page)) {
                        hPage += (1.0 * pagesRank.get(from) / (1.0 * outDegrees.get(from)));
                    }
                    hPage *= alpha; // Multiply by dumping factor.
                }

                pagesRank.set(page, hPage + aPage + oneProb);
                pagesRankSum += hPage + aPage + oneProb;
            }
        }
    }

    /* Print page ranks */
    private void printPR() {
        Double checkSum = 0.0;
        for (Integer page = 0; page < pagesCount; page++) {
            checkSum += pagesRank.get(page);
            System.out.println(page.toString() + " = " + pagesRank.get(page));
        }
        System.out.println("checkSum = " + checkSum.toString());
    }

    /* Save page ranks to a file*/
    private void savePR() {
        try (PrintWriter out = new PrintWriter("Output/pageranks.txt")) {
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
