package search.engine.main;

import org.jsoup.nodes.Document;
import search.engine.crawler.Crawler;
import search.engine.indexer.Indexer;
import search.engine.models.WebPage;
import search.engine.server.Server;
import search.engine.utils.WebUtilities;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;


public class Main {

    private static Scanner scanner;

    public static void main(String[] args) throws IOException {
        scanner = new Scanner(System.in);

        int choice = -1;

        while (choice == -1) {
            System.out.println("Please enter a unit to test:");
            System.out.println("1. Crawler");
            System.out.println("2. Indexer");
            System.out.println("3. Ranker");
            System.out.println("4. Query Processor");
            System.out.println("5. Web Page Parser");
            System.out.println("6. Exit");

            choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    testCrawler();
                    break;
                case 2:
                    testIndexer();
                    break;
                case 3:
                    testRanker();
                    break;
                case 4:
                    testQueryProcessor();
                    break;
                case 5:
                    testWebPageParse();
                    break;
                case 6:
                    System.out.println("Bye!");
                    break;
                default:
                    System.out.println("Invalid choice");
                    choice = -1;
                    break;
            }
        }

        scanner.close();
    }

    private static void testCrawler() {
        System.out.println("Please enter the number of crawler threads: ");
        Crawler crawler = new Crawler();
        crawler.start(scanner.nextInt());
    }

    private static void testIndexer() {
        Indexer indexer = new Indexer();

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

    private static void testRanker() {

    }

    private static void testQueryProcessor() {
        // Server
        Server.serve();
    }

    private static void testWebPageParse() throws IOException {
        Document doc = WebUtilities.fetchWebPage("http://codeforces.com/problemset/problem/950/B");
        System.out.println("Fetched");
        WebPage page = new WebPage(doc);

        PrintWriter writer = new PrintWriter(new FileWriter("data/tmp.txt"));

        //
        // Web page URL
        //
        writer.printf("Web Page URL:\n\t%s\n\n", page.url);

        //
        // Out links
        //
        writer.printf("Out Links:\n");
        int idx = 0;
        for (String link : page.outLinks) {
            writer.printf("\t%-3d: %s\n", idx++, link);
        }
        writer.printf("\n");

        //
        // Web page info
        //
        writer.printf("Web Page Content:\n\t%s\n\n", page.content);
        writer.printf("Web Page Parsed Words Count:\n\t%d\n\n", page.wordsCount);
        writer.printf("Web Page Distinct Parsed Words Count:\n\t%d\n\n", page.wordPosMap.size());

        //
        // Word index
        //
        writer.printf("Words Index:\n");
        for (String word : page.wordPosMap.keySet()) {
            writer.printf("\t%s\n", word);
            writer.printf("\t\tPositions:\t");
            for (int pos : page.wordPosMap.get(word)) {
                writer.printf("%d\t", pos);
            }
            writer.printf("\n");
            writer.printf("\t\tScores:\t\t");
            for (int score : page.wordScoreMap.get(word)) {
                writer.printf("%d\t", score);
            }
            writer.printf("\n\n\n");
        }
        writer.printf("\n");


        writer.close();
    }
}
