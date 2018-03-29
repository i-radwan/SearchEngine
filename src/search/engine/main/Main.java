package search.engine.main;

import org.jsoup.nodes.Document;
import search.engine.crawler.Crawler;
import search.engine.indexer.Indexer;
import search.engine.models.WebPage;
import search.engine.server.Server;
import search.engine.utils.Utilities;
import search.engine.utils.WebPageParser;
import search.engine.utils.WebUtilities;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class Main {

    private static Scanner scanner;

    public static void main(String[] args) throws IOException {
        scanner = new Scanner(System.in);

        int choice = -1;

        while (choice == -1) {
            System.out.println("Please enter a function to run:");
            System.out.println("1. Crawler");
            System.out.println("2. Clear Database");
            System.out.println("3. Test Indexer");
            System.out.println("4. Test Ranker");
            System.out.println("5. Test Query Processor");
            System.out.println("6. Test Web Page Parser");
            System.out.println("7. Exit");

            choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    startCrawler();
                    break;
                case 2:
                    clearDatabase();
                    break;
                case 3:
                    testIndexer();
                    break;
                case 4:
                    testRanker();
                    break;
                case 5:
                    testQueryProcessor();
                    break;
                case 6:
                    testWebPageParse();
                    break;
                case 7:
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

    /**
     * Start running the crawling process.
     */
    private static void startCrawler() {
        System.out.println("Please enter the number of crawler threads: ");
        int cnt = scanner.nextInt();

        Indexer indexer = new Indexer();
        Crawler crawler = new Crawler(indexer);
        crawler.start(cnt);

//        crawler.readPreviousData();
//
//        while (true) {
//            crawler.start(cnt);
//        }
    }

    /**
     * Clears our database and recreate the indexes.
     */
    private static void clearDatabase() {
        System.out.println("Clearing search engine database...");
        Indexer.migrate();
        System.out.println("Done!");
    }

    private static void testIndexer() {

    }

    private static void testRanker() {

    }

    private static void testQueryProcessor() {
        // Server
        Server.serve();
    }

    private static void testWebPageParse() throws IOException {
        URL url = new URL("http://codeforces.com/problemset/problem/950/B");
        Document doc = WebUtilities.fetchWebPage(url.toString());
        System.out.println("Fetched");

        WebPageParser parser = new WebPageParser();
        WebPage page = parser.parse(url, doc);

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
            writer.printf("\n\n");
        }
        writer.printf("\n");

        //
        // Web Page Dictionary
        //
        writer.printf("Words Dictionary:\n");
        Map<String, List<String>> dictionary = Utilities.getWordsDictionary(page.wordPosMap.keySet());
        for (String stem : dictionary.keySet()) {
            writer.printf("\t%s\n", stem);
            writer.printf("\t\tSynonyms:\t\t");
            for (String word : dictionary.get(stem)) {
                writer.printf("%s\t", word);
            }
            writer.printf("\n\n");
        }

        writer.close();
    }
}
