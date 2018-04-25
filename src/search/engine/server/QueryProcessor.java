package search.engine.server;

import org.bson.Document;
import org.bson.types.ObjectId;
import search.engine.indexer.Indexer;
import search.engine.indexer.WebPage;
import search.engine.ranker.Ranker;
import search.engine.utils.Constants;
import search.engine.utils.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QueryProcessor {

    //
    // Member variables
    //
    private Indexer mIndexer;

    private int mPageNumber;
    private boolean mIsPhraseSearch;
    private String mOriginalQuery;
    private String mQuery;
    private List<String> mQueryWords;
    private List<String> mQueryStems;

    private int mTotalResultsCount;
    private List<ObjectId> mRankedIds;
    private List<WebPage> mResults;

    //
    // Member methods
    //

    /**
     * Constructs a query processor
     *
     * @param indexer    the indexer to search with
     * @param query      the user's raw search query to search for
     * @param pageNumber the required results page number for pagination
     * @throws Exception
     */
    public QueryProcessor(Indexer indexer, String query, String pageNumber) throws Exception {
        mIndexer = indexer;
        parseSearchQuery(query, pageNumber);
        searchAndRankResults();
    }

    /**
     * Returns the paginated matching results in JSON format.
     *
     * @return a JSON string with the matching results
     */
    public String getJsonResult() {
        List<Document> pagesDocuments = new ArrayList<>();

        SnippetExtractor snippetExtractor = new SnippetExtractor();

        for (ObjectId id : mRankedIds) {
            for (WebPage webPage : mResults) {
                if (!webPage.id.equals(id)) continue;

                String snippet = snippetExtractor.extractWebPageSnippet(webPage.content, mOriginalQuery);

                Document doc = new Document()
                        .append("title", webPage.title)
                        .append("url", webPage.url)
                        .append("snippet", snippet);

                pagesDocuments.add(doc);

                break;
            }
        }

        int pagesCount = ((mTotalResultsCount + Constants.SINGLE_PAGE_RESULTS_COUNT - 1) / Constants.SINGLE_PAGE_RESULTS_COUNT);

        Document paginationDocument = new Document()
                .append("pages_count", pagesCount)
                .append("current_page", mPageNumber);

        Document webPagesResponse = new Document()
                .append("pages", pagesDocuments)
                .append("pagination", paginationDocument);

        return webPagesResponse.toJson();
    }

    /**
     * Parses the user's raw search query and generate a stemmed version of it
     * needed in ranking the results.
     *
     * @param rawQuery the user's raw search query to search for
     * @param number   the required results page number for pagination
     * @throws Exception when empty or short search query is used
     */
    private void parseSearchQuery(String rawQuery, String number) throws Exception {
        mOriginalQuery = mQuery = (rawQuery == null ? "" : rawQuery.trim());

        // Check if too short query
        if (mQuery.length() <= 3) {
            throw new Exception("Please fill a valid search query!");
        }

        //
        // Process the query string
        //
        mIsPhraseSearch = (mQuery.startsWith("\"") && mQuery.endsWith("\""));
        mQuery = mQuery.substring(0, Math.min(mQuery.length(), Constants.QUERY_MAX_LENGTH));
        mQuery = Utilities.processString(mQuery);
        mQueryWords = Arrays.asList(mQuery.split(" "));

        // Remove stop words from search query in normal search mode
        if (!mIsPhraseSearch) {
            mQueryWords = Utilities.removeStopWords(mQueryWords);
        }

        mQueryStems = Utilities.stemWords(mQueryWords);

        // Check if no words are left after processing
        if (mQueryWords.isEmpty()) {
            throw new Exception("Please fill a valid search query!");
        }

        // Print query words
        System.out.print("Search query words:\t ");
        for (String word : mQueryWords) {
            System.out.print(word + " ");
        }
        System.out.println();

        // Print query words
        System.out.print("Search query stems:\t ");
        for (String stem : mQueryStems) {
            System.out.print(stem + " ");
        }
        System.out.println();

        //
        // Parse page number
        //
        try {
            mPageNumber = Integer.parseInt(number);
        } catch (Exception e) {
            mPageNumber = 1;
        }
    }

    /**
     * Searches for web pages matching the user's search query and ranks the results.
     *
     * @throws Exception when no matching results
     */
    private void searchAndRankResults() throws Exception {
        //
        // Search for matching results
        //
        long now, startTime = System.nanoTime();

        List<WebPage> matchingResults;

        if (mIsPhraseSearch) {
            matchingResults = mIndexer.searchByPhrase(mQueryWords, mQueryStems);
        } else {
            matchingResults = mIndexer.searchByWord(mQueryWords, mQueryStems);
        }

        //
        now = System.nanoTime();
        System.out.printf("Search time:\t %.04f sec\n", (now - startTime) / 1e9);
        startTime = now;

        mTotalResultsCount = matchingResults.size();

        if (matchingResults.isEmpty()) {
            throw new Exception("No matching results, please try a different search query!");
        }

        //
        // Save search query for later suggestions
        //
        mIndexer.insertSuggestion(mQuery);

        //
        // Rank matching results
        //
        Ranker ranker = new Ranker(mIndexer, matchingResults, mQueryWords, mQueryStems);
        mRankedIds = ranker.rank(mPageNumber);
        mResults = mIndexer.searchById(mRankedIds, Constants.FIELDS_FOR_SEARCH_RESULTS);

        //
        now = System.nanoTime();
        System.out.printf("Ranking time:\t %.04f sec\n", (now - startTime) / 1e9);

        //
        System.out.printf("Total results:\t %d\n", mTotalResultsCount);
    }
}
