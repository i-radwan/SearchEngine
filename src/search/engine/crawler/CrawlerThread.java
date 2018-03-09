package search.engine.crawler;

import org.jsoup.nodes.Document;
import search.engine.indexer.Indexer;
import search.engine.models.WebPage;
import search.engine.utils.Constants;
import search.engine.utils.WebUtilities;

import java.net.URL;
import java.util.concurrent.*;


public class CrawlerThread extends java.lang.Thread {

    //
    // Static variables
    //
    public static int sWebPagesCnt = 0;
    public static BlockingQueue<String> sURLsQueue = new LinkedBlockingDeque<>();
    public static ConcurrentSkipListSet<String> sVisitedURLs = new ConcurrentSkipListSet<>();
    public static ConcurrentHashMap<String, Integer> sBaseURLVisitedCnt = new ConcurrentHashMap<>();

    //
    // Member variables
    //
    private RobotsTextParser mRobotTextParser;
    private Indexer mIndexer;


    /**
     * Constructs a new crawler thread.
     *
     * @param robotManager robot manger object to handle robots text parsing
     * @param indexer      an indexer object in order to store the crawled web pages
     */
    CrawlerThread(RobotsTextManager robotManager, Indexer indexer) {
        mRobotTextParser = new RobotsTextParser(robotManager);
        mIndexer = indexer;
    }

    /**
     * Crawler main function.
     * As long as there exists URLs to crawl, the function would be
     * running getting documents and new URLs.
     */
    @Override
    public void run() {
        System.out.println("    - Crawler " + this.getName() + " started");

        while (true) {
            URL curUrl = getNextURL();

            if (curUrl == null) {
                break;
            }

            String curUrlStr = curUrl.toString();
            String curBaseUrl = WebUtilities.getBaseURL(curUrl);

            // If the following check placed earlier then the crawler would be more interested in
            // fetching robots.txt rather than fetching the web pages themselves.
            if (!mRobotTextParser.allowedURL(curUrl)) {
                Output.log("Couldn't crawl url : " + curUrlStr + " because of robots.txt !!!!!!");
                removeURLFromCnt(curBaseUrl);
                continue;
            }


            Output.log("Crawling: " + curUrlStr);

            Document doc = WebUtilities.fetchWebPage(curUrlStr);

            if (doc == null) {
                Output.log("Empty HTML document returned : " + curUrlStr);
                removeURLFromCnt(curBaseUrl);
                continue;
            }

            Output.logVisitedURL(curUrlStr);

            processWebPage(doc);
        }

        System.out.println("Crawler " + this.getName() + " is exiting...");
    }

    /**
     * Processes the given HTML document by extracting the out links
     * and inserting then in the queue, and indexing it in the database.
     *
     * @param doc the web page document to process
     */
    private void processWebPage(Document doc) {
        WebPage page = new WebPage(doc);

        //
        // Extract web page URLs
        //
        for (String urlStr : page.outLinks) {
            URL url = WebUtilities.getURL(urlStr);

            if (url == null) {
                continue;
            }

            String baseUrlStr = WebUtilities.getBaseURL(url);

            // Lock the arrays and insert in them
            synchronized (sVisitedURLs) {
                synchronized (sURLsQueue) {
                    synchronized (sBaseURLVisitedCnt) {
                        //
                        if (crawlable(urlStr, baseUrlStr))
                            addURL(urlStr, baseUrlStr);
                        else
                            Output.log("Skipped: " + urlStr);
                    }
                }
            }
        }

        //mIndexer.indexWebPage(page);
    }

    /**
     * Gets the front URL of the queue and returns it.
     * Throws an exception if it couldn't poll any URLs in {@code MAX_POLL_WAIT_TIME_MS} millis.
     *
     * @return the front url string of the queue
     */
    private URL getNextURL() {
        String url = "";

        try {
            url = sURLsQueue.poll(Constants.MAX_POLL_WAIT_TIME_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Output.log(e.getMessage());
        }

        return WebUtilities.getURL(url);
    }

    /**
     * Adds the given URL to the queue and marks it as visited.
     * <p>
     * The function must be called with <b>exclusive</b> access to:
     * <ul>
     * <li>{@code sWebPagesCnt}</li>
     * <li>{@code sBaseURLVisitedCnt}</li>
     * <li>{@code sVisitedURLs}</li>
     * </ul>
     *
     * @param url     the web page URL to be added
     * @param baseURL the web page base URL string
     */
    private void addURL(String url, String baseURL) {
        sWebPagesCnt++;
        sURLsQueue.add(url);
        sVisitedURLs.add(url);
        sBaseURLVisitedCnt.put(
                baseURL,
                sBaseURLVisitedCnt.getOrDefault(baseURL, 0) + 1
        );
        Output.logURL(url);
    }

    /**
     * Removes the given URL from the web pages count.
     * To be called when the web page fails to be crawled,
     * either because it is not allowed by robots rules or due to any other errors.
     *
     * @param baseURL the web page base URL
     */
    private void removeURLFromCnt(String baseURL) {
        synchronized (sBaseURLVisitedCnt) {
            sWebPagesCnt--;
            sBaseURLVisitedCnt.put(
                    baseURL,
                    sBaseURLVisitedCnt.getOrDefault(baseURL, 1) - 1
            );
        }
    }

    /**
     * Checks if the given URL is valid to be crawled.
     * <p>
     * Valid rules:
     * <ul>
     * <li>The maximum limit of crawling web pages is not reached.</li>
     * <li>The url is not visited before.</li>
     * </ul>
     * <p>
     * The function must be called with <b>exclusive</b> access to:
     * <ul>
     * <li>{@code sWebPagesCnt}</li>
     * <li>{@code sBaseURLVisitedCnt}</li>
     * <li>{@code sVisitedURLs}</li>
     * </ul>
     *
     * @param url     the web page URL string
     * @param baseURL the web page base URL string
     * @return {@code true} if the given URL is valid to be crawled
     */
    private boolean crawlable(String url, String baseURL) {
        return sWebPagesCnt < Constants.MAX_WEB_PAGES_COUNT
                && sBaseURLVisitedCnt.getOrDefault(baseURL, 0) < Constants.MAX_BASE_URL_COUNT
                && !sVisitedURLs.contains(url);
    }
}
