package search.engine.crawler;

import javafx.util.Pair;

import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class Input {
	private static Scanner mURLFile, mVisitedURLsFile, mDisallowedURLsFile, mAllowedURLsFile, mURLIdsFile, mURLRulesFile, mSeedFile;

	public static void init() {
		try {
			mURLFile = new Scanner(new FileReader(Constants.URLS_FILE_NAME));
			mVisitedURLsFile = new Scanner(new FileReader(Constants.VISITED_URLS_FILE_NAME));
			mDisallowedURLsFile = new Scanner(new FileReader(Constants.DISALLOWED_URLS_FILE_NAME));
			mAllowedURLsFile = new Scanner(new FileReader(Constants.ALLOWED_URLS_FILE_NAME));
			mURLIdsFile = new Scanner(new FileReader(Constants.URL_IDS_FILE_NAME));
			mURLRulesFile = new Scanner(new FileReader(Constants.URL_RULES_FILE_NAME));
			mSeedFile = new Scanner(new FileReader(Constants.SEED_FILE_NAME));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads the URL seeds and fills the URLs queue and the visited URLs set
	 * @param webUrls
	 * @param visitedUrls
	 */
	public static void readSeed(BlockingQueue<String> webUrls, ConcurrentSkipListSet<String> visitedUrls) {
		while(mSeedFile.hasNextLine()) {
			String s = mSeedFile.nextLine();
			webUrls.add(s);
			visitedUrls.add(s);
		}
	}

	/**
	 * Reads the previous runs data in case of interruption to continue from where it left of
	 * @param webUrls
	 * @param visitedUrls
	 * @param allowedUrls
	 * @param disallowedUrls
	 * @param urlRules
	 * @param urlIds
	 * @param baseUrlCnt
	 */
	public static void readPreviousRunData(BlockingQueue<String> webUrls, Set<String> visitedUrls, Set<Integer> allowedUrls, Set<Integer> disallowedUrls, ConcurrentHashMap<Integer, Pair<ArrayList<String>, Boolean>> urlRules, ConcurrentHashMap<String, Integer> urlIds, ConcurrentHashMap<String, Integer> baseUrlCnt) {
		visitedUrls.addAll(readVisitedURLs());
		baseUrlCnt.putAll(getBaseUrlCnt(visitedUrls));
		webUrls.addAll(readURLs(visitedUrls));
		allowedUrls.addAll(readAllowedURLs());
		disallowedUrls.addAll(readDisallowedURLs());
		urlIds.putAll(readURLIds());
		urlRules.putAll(readURLRules(urlIds));
	}

	/**
	 * Gets the Count of each website's webpages from the visited set
	 * @param visitedUrls
	 * @return
	 */
	private static ConcurrentHashMap<String, Integer> getBaseUrlCnt(Set<String> visitedUrls) {
		ConcurrentHashMap<String, Integer> ret = new ConcurrentHashMap<>();
		for(String url : visitedUrls) {
			try {
				URL tmp = new URL(url);
				ret.putIfAbsent(URLUtilities.getBaseURL(tmp), 0);
				Integer x = ret.get(URLUtilities.getBaseURL(tmp));
				ret.put(URLUtilities.getBaseURL(tmp), x+1);
			} catch (MalformedURLException e) {	}
		}
		return ret;
	}

	/**
	 * Takes in a scanner and reads the file into set of lines
	 * @param scanner
	 * @return
	 */
	private static Set<String> readFile(Scanner scanner) {
		Set<String> ret = new HashSet<>();
		while(scanner.hasNextLine()) {
			ret.add(scanner.nextLine());
		}
		return ret;
	}

	/**
	 * Takes in a scanner and reads the file into set of Integers
	 * @param scanner
	 * @return
	 */
	private static Set<Integer> readIntegers(Scanner scanner) {
		Set<Integer> ret = new HashSet<>();
		while(scanner.hasNextInt()) {
			ret.add(scanner.nextInt());
		}
		return ret;
	}

	/**
	 * Reads the URLs to be crawled from the URL file
	 * @param visitedURLs
	 * @return
	 */
	public static ArrayList<String> readURLs(Set<String> visitedURLs) {
		ArrayList<String> ret = new ArrayList<>();
		String s;
		while(mURLFile.hasNextLine()) {
			s = mURLFile.nextLine();

			if (!visitedURLs.contains(s)) {
				ret.add(s);
				visitedURLs.add(s);
			}
		}
		return ret;
	}

	/**
	 * Reads the URLs that has been visited from the visited file
	 * @return
	 */
	public static Set<String> readVisitedURLs() {
		return readFile(mVisitedURLsFile);
	}

	/**
	 * Reads the URLs that are allowed to be crawled by the robots.txt
	 * @return
	 */
	public static Set<Integer> readAllowedURLs() {
		return readIntegers(mAllowedURLsFile);
	}

	/**
	 * Reads the URLs that are disallowed to be crawled by the robots.txt
	 * @return
	 */
	public static Set<Integer> readDisallowedURLs() {
		return readIntegers(mDisallowedURLsFile);
	}

	/**
	 * Reads the URLs with their ids in order to know each URL id for the RobotsTextManager
	 * @return
	 */
	public static ConcurrentHashMap<String, Integer> readURLIds() {
		ConcurrentHashMap<String, Integer> ret = new ConcurrentHashMap<>();
		while(mURLIdsFile.hasNextLine()) {
			Integer id = mURLIdsFile.nextInt();
			mURLIdsFile.nextLine();
			String url = mURLIdsFile.nextLine();
			ret.put(url, id);
		}
		return ret;
	}

	/**
	 * Reads for each URL its rules that has been gotten from the robots.txt
	 * @param UrlIds
	 * @return
	 */
	public static ConcurrentHashMap<Integer, Pair<ArrayList<String>, Boolean>> readURLRules(ConcurrentHashMap<String, Integer> UrlIds) {
		ConcurrentHashMap<Integer, Pair<ArrayList<String>, Boolean>> ret = new ConcurrentHashMap<>();
		String curUrl;
		Integer curId = 0;
		while(mURLRulesFile.hasNextLine()) {
			String line = mURLRulesFile.nextLine();
			if (line.startsWith(Constants.INIT_URL_RULE_FILE)) {
				curUrl = line.substring(Constants.INIT_URL_RULE_FILE.length(), line.length());
				curId = UrlIds.get(curUrl);
				ret.put(curId, new Pair<>(new ArrayList<>(), true));
			}
			else {
				ret.get(curId).getKey().add(line);
			}
		}
		return ret;
	}


	/**
	 * Closes the opened files after finish
	 */
	public static void closeFiles() {
		mURLFile.close();
		mVisitedURLsFile.close();
		mDisallowedURLsFile.close();
		mAllowedURLsFile.close();
		mURLIdsFile.close();
		mURLRulesFile.close();
	}
}
