package search.engine.crawler;

import search.engine.utils.Constants;
import search.engine.utils.WebUtilities;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class Input {

    //
    // Static variables
    //
    // TODO: change Scanner to BufferedReader as it is much more faster
    private static Scanner sSeedFile;
    private static Scanner sURLFile;
    private static Scanner sVisitedURLsFile;


    /**
     * Initializes the input files.
     */
    public static void init() {
        try {
            sSeedFile = new Scanner(new FileReader(Constants.SEED_FILE_NAME));
            sURLFile = new Scanner(new FileReader(Constants.URLS_FILE_NAME));
            sVisitedURLsFile = new Scanner(new FileReader(Constants.VISITED_URLS_FILE_NAME));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads the URL seeds and fills the URLs queue and the visited URLs set
     */
    public static void readSeed() {
        while (sSeedFile.hasNextLine()) {
            String s = sSeedFile.nextLine();

            if (!CrawlerThread.sVisitedURLs.contains(s)) {
                CrawlerThread.sURLsQueue.add(s);
                CrawlerThread.sVisitedURLs.add(s);
            }
        }
    }

    /**
     * Reads the data of the previous runs in case of interruption.
     * Used to continue from the same state as before.
     */
    public static void readPreviousRunData() {
        CrawlerThread.sVisitedURLs.addAll(readVisitedURLs());
        CrawlerThread.sBaseURLVisitedCnt.putAll(getBaseURLVisitedCnt(CrawlerThread.sVisitedURLs));
        CrawlerThread.sURLsQueue.addAll(readURLs(CrawlerThread.sVisitedURLs));
    }

    /**
     * Reads the URLs to be crawled.
     *
     * @param visitedURLs set of visited URLs
     * @return a list of URLs to be crawled
     */
    public static List<String> readURLs(Set<String> visitedURLs) {
        List<String> ret = new ArrayList<>();

        while (sURLFile.hasNextLine()) {
            String url = sURLFile.nextLine();

            if (!visitedURLs.contains(url)) {
                ret.add(url);
                visitedURLs.add(url);
            }
        }

        return ret;
    }

    /**
     * Reads the URLs that has been visited in the previous run.
     *
     * @return set of visited URLs.
     */
    public static Set<String> readVisitedURLs() {
        return readFile(sVisitedURLsFile);
    }

    /**
     * Calculates and returns the visited web pages count for each website.
     *
     * @param visitedURLs a set of visited URLs
     * @return a map containing the visited count for each unique website
     */
    private static ConcurrentHashMap<String, Integer> getBaseURLVisitedCnt(Set<String> visitedURLs) {
        ConcurrentHashMap<String, Integer> ret = new ConcurrentHashMap<>();

        for (String link : visitedURLs) {
            URL url = WebUtilities.getURL(link);

            if (url == null) {
                continue;
            }

            String baseURL = WebUtilities.getBaseURL(url);

            ret.put(
                    baseURL,
                    ret.getOrDefault(baseURL, 0) + 1
            );
        }

        return ret;
    }

    /**
     * Reads the given stream as a set of strings.
     *
     * @param scanner the stream to read from
     * @return a set of string
     */
    private static Set<String> readFile(Scanner scanner) {
        Set<String> ret = new HashSet<>();
        while (scanner.hasNextLine()) {
            ret.add(scanner.nextLine());
        }
        return ret;
    }

    /**
     * Reads the given stream as a set of integers.
     *
     * @param scanner the stream to read from
     * @return a set of integers
     */
    private static Set<Integer> readIntegers(Scanner scanner) {
        Set<Integer> ret = new HashSet<>();
        while (scanner.hasNextInt()) {
            ret.add(scanner.nextInt());
        }
        return ret;
    }

    /**
     * Closes the opened files after finish.
     */
    public static void closeFiles() {
        sURLFile.close();
        sVisitedURLsFile.close();
    }
}
