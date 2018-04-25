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
import java.util.Comparator;
import java.util.List;

public class QueryProcessor {

    //
    // Member variables
    //
    private Indexer mIndexer;

    private int mPageNumber;
    private String mQuery;
    private String mOriginalQuery;
    private boolean mIsPhraseSearch;
    private List<String> mOriginalQueryWords;
    private List<String> mOriginalQueryStems;
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
     * @param indexer the indexer to search with
     */
    public QueryProcessor(Indexer indexer) {
        mIndexer = indexer;
    }

    /**
     * Prepares the results for the given search query.
     *
     * @param query      the user's raw search query to search for
     * @param pageNumber the required results page number for pagination
     * @throws Exception
     */
    public void process(String query, String pageNumber) throws Exception {
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

        for (ObjectId id : mRankedIds) {
            for (WebPage webPage : mResults) {
                if (webPage.id.equals(id)) {
                    Document doc = new Document()
                            .append("title", webPage.title)
                            .append("url", webPage.url)
                            .append("snippet", extractWebpageSnippet(webPage.content));

                    pagesDocuments.add(doc);
                    break;
                }
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
        mQuery = (rawQuery == null ? "" : rawQuery).trim();

        // Check if too short query
        if (mQuery.length() <= 3) {
            throw new Exception("Please fill a valid search query!");
        }

        //
        // Process the mOriginalQuery string, for snippet extraction purposes
        //
        mOriginalQuery = Utilities.removeSpecialCharsAroundWord(mQuery);

        mOriginalQuery = mOriginalQuery
                .substring(0, Math.min(mOriginalQuery.length(), Constants.QUERY_MAX_LENGTH))
                .toLowerCase();

        mOriginalQueryWords = Arrays.asList(mOriginalQuery.split(" "));
        for (int i = 0; i < mOriginalQueryWords.size(); i++) {
            mOriginalQueryWords.set(i, Utilities.removeSpecialCharsAroundWord(mOriginalQueryWords.get(i)));
        }

        mOriginalQueryStems = Utilities.stemWords(mOriginalQueryWords);

        //
        // Process the mQuery string
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

    /**
     * Extracts the important snippets from the given webpage content
     *
     * @param content the webpage content string
     * @return concatenated webpage snippets
     */
    private String extractWebpageSnippet(String content) {
        ArrayList<Snippet> nominatedSnippets = getNominatedSnippets(content);

        // Sort by snippet length desc. (this will be better, and manages phrases too)
        nominatedSnippets.sort(Comparator.comparingInt(s -> (s.L - s.R)));

        // Select top snippets w.r.t. size
        List<Snippet> selectedSnippets = getSelectedSnippets(nominatedSnippets);

        // Sort again by L to print them in order
        selectedSnippets.sort(Comparator.comparingInt(s -> s.L));

        // Concatenate small snippets
        String pageSnippet = concatenateSnippets(selectedSnippets);

        return completeSnippetFilling(content, pageSnippet, selectedSnippets);
    }

    private String completeSnippetFilling(String content, String snippet, List<Snippet> selectedSnippets) {
        // Escaping route
        if (selectedSnippets.size() == 0) {
            snippet = content.substring(0, Constants.MAX_SNIPPETS_CHARS_COUNT) + "...";
        } else if (snippet.length() < Constants.MAX_SNIPPETS_CHARS_COUNT) {
            // Fill more to show full-like snippet
            int beginIndex = selectedSnippets.get(selectedSnippets.size() - 1).R + 1;

            snippet += "..." + content.substring(
                    beginIndex,
                    Math.min(
                            content.length(),
                            beginIndex + Constants.MAX_SNIPPETS_CHARS_COUNT - snippet.length() + 1
                    )
            );
        }

        return snippet;
    }

    private List<Snippet> getSelectedSnippets(ArrayList<Snippet> nominatedSnippets) {
        return nominatedSnippets.subList(0,
                Math.min(Constants.MAX_SNIPPETS_COUNT, nominatedSnippets.size())
        );
    }

    private String concatenateSnippets(List<Snippet> selectedSnippets) {
        String snippet = "...";

        // Concatenate to get page snippet
        for (int i = 0; i < selectedSnippets.size(); ++i) {
            snippet += selectedSnippets.get(i).str;
            snippet += (i < selectedSnippets.size() - 1) ? "..." : "";
        }

        return snippet;
    }

    private ArrayList<Snippet> getNominatedSnippets(String content) {
        ArrayList<Snippet> nominatedSnippets = new ArrayList<>();

        String[] pageContentArray = content.split(" ");
        int pageContentArrayLength = pageContentArray.length;

        int lastKeywordIdx = -3;

        for (int key = 0; key < pageContentArrayLength; ++key) {
            String word = prepareWordForSnippet(pageContentArray[key]);

            if (mOriginalQueryStems.indexOf(word) == -1) continue;

            Snippet snippet = new Snippet();
            Snippet lastSnippet = null;
            int snippetStringStartIdx;

            if (!nominatedSnippets.isEmpty()) lastSnippet = nominatedSnippets.get(nominatedSnippets.size() - 1);

            // New snippet
            //ToDo 2 to constant
            if (key - lastKeywordIdx > 2) {
                snippet.L = Math.max(key - 2, (lastSnippet != null ? lastSnippet.R + 1 : 0));
                snippet.R = Math.min(pageContentArrayLength - 1, key + 2);

                snippetStringStartIdx = snippet.L;

                nominatedSnippets.add(snippet);
            }
            // Merge snippets
            else {
                int oldLastSnippetR = lastSnippet.R;
                lastSnippet.R = Math.min(pageContentArrayLength - 1, key + 2);

                snippet = lastSnippet;
                snippetStringStartIdx = oldLastSnippetR + 1;
            }

            // Separate newly added words from the previous snippet.str words
            if (snippet == lastSnippet)
                snippet.str += " ";

            // Add/Update snippet words
            for (int i = snippetStringStartIdx; i <= snippet.R; ++i) {
                String tmpWord = Utilities.stemWord(
                        Utilities.removeSpecialCharsAroundWord(pageContentArray[i])
                ).toLowerCase();

                boolean isKeyword = (mOriginalQueryStems.indexOf(tmpWord) > -1);

                snippet.str += (isKeyword) ? "<b>" : "";
                snippet.str += pageContentArray[i];
                snippet.str += (isKeyword) ? "</b>" : "";
                snippet.str += (i < snippet.R) ? " " : "";
            }

            lastKeywordIdx = key;
        }

        return nominatedSnippets;
    }

    private String prepareWordForSnippet(String word) {
        return Utilities.stemWord(Utilities.removeSpecialCharsAroundWord(word)).toLowerCase();
    }

    private class Snippet {
        String str = "";
        int L, R;
    }
}
