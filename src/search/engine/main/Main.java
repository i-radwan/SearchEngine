package search.engine.main;

import search.engine.crawler.Crawler;
import search.engine.indexer.Indexer;
import search.engine.indexer.WebPage;
import search.engine.server.Server;

import java.util.*;


public class Main {

    public static void main(String[] args) throws InterruptedException {
        //testQueryProcessor();
        //testCrawler();
        //testIndexer();
        //testRanker();
    }

    public static void testQueryProcessor() {
        // Server
        Server.serve();
    }

    public static void testCrawler() throws InterruptedException {
        Crawler crawler = new Crawler();
        crawler.start(5);
    }

    public static void testIndexer() {
        Indexer indexer = new Indexer();

        //
        // Add dummy data
        indexer.indexWebPage(new WebPage(
                "google.com",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum"
        ));

        indexer.indexWebPage(new WebPage(
                "codefoces.com",
                "hello everybody! Today, on March 4th 2018 the final round of Technocup olympiad for Russian-speaking high school students is held. The round starts at 11:30 Moscow time."
        ));

        indexer.indexWebPage(new WebPage(
                "test.com",
                "hello world hi Ibrahim Samir Eid"
        ));

        //
        // Testing web graph and updating ranks
        //
        Map<String, WebPage> map = indexer.getWebGraph();
        Collection<WebPage> collection = new ArrayList<>(map.values());
        for (WebPage page : collection) {
            page.rank = 9;
            System.out.println(page.toDocument());
        }
        indexer.updatePageRanks(map.values());

        //
        // Testing search
        //
        List<WebPage> list = indexer.searchByPhrase(Arrays.asList("hello", "world"));
        for (WebPage page : list) {
            System.out.println(page.toDocument());
        }
    }

    public static void testRanker() {

    }
}
