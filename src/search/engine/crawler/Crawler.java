package search.engine.crawler;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class Crawler {

    private static ArrayList<Thread> mCrawlers = new ArrayList<>();
    private static RobotsTextManager mRobotManager;

    /**
     * Initializes the web crawlers and the start URLs
     *
     * @param threadsCnt
     * @throws IOException
     * @throws InterruptedException
     */
    public static void start(int threadsCnt) throws InterruptedException {
        init();

        initAndStartThreads(threadsCnt);
        waitThreadsFinish();

        Output.closeFiles();
    }

    /**
     * Reads the seed of URLs to crawl and the previous runs data
     *
     * @throws IOException
     */
    private static void init() {
        Output.init();
        //Output.clearFiles();
        Input.init();


        System.out.println("Reading Seed and Previous run data please wait!!");

        Input.readPreviousRunData();
        Input.readSeed();

        Input.closeFiles();
        for (ConcurrentHashMap.Entry<String, Integer> entry : CrawlerThread.mBaseURLCnt.entrySet()) {
            CrawlerThread.mWebPagesCnt += entry.getValue();
        }

        System.out.println("Ready.");
        mRobotManager = new RobotsTextManager();
    }

    /**
     * Initialize the threads and set their name and start them
     *
     * @param size
     */
    private static void initAndStartThreads(int size) {
        for (int i = 0; i < size; i++) {
            mCrawlers.add(new CrawlerThread(mRobotManager));
            mCrawlers.get(i).setName(String.valueOf((i + 1)));
            mCrawlers.get(i).start();
        }
    }

    /**
     * Try and join the threads after they are finished
     *
     * @throws InterruptedException
     */
    private static void waitThreadsFinish() throws InterruptedException {
        for (Thread c : mCrawlers) {
            c.join();
        }
    }
}