package search.engine.crawler;

import javafx.util.Pair;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingDeque;


public class Crawler {
	
	private static BlockingQueue<String> mWebURLs;
	private static ConcurrentSkipListSet<String> mVisitedURLs;
	private static ArrayList<Thread> mCrawlers = new ArrayList<>();
	private static ConcurrentSkipListSet<Integer> mAllowedURLs, mDisallowedURLs;
	private static ConcurrentHashMap<Integer, Pair<ArrayList<String>, Boolean>> mURLRules;
	private static ConcurrentHashMap<String, Integer> mBaseUrlCnt;
	private static ConcurrentHashMap<String, Integer> mURLIds;
	private static RobotsTextManager mRobotManager;
	private static int mWebPagesCnt = 0;
	
	/**
	 * Initializes the web crawlers and the start URLs
	 *
	 * @param threadsCnt
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void start(int threadsCnt) throws InterruptedException {
		//Output.clearFiles();
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
		Input.init();

		mWebURLs = new LinkedBlockingDeque<>();
		mVisitedURLs = new ConcurrentSkipListSet<>();
		mAllowedURLs = new ConcurrentSkipListSet<>();
		mDisallowedURLs = new ConcurrentSkipListSet<>();
		mURLRules = new ConcurrentHashMap<>();
		mURLIds = new ConcurrentHashMap<>();
		mBaseUrlCnt = new ConcurrentHashMap<>();
		
		System.out.println("Reading Seed and Previous run data please wait!!");
		Input.readSeed(mWebURLs, mVisitedURLs);
		Input.readPreviousRunData(mWebURLs, mVisitedURLs, mAllowedURLs, mDisallowedURLs, mURLRules, mURLIds, mBaseUrlCnt);
		Input.closeFiles();
		for (ConcurrentHashMap.Entry<String, Integer> entry : mBaseUrlCnt.entrySet()) {
			mWebPagesCnt += entry.getValue();
		}
		System.out.println("Ready.");
		mRobotManager = new RobotsTextManager(mURLIds, mURLRules, mDisallowedURLs, mAllowedURLs, "*");
	}
	
	/**
	 * Initialize the threads and set their name and start them
	 *
	 * @param size
	 */
	private static void initAndStartThreads(int size) {
		for (int i = 0; i < size; i++) {
			mCrawlers.add(new CrawlerThread(mWebURLs, mVisitedURLs, mRobotManager, mBaseUrlCnt, mWebPagesCnt));
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
		for (int i = 0; i < mCrawlers.size(); i++) mCrawlers.get(i).join();
	}
}