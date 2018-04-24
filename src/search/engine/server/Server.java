package search.engine.server;

import search.engine.indexer.Indexer;
import search.engine.utils.Constants;
import search.engine.utils.Utilities;
import spark.Request;
import spark.Response;

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

        // Setup server port number
        port(Constants.SEARCH_ENGINE_PORT_NUMBER);

        // Main html page
        get("/", (Request req, Response res) -> {
            res.redirect("index.html");
            return null;
        });

        //
        // Register search end-point
        //
        get("/search", (Request req, Response res) -> {
            String ret = "";

            try {
                QueryProcessor processor = new QueryProcessor(
                        sIndexer,
                        req.queryParams("q"),
                        req.queryParams("page")
                );

                ret = processor.getJsonResult();
            } catch (Exception e) {
                ret = "{error_msg:\"" + e.getMessage() + "\"}";
            }

            //
            System.out.println("-------------------------------------------");

            return "app.webpagesCallBack(" + ret + ")";
        });

        //
        // Register suggestions end-point
        //
        get("/suggestions", (Request req, Response res) -> {
            // Parse query string
            String queryString = Utilities.processString(req.queryParams("q"));

            // Get suggestions from the indexer
            List<String> suggestions = sIndexer.getSuggestions(queryString);

            return "app.suggestionsCallBack(" + suggestions.toString() + ")";
        });
    }
}
