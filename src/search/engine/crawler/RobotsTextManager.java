package search.engine.crawler;

import javafx.util.Pair;

import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;


public class RobotsTextManager {
	
	private static ConcurrentSkipListSet<Integer> mDisallowedURLs, mAllowedURLs;
	public static ConcurrentHashMap<Integer, Pair<ArrayList<String>, Boolean>> mURLRules;
	private static ConcurrentHashMap<String, Integer> mURLIds;
	protected final String mUserAgent;
	
	/**
	 * Takes the data and initialize it's static members
	 *
	 * @param urlIds
	 * @param urlRules
	 * @param disallowedUrls
	 * @param allowedUrls
	 * @param userAgent
	 */
	RobotsTextManager(ConcurrentHashMap<String, Integer> urlIds, ConcurrentHashMap<Integer, Pair<ArrayList<String>, Boolean>> urlRules, ConcurrentSkipListSet<Integer> disallowedUrls, ConcurrentSkipListSet<Integer> allowedUrls, String userAgent) {
		mURLRules = urlRules;
		mDisallowedURLs = disallowedUrls;
		mUserAgent = userAgent;
		mAllowedURLs = allowedUrls;
		mURLIds = urlIds;
	}
	
	/**
	 * Returns the URL id if it exists else it creates a new ID and give it to the URL
	 *
	 * @param url
	 * @return
	 */
	public Integer getURLId(String url) {
		synchronized (mURLIds) {
			if (mURLIds.containsKey(url))
				return mURLIds.get(url);
			
			Output.logURLId(mURLIds.size(), url);
			mURLIds.put(url, mURLIds.size());
			return mURLIds.size() - 1;
		}
	}
	
	/**
	 * Returns true if the Object has the URL's robots.txt Cached
	 *
	 * @param url
	 * @return
	 */
	public boolean isCached(URL url) {
		synchronized (mURLRules) {
			if (mURLRules.containsKey(getURLId(URLUtilities.getBaseURL(url))))
				return true;
			return false;
		}
	}
	
	/**
	 * If the URL was already cached it returns true else it Caches the URL by inserting a new arraylist and setting the
	 * value of the pair to be false in order to know that that it was Cached but it's robots.txt was not generated
	 *
	 * @param url
	 * @return returns true if the URL wasn't cached and false if it was
	 */
	public boolean cacheURL(URL url) {
		synchronized (mURLRules) {
			if (!isCached(url)) {
				mURLRules.put(getURLId(URLUtilities.getBaseURL(url)), new Pair<>(new ArrayList<>(), false));
				return true;
			}
			return false;
		}
	}
	
	
	/**
	 * takes the URL and the list of the new rules and sets them
	 * also sets the value of the pair to be true in order to know that the robots.txt was inserted and then notify the
	 * waiting threads
	 *
	 * @param url
	 * @param rules
	 */
	public void updateRules(URL url, ArrayList<String> rules) {
		synchronized (mURLRules) {
			mURLRules.put(getURLId(URLUtilities.getBaseURL(url)), new Pair<>(rules, true));
			
			ConcurrentHashMap<Integer, Pair<ArrayList<String>, Boolean>> sync = mURLRules;
			synchronized (sync) {
				sync.notifyAll();
			}
		}
	}
	
	/**
	 * returns true if the URL is allowed to be crawled
	 *
	 * @param url
	 * @return
	 */
	public boolean isUrlAllowed(URL url) {
		if (mAllowedURLs.contains(getURLId(url.toString())))
			return true;
		if (mDisallowedURLs.contains(getURLId(url.toString())))
			return false;
		
		ArrayList<String> rules;
		synchronized (mURLRules) {
			rules = new ArrayList<>(mURLRules.get(getURLId(URLUtilities.getBaseURL(url))).getKey());
		}
		boolean ret = URLUtilities.doesURLMatch(url.toString(), rules);
		if (ret) {
			mDisallowedURLs.add(getURLId(url.toString()));
			Output.logDisallowedURL(String.valueOf(getURLId(url.toString())));
		} else {
			mAllowedURLs.add(getURLId(url.toString()));
			Output.logAllowedURL(String.valueOf(getURLId(url.toString())));
		}
		
		
		return !ret;
	}
}
