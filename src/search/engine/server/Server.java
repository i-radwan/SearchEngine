package search.engine.server;

import org.bson.Document;
import org.bson.types.ObjectId;
import search.engine.indexer.Indexer;
import search.engine.indexer.WebPage;
import search.engine.ranker.Ranker;
import search.engine.utils.Constants;
import search.engine.utils.Utilities;
import spark.Request;
import spark.Response;

import javax.print.Doc;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static spark.Spark.get;
import static spark.SparkBase.externalStaticFileLocation;
import static spark.SparkBase.port;


public class Server {

    private static Indexer sIndexer = new Indexer();

    /**
     * Starts serving the clients.
     */
    public static void serve() {
        // Setup static files link
        externalStaticFileLocation(System.getProperty("user.dir") + "/client");

        // Server
        port(Constants.SEARCH_ENGINE_PORT_NUMBER);

        // Main html page
        get("/", (Request req, Response res) -> {
            res.redirect("index.html");
            return null;
        });

        // Search end point
        get("/search", (Request req, Response res) -> {
            String queryString = req.queryParams("q");
            String pageNumber = req.queryParams("page");

            queryString = queryString.substring(0, Math.min(queryString.length(), Constants.QUERY_MAX_LENGTH));

            // Check for empty queries
            if (queryString == null || queryString.trim().isEmpty()) {
                return "app.webpagesCallBack({error_msg:\"Please fill valid query!\"})";
            }

            if (queryString.trim().length() <= 3) {
                return "app.webpagesCallBack({error_msg:\"For better results, use longer query!\"})";
            }

            // Check for empty page number
            if (pageNumber == null || pageNumber.trim().isEmpty() || !pageNumber.matches("\\d+")) {
                pageNumber = "1";
            }

            // Process the query
            boolean isPhraseSearch = (queryString.startsWith("\"") && queryString.endsWith("\""));
            queryString = Utilities.processString(queryString);
            List<String> queryWords = Arrays.asList(queryString.split(" "));
            List<String> queryStems = Utilities.stemWords(queryWords);

            // Get results from the indexer
            List<WebPage> allMatchedResults;

            if (isPhraseSearch) {
                allMatchedResults = sIndexer.searchByPhrase(queryWords);
            } else {
                queryWords = Utilities.removeStopWords(queryWords);
                allMatchedResults = sIndexer.searchByWord(queryStems);
            }

            if (allMatchedResults.isEmpty()) {
                return "app.webpagesCallBack({error_msg:\"No matching, please use different query!\"})";
            }

            // Add suggestion
            sIndexer.insertSuggestion(queryString);

            // Call the ranker
            Ranker ranker = new Ranker(sIndexer, allMatchedResults, queryWords, queryStems);
            List<ObjectId> rankedWebPagesIds = ranker.rank(Integer.valueOf(pageNumber));
            List<WebPage> results = sIndexer.searchById(rankedWebPagesIds, Constants.FIELDS_FOR_SEARCH_RESULTS);

            List<Document> pagesDocuments = new ArrayList<>();

            for (ObjectId id : rankedWebPagesIds) {
                for (WebPage webPage : results) {
                    if (webPage.id.equals(id)) {
                        Document doc = new Document()
                                .append("title", webPage.title)
                                .append("url", webPage.url)
                                .append("content", webPage.content);

                        pagesDocuments.add(doc);
                        break;
                    }
                }
            }

            int pagesCount = ((allMatchedResults.size() + Constants.SINGLE_PAGE_RESULTS_COUNT - 1) /
                    Constants.SINGLE_PAGE_RESULTS_COUNT);

            Document paginationDocument = new Document()
                    .append("pages_count", pagesCount)
                    .append("current_page", Integer.parseInt(pageNumber));

            Document webpagesResponse = new Document()
                    .append("pages", pagesDocuments)
                    .append("pagination", paginationDocument);

            return "app.webpagesCallBack(" + webpagesResponse.toJson() + ")";
        });

        // Suggestions endpoint
        get("/suggestions", (Request req, Response res) -> {
            String queryString = Utilities.processString(req.queryParams("q"));
            queryString = queryString.substring(0, Math.min(queryString.length(), Constants.QUERY_MAX_LENGTH));

            // Get suggestions from the indexer
            List<String> suggestions = sIndexer.getSuggestions(queryString);

            return "app.suggestionsCallBack(" + suggestions.toString() + ")";
        });
    }
}
