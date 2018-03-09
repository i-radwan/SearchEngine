package search.engine.crawler;

import search.engine.utils.Constants;
import search.engine.utils.WebUtilities;

import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
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
    private static Scanner sAllowedURLsFile;
    private static Scanner sDisallowedURLsFile;
    private static Scanner sURLIdsFile;
    private static Scanner sURLRulesFile;


    /**
     * Initializes the input files.
     */
    public static void init() {
        try {
            sURLFile = new Scanner(new FileReader(Constants.URLS_FILE_NAME));
            sVisitedURLsFile = new Scanner(new FileReader(Constants.VISITED_URLS_FILE_NAME));
            sDisallowedURLsFile = new Scanner(new FileReader(Constants.DISALLOWED_URLS_FILE_NAME));
            sAllowedURLsFile = new Scanner(new FileReader(Constants.ALLOWED_URLS_FILE_NAME));
            sURLIdsFile = new Scanner(new FileReader(Constants.URL_IDS_FILE_NAME));
            sURLRulesFile = new Scanner(new FileReader(Constants.URL_RULES_FILE_NAME));
            sSeedFile = new Scanner(new FileReader(Constants.SEED_FILE_NAME));
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
    public static void readPreviousRunData(RobotsTextManager manager) {
        CrawlerThread.sVisitedURLs.addAll(readVisitedURLs());
        CrawlerThread.sBaseURLVisitedCnt.putAll(getBaseURLVisitedCnt(CrawlerThread.sVisitedURLs));
        CrawlerThread.sURLsQueue.addAll(readURLs(CrawlerThread.sVisitedURLs));
        manager.allowedURLs.addAll(readAllowedURLs());
        manager.disallowedURLs.addAll(readDisallowedURLs());
        manager.URLIds.putAll(readURLIds());
        manager.URLRules.putAll(readURLRules(manager.URLIds));
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
     * Reads the URLs that are allowed to be crawled by the robots.txt.
     *
     * @return set of allowed URLs IDs
     */
    public static Set<Integer> readAllowedURLs() {
        return readIntegers(sAllowedURLsFile);
    }

    /**
     * Reads the URLs that are disallowed to be crawled by the robots.txt.
     *
     * @return set of disallowed URLs IDs
     */
    public static Set<Integer> readDisallowedURLs() {
        return readIntegers(sDisallowedURLsFile);
    }

    /**
     * Reads the URLs with their IDs in order to know the ID of each URL.
     * To be used in {@code RobotsTextManager}.
     *
     * @return a map from a URL string to an integer ID
     */
    public static ConcurrentHashMap<String, Integer> readURLIds() {
        ConcurrentHashMap<String, Integer> ret = new ConcurrentHashMap<>();

        while (sURLIdsFile.hasNextLine()) {
            Integer id = sURLIdsFile.nextInt();
            sURLIdsFile.nextLine();
            String url = sURLIdsFile.nextLine();
            ret.put(url, id);
        }

        return ret;
    }

    /**
     * Reads the
     * Reads for each URL its rules that has been gotten from the robots.txt
     *
     * @param URLIds map from a URL string to a unique ID
     * @return a map from an id to a robots rules object
     */
    public static ConcurrentHashMap<Integer, RobotsRules> readURLRules(ConcurrentHashMap<String, Integer> URLIds) {
        ConcurrentHashMap<Integer, RobotsRules> ret = new ConcurrentHashMap<>();

        RobotsRules curRules = null;

        while (sURLRulesFile.hasNextLine()) {
            String line = sURLRulesFile.nextLine();

            if (line.startsWith(Constants.INIT_URL_RULE_FILE)) {
                String url = line.substring(Constants.INIT_URL_RULE_FILE.length(), line.length());
                int curId = URLIds.get(url);
                curRules = new RobotsRules(true);
                ret.put(curId, curRules);
            } else if (curRules != null) {
                curRules.rules.add(line);
            }
        }

        return ret;
    }

    /**
     * Calculates and returns the visited web pages count for each website.
     *
     * @param visitedURLs a set of visited URLs
     * @return a map containing the visited count for each unique website
     */
    private static ConcurrentHashMap<String, Integer> getBaseURLVisitedCnt(Set<String> visitedURLs) {
        ConcurrentHashMap<String, Integer> ret = new ConcurrentHashMap<>();

        for (String url : visitedURLs) {
            try {
                URL tmp = new URL(url);
                String baseURL = WebUtilities.getBaseURL(tmp);
                Integer cnt = ret.getOrDefault(baseURL, 0);
                ret.put(baseURL, cnt + 1);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
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
        sDisallowedURLsFile.close();
        sAllowedURLsFile.close();
        sURLIdsFile.close();
        sURLRulesFile.close();
    }
}
