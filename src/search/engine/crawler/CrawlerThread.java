package search.engine.crawler;

import org.jsoup.nodes.Document;
import search.engine.indexer.Indexer;
import search.engine.models.WebPage;
import search.engine.utils.Constants;
import search.engine.utils.Utilities;
import search.engine.utils.WebPageParser;
import search.engine.utils.WebUtilities;

import java.net.URL;
import java.util.concurrent.*;


public class CrawlerThread extends Thread {

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
     * running getting documents and new URLs.
     */
    @Override
    public void run() {
        System.out.println("Crawler " + this.getName() + " started");

        while (true) {
            // Pop the first URL in the queue
            URL curUrl = getNextURL();

            // If no URL was found in the queue then break
            if (curUrl == null) {
                break;
            }

            // Start crawling the current web page
            crawl(curUrl);
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
        String baseUrlStr = WebUtilities.getBaseURL(url);

        // ===========================================================================
        //
        // Check for the frequency of visiting the current web page
        //
        WebPage lastPage = mIndexer.getWebPageByURL(urlStr);

        // If fetch skip count does not reach the limit then skip fetching this page
        // and increment fetch skip count by one
        if (lastPage.fetchSkipCount + 1 < lastPage.fetchSkipLimit) {
            mIndexer.incrementFetchSkipCount(lastPage.url);
            extractOutLinks(lastPage);
            removeURLFromCnt(baseUrlStr);
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
            mIndexer.removeWebPage(lastPage.url);
            removeURLFromCnt(baseUrlStr);
            Output.log("Not allowed by robots.txt : " + urlStr);
            System.out.println("Not allowed by robots.txt : " + urlStr);
            return;
        }
        // ===========================================================================
        //
        // Check robots text rules
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

        WebPage page = WebPageParser.parse(doc);
        extractOutLinks(page);
        processWebPage(page, lastPage);
        Output.logVisitedURL(urlStr);
    }

    /**
     * Processes the given web page by comparing its content with the previously
     * fetched content from the previous crawler run and
     * updates the values accordingly.
     *
     * @param curPage the currently fetched web page
     * @param prvPage the fetched web page from the indexer
     */
    private void processWebPage(WebPage curPage, WebPage prvPage) {
        curPage.rank = prvPage.rank;
        curPage.fetchSkipLimit = prvPage.fetchSkipLimit;
        curPage.fetchSkipCount = 0;

        // If no changes happens to the content of the web page then
        // increase the skip fetch limit and return
        if (curPage.wordsCount == prvPage.wordsCount
                && curPage.outLinks.equals(prvPage.outLinks)
                && curPage.content.equals(prvPage.content)
                && curPage.wordPosMap.equals(prvPage.wordPosMap)
                && curPage.wordScoreMap.equals(prvPage.wordScoreMap)) {

            curPage.fetchSkipLimit = Math.min(Constants.MAX_FETCH_SKIP_LIMIT, prvPage.fetchSkipLimit * 2);
            mIndexer.updateFetchSkipLimit(curPage.url, curPage.fetchSkipLimit);

            Output.log("Same page content  : " + curPage.url);
            System.out.println("Same page content  : " + curPage.url);
            return;
        }

        // Update the indexer
        mIndexer.indexWebPage(curPage);
        mIndexer.updateWordsDictionary(Utilities.getWordsDictionary(curPage.wordPosMap.keySet()));
    }

    /**
     * Processes the given HTML document by extracting its out links
     * and inserting them in the queue,
     * and indexing the web page in the database.
     *
     * @param page the web page to process
     */
    private void extractOutLinks(WebPage page) {
        for (String urlStr : page.outLinks) {
            URL url = WebUtilities.getURL(urlStr);
            String baseUrlStr = WebUtilities.getBaseURL(url);

            // Lock the arrays and insert in them
            synchronized (sVisitedURLs) {
                synchronized (sURLsQueue) {
                    synchronized (sBaseURLVisitedCnt) {
                        //
                        if (crawlable(urlStr, baseUrlStr))
                            addURL(urlStr, baseUrlStr);
                        //else
                        //    Output.log("Skipped : " + urlStr);
                    }
                }
            }
        }
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
