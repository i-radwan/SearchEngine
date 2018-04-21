package search.engine.crawler;

import org.jsoup.nodes.Document;
import search.engine.indexer.Indexer;
import search.engine.indexer.WebPage;
import search.engine.indexer.WebPageParser;
import search.engine.utils.Constants;
import search.engine.utils.WebUtilities;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;


public class CrawlerThread extends Thread {

    //
    // Static variables
    //
    public static int sWebPagesCnt = 0;
    public static BlockingQueue<String> sURLsQueue = new LinkedBlockingDeque<>();
    public static ConcurrentSkipListSet<String> sVisitedURLs = new ConcurrentSkipListSet<>();
    public static ConcurrentHashMap<String, Integer> sBaseURLVisitedCnt = new ConcurrentHashMap<>();

    private static final Object sLock = new Object();

    //
    // Member variables
    //
    private RobotsTextManager mRobotsTextManager;
    private Indexer mIndexer;


    /**
     * Constructs a new crawler thread.
     *
     * @param robotsMan robots text manger object to handle robots text parsing and retrieving
     * @param indexer   an indexer object in order to store the crawled web pages
     */
    CrawlerThread(RobotsTextManager robotsMan, Indexer indexer) {
        mRobotsTextManager = robotsMan;
        mIndexer = indexer;
    }

    /**
     * Crawler main function.
     * As long as there exists URLs to crawl, the function would be
     * running fetching web page documents and adding new URLs.
     */
    @Override
    public void run() {
        System.out.println("Crawler " + this.getName() + " started");

        while (true) {
            try {
                // Pop the first URL in the queue
                String url = sURLsQueue.poll(Constants.MAX_POLL_WAIT_TIME_MS, TimeUnit.MILLISECONDS);

                // If no URL was returned then exit
                if (url == null) {
                    break;
                }

                // Start crawling the current web page
                crawl(new URL(url));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("Crawler " + this.getName() + " is exiting...");
    }

    /**
     * Crawls the given web page URL and index it in the database.
     *
     * @param url a web page URL object to crawl
     */
    private void crawl(URL url) {
        // Get URL string and base URL string for further uses
        String urlStr = url.toString();
        String baseUrlStr = url.getHost();

        // ===========================================================================
        //
        // Check for the frequency of visiting the current web page
        //

        WebPage lastPage = mIndexer.getWebPageByURL(urlStr);

        // If fetch skip count does not reach the limit then skip fetching this page
        // and increment fetch skip count by one
        if (lastPage.fetchSkipCount + 1 < lastPage.fetchSkipLimit) {
            mIndexer.incrementFetchSkipCount(lastPage.id);
            enqueueOutLinks(lastPage.outLinks);
            //removeURLFromCnt(baseUrlStr);
            Output.log("Not fetched due to skip limits : " + urlStr);
            System.out.println("Not fetched due to skip limits : " + urlStr);
            return;
        }

        // ===========================================================================
        //
        // Check robots text rules
        //

        // If the current web page URL is not allowed by robots text then
        // remove it from the indexer and continue
        if (!mRobotsTextManager.allowedURL(url)) {
            mIndexer.removeWebPage(lastPage.id);
            removeURLFromCnt(baseUrlStr);
            Output.log("Not allowed by robots.txt : " + urlStr);
            System.out.println("Not allowed by robots.txt : " + urlStr);
            return;
        }

        // ===========================================================================
        //
        // Fetch the content of the web page
        //

        // Fetch the current web page content
        Output.log("Fetching : " + urlStr);
        System.out.println("Fetching: " + urlStr);
        Document doc = WebUtilities.fetchWebPage(urlStr);

        // If any errors occurred during connection then continue
        if (doc == null || doc.body() == null) {
            removeURLFromCnt(baseUrlStr);
            Output.log("Empty HTML document returned : " + urlStr);
            System.out.println("Empty HTML document returned : " + urlStr);
            return;
        }

        // ===========================================================================
        //
        // Process the current fetched web page
        //

        List<String> outLinks = WebPageParser.extractOutLinks(doc);
        mIndexer.indexWebPageAsync(url, doc, outLinks, lastPage);
        enqueueOutLinks(outLinks);
        Output.logVisitedURL(urlStr);
    }

    /**
     * Enqueues the given list of links into the crawlers shared queue.
     *
     * @param outLinks the web page out links to enqueue
     */
    private void enqueueOutLinks(List<String> outLinks) {
        // Randomly shuffle the out links
        Collections.shuffle(outLinks);

        for (String url : outLinks) {
            String baseURL = WebUtilities.getHostName(url);

            // Lock the resources and enqueue the link
            synchronized (sLock) {
                if (crawlable(url, baseURL)) {
                    addURL(url, baseURL);
                }
            }
        }
    }

    /**
     * Adds the given URL to the queue and marks it as visited.
     * <p>
     * The function must be called with <b>exclusive</b> access to {@code sLock}
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
        synchronized (sLock) {
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
     * Validity rules:
     * <ul>
     * <li>The maximum limit of crawling web pages is not reached.</li>
     * <li>The url is not visited before.</li>
     * </ul>
     * <p>
     * The function must be called with <b>exclusive</b> access to {@code sLock}
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
