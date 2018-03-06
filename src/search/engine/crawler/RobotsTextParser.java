package search.engine.crawler;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;

import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;


public class RobotsTextParser {
	
	public static RobotsTextManager mManager;
	
	/**
	 * Constructor that takes RobotTxtManager
	 *
	 * @param manager
	 */
	
	RobotsTextParser(RobotsTextManager manager) {
		mManager = manager;
	}
	
	/**
	 * Generates the rules of the robots.txt File from the given URL if the URL rules was there then don't do anything
	 * If the URL was already cached check if the thread responsible for getting the robots.txt has gotten it or not
	 * If the thread has got it then use it else wait on the thread to notify you that he has got it
	 *
	 * @param url
	 */
	private void generateRobotTxt(URL url) {
		//check if the URL is already cached in the data and if it wasn't cached cache it
		if (mManager.cacheURL(url)) {
			//parse the file to a HashMap
			HashMap<String, ArrayList<String>> parsedRobotsTxt = parseRobotsText(URLUtilities.getRobotsTxt(url));
			
			//Gets the current user-agent rules
			ArrayList<String> rules = getUserAgentRules(parsedRobotsTxt);
			if (rules == null)
				rules = new ArrayList<>();
			
			Output.logURLRule(url.toString(), rules);
			mManager.updateRules(url, rules);
		} else {
			ConcurrentHashMap<Integer, Pair<ArrayList<String>, Boolean>> sync = mManager.mURLRules;
			Pair<ArrayList<String>, Boolean> sync2 = sync.get(mManager.getURLId(URLUtilities.getBaseURL(url)));
			synchronized (sync) {
				//The robots.txt is still not ready
				while (!sync2.getValue()) {
					try {
						sync.wait();
						sync2 = sync.get(mManager.getURLId(URLUtilities.getBaseURL(url)));
					} catch (InterruptedException e) {
					}
				}
			}
		}
	}
	
	/**
	 * Returns true if the given url doesn't violate the robots.txt rules
	 *
	 * @param url
	 * @return
	 */
	public boolean isCrawlable(URL url) {
		generateRobotTxt(url);
		return mManager.isUrlAllowed(url);
	}
	
	
	/**
	 * Takes the parsed robots.txt and returns the Rules regarding the current User Agent
	 *
	 * @param parsedRobotsTxt
	 * @return
	 */
	private ArrayList<String> getUserAgentRules(HashMap<String, ArrayList<String>> parsedRobotsTxt) {
		//Concatanates the default user Agent rules with the current user Agent rules
		ArrayList<String> ret = parsedRobotsTxt.get("*");
		if (mManager.mUserAgent != "*") {
			ret.addAll(parsedRobotsTxt.get(mManager.mUserAgent));
		}
		return ret;
	}
	
	/**
	 * Takes lines of the robots.txt and parses it
	 *
	 * @param robotTxt
	 * @return
	 */
	private HashMap<String, ArrayList<String>> parseRobotsText(ArrayList<String> robotTxt) {
		HashMap<String, ArrayList<String>> parsedRobotTxt = new HashMap<>();
		String curUserAgent = null;
		
		//loop on each line of the file if it starts with user-agent then it's a new user-agent
		//if it starts with allow/disallow then it's a new rule for the current user-agent
		for (String line : robotTxt) {
			if (line.startsWith("user-agent")) {
				curUserAgent = line.split(":")[1].trim();
				if (!parsedRobotTxt.containsKey(curUserAgent))
					parsedRobotTxt.put(curUserAgent, new ArrayList<>());
			} else if (curUserAgent != null && (line.startsWith("disallow") || line.startsWith("allow"))) {
				ArrayList<String> rules = parsedRobotTxt.get(curUserAgent);
				rules.add(line);
			}
		}
		
		return parsedRobotTxt;
	}
}
