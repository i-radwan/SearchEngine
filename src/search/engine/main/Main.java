package search.engine.main;

import search.engine.query_processor.QueryProcessor;

import java.util.ArrayList;

import static spark.Spark.*;

public class Main {

    public static void main(String[] args) {
        // Server
        port(8080);

        get("/search/:q", (req, res) -> {
            String queryString = req.params(":q");

            QueryProcessor queryProcessor = new QueryProcessor();

            ArrayList<ArrayList<String>> queriesList = queryProcessor.process(queryString);

            return queryString;
        });
    }
}
