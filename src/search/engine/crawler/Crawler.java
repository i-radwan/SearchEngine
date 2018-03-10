package search.engine.crawler;

import search.engine.indexer.Indexer;

import java.util.ArrayList;
import java.util.List;


public class Crawler {

    //
    // Member variables
    //
    private List<Thread> mCrawlerThreads;
    private RobotsTextManager mRobotManager;
    private Indexer mIndexer;


    /**
     * Initializes the web crawler environment and starts
     * crawling.
     *
     * @param threadsCnt the number of crawler threads to start
     */
    public void start(int threadsCnt) {
        init();
        initAndStartThreads(threadsCnt);
        waitThreadsFinish();
        Output.closeFiles();
    }

    /**
     * Reads the seed of URLs to crawl and the data of the previous runs.
     */
    private void init() {
        mRobotManager = new RobotsTextManager();
        mIndexer = new Indexer();

        // Initialize I/O files
        Output.init();
        Output.clearFiles();
        Input.init();

        System.out.println("Reading URL seeds and previous run data, please wait...");

        Input.readSeed();
        Input.readPreviousRunData(mRobotManager);
        Input.closeFiles();

        // Count the number of visited web pages in the previous run
        for (Integer cnt : CrawlerThread.sBaseURLVisitedCnt.values()) {
            CrawlerThread.sWebPagesCnt += cnt;
        }

        System.out.println("Finished reading data");
    }

    /**
     * Initializes the crawler threads and starts them.
     *
     * @param count the number of crawler threads to start
     */
    private void initAndStartThreads(int count) {
        System.out.println("Starting crawler threads...");

        mCrawlerThreads = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            mCrawlerThreads.add(new CrawlerThread(mRobotManager, mIndexer));
            mCrawlerThreads.get(i).setName(String.valueOf((i + 1)));
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