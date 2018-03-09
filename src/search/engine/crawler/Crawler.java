package search.engine.crawler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Crawler {

    private List<Thread> mCrawlerThreads;
    private RobotsTextManager mRobotManager;

    /**
     * Initializes the web crawler environment and starts
     * crawling.
     *
     * @param threadsCnt the number of crawler threads to start
     * @throws IOException
     * @throws InterruptedException
     */
    public void start(int threadsCnt) throws InterruptedException {
        init();
        initAndStartThreads(threadsCnt);
        waitThreadsFinish();
        Output.closeFiles();
    }

    /**
     * Reads the seed of URLs to crawl and the data of the previous runs.
     *
     * @throws IOException
     */
    private void init() {
        mRobotManager = new RobotsTextManager();

        // Initialize I/O files
        Output.init();
        Input.init();

        System.out.println("Reading URL seeds and previous run data, please wait...");

        Input.readPreviousRunData(mRobotManager);
        Input.readSeed();
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
            mCrawlerThreads.add(new CrawlerThread(mRobotManager));
            mCrawlerThreads.get(i).setName(String.valueOf((i + 1)));
            mCrawlerThreads.get(i).start();
        }
    }

    /**
     * Waits the crawler threads until they finish.
     *
     * @throws InterruptedException
     */
    private void waitThreadsFinish() throws InterruptedException {
        for (Thread c : mCrawlerThreads) {
            c.join();
        }
    }
}