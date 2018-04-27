package search.engine.crawler;

import search.engine.indexer.Indexer;
import search.engine.utils.WebUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class Crawler {

    //
    // Member variables
    //
    private List<Thread> mCrawlerThreads;
    private RobotsTextManager mRobotsTextManager;
    private Indexer mIndexer;


    /**
     * Constructor.
     *
     * @param indexer a database indexer object to store the crawled web pages.
     */
    public Crawler(Indexer indexer) {
        mIndexer = indexer;
        mRobotsTextManager = new RobotsTextManager();
    }

    /**
     * Initializes the web crawler environment and starts
     * crawling.
     *
     * @param threadsCnt the number of crawler threads to start
     */
    public void start(int threadsCnt) {
        System.out.println("Start crawling...");

        Output.openFiles();
        Input.readSeed();
        calcVisitedUrlCount();
        startThreads(threadsCnt);
        waitThreadsFinish();
        Output.closeFiles();
        clearData();

        System.out.println("Finish crawling");

        System.out.println("Total fetched web pages: " + CrawlerThread.sTotalFetchedWebPagesCnt);
        System.out.println("Total indexed web pages: " + CrawlerThread.sTotalIndexedWebPagesCnt);
    }

    /**
     * Read the data of the last crawling run.
     */
    public void readPreviousData() {
        System.out.println("Reading previous run data...");
        Input.readPreviousData();
        System.out.println(CrawlerThread.sURLsQueue.size() + " URL(s) has been added to the queue");
    }

    /**
     * Calculates the URL visited counts according to
     * the current data in the visited set.
     */
    private void calcVisitedUrlCount() {
        // Clear previous counts
        CrawlerThread.sWebPagesCnt = 0;
        CrawlerThread.sBaseURLVisitedCnt = new ConcurrentHashMap<>();

        // Calculate current counts
        for (String url : CrawlerThread.sVisitedURLs) {
            String baseURL = WebUtilities.getHostName(url);

            if (baseURL == null) {
                continue;
            }

            CrawlerThread.sWebPagesCnt++;
            CrawlerThread.sBaseURLVisitedCnt.put(
                    baseURL,
                    CrawlerThread.sBaseURLVisitedCnt.getOrDefault(baseURL, 0) + 1
            );
        }
    }

    /**
     * Clears crawling data.
     */
    private void clearData() {
        CrawlerThread.sURLsQueue.clear();
        CrawlerThread.sVisitedURLs.clear();
        Output.clearFiles();
    }

    /**
     * Initializes the crawler threads and starts them.
     *
     * @param count the number of crawler threads to start
     */
    private void startThreads(int count) {
        mCrawlerThreads = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            mCrawlerThreads.add(new CrawlerThread(mRobotsTextManager, mIndexer));
            mCrawlerThreads.get(i).setName("Crawler-Thread-" + String.valueOf(i + 1));
            mCrawlerThreads.get(i).start();
        }
    }

    /**
     * Waits the crawler threads until they finish.
     */
    private void waitThreadsFinish() {
        for (Thread thread : mCrawlerThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}