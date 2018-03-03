package search.engine.main;

import search.engine.query_processor.QueryProcessor;

import java.util.ArrayList;

import static spark.Spark.*;

public class Main {

    public static void main(String[] args) {
        // Server
        port(8080);

        get("/search", (req, res) -> {
            String queryString = req.queryParams("q");

            QueryProcessor queryProcessor = new QueryProcessor();

            ArrayList<ArrayList<String>> queriesList = queryProcessor.process(queryString);
            boolean isPhraseSearch = queryProcessor.isPhraseSearch();

            // Call the indexer

            // Call the ranker

            return queryString;
        });
    }
}
