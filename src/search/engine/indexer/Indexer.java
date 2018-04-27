package search.engine.indexer;

import com.mongodb.MongoClient;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import search.engine.crawler.Output;
import search.engine.utils.Constants;

import java.net.URL;
import java.util.*;


public class Indexer {

    //
    // Member variables
    //

    /**
     * Mongo database collections
     */
    private MongoCollection<Document> mWebPagesCollection;
    private MongoCollection<Document> mDictionaryCollection;
    private MongoCollection<Document> mSuggestionsCollection;

    /**
     * Lock object used to lock bulk upsertions.
     */
    private final Object mLock = new Object();

    // ===========================================================================
    //
    // Web Pages Collection methods
    //

    /**
     * Clears the current search engine database and recreate the indexes.
     */
    public static void reset() {
        // Create Mongo Client
        MongoClient mongoClient = new MongoClient(
                Constants.DATABASE_HOST_ADDRESS,
                Constants.DATABASE_PORT_NUMBER
        );

        // Drop database
        mongoClient.dropDatabase(Constants.DATABASE_NAME);

        // Get search engine database
        MongoDatabase database = mongoClient.getDatabase(Constants.DATABASE_NAME);

        //
        MongoCollection<Document> collection;
        IndexOptions indexOptions = new IndexOptions().unique(true);

        // Create web page collection and its indexes
        collection = database.getCollection(Constants.COLLECTION_WEB_PAGES);
        collection.createIndex(Indexes.ascending(Constants.FIELD_URL), indexOptions);
        collection.createIndex(Indexes.ascending(Constants.FIELD_WORDS_INDEX + "." + Constants.FIELD_TERM));
        collection.createIndex(Indexes.ascending(Constants.FIELD_STEMS_INDEX + "." + Constants.FIELD_TERM));

        // Create dictionary collection and its indexes
        database.createCollection(Constants.COLLECTION_DICTIONARY);
        collection = database.getCollection(Constants.COLLECTION_DICTIONARY);
        collection.createIndex(Indexes.ascending(Constants.FIELD_TERM), indexOptions);

        // Create suggestions collection and its indexes
        database.createCollection(Constants.COLLECTION_SUGGESTIONS);
        collection = database.getCollection(Constants.COLLECTION_SUGGESTIONS);
        collection.createIndex(Indexes.ascending(Constants.FIELD_SUGGESTION), indexOptions);
    }

    /**
     * Constructs indexer object.
     */
    public Indexer() {
        // Create Mongo Client
        MongoClient mongoClient = new MongoClient(
                Constants.DATABASE_HOST_ADDRESS,
                Constants.DATABASE_PORT_NUMBER
        );

        // Get Search engine database
        MongoDatabase database = mongoClient.getDatabase(
                Constants.DATABASE_NAME
        );

        // Get collections from the database
        mWebPagesCollection = database.getCollection(Constants.COLLECTION_WEB_PAGES);
        mDictionaryCollection = database.getCollection(Constants.COLLECTION_DICTIONARY);
        mSuggestionsCollection = database.getCollection(Constants.COLLECTION_SUGGESTIONS);
    }

    /**
     * Indexes the given web page document in the search engine inverted database asynchronously.
     * Starts a new thread to parse and index the web page in the database.
     *
     * @param url      the web page URL object
     * @param pageDoc  the web page raw content
     * @param outLinks the web page out links
     * @param prvPage  the previous version of the web page, retrieved from the database
     */
    public void indexWebPageAsync(URL url, org.jsoup.nodes.Document pageDoc, List<String> outLinks, WebPage prvPage) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                indexWebPage(url, pageDoc, outLinks, prvPage);
            }
        });

        t.setName("Indexer-Thread\t");
        t.start();
    }

    /**
     * Indexes the given web page document in the search engine inverted database.
     *
     * @param url      the web page URL object
     * @param pageDoc  the web page raw content
     * @param outLinks the web page out links
     * @param prvPage  the previous version of the web page, retrieved from the database
     */
    public void indexWebPage(URL url, org.jsoup.nodes.Document pageDoc, List<String> outLinks, WebPage prvPage) {
        // Parse the raw web page document
        WebPageParser parser = new WebPageParser();
        WebPage curPage = parser.parse(url, pageDoc);

        curPage.outLinks = outLinks;
        curPage.rank = prvPage.rank;
        curPage.fetchSkipLimit = prvPage.fetchSkipLimit;
        curPage.fetchSkipCount = 0;

        // Compare the newly fetched page with its previous version from the database.
        if (curPage.wordsCount == prvPage.wordsCount
                && curPage.title.equals(prvPage.title)
                && curPage.wordPosMap.equals(prvPage.wordPosMap)
                && curPage.stemMap.equals(prvPage.stemMap)) {

            // If no changes happens to the content of the web page then
            // increase the skip fetch limit and return
            curPage.fetchSkipLimit = Math.min(Constants.MAX_FETCH_SKIP_LIMIT, prvPage.fetchSkipLimit * 2);
            updateFetchSkipLimit(prvPage.id, curPage.fetchSkipLimit);

            Output.log("Same page content : " + curPage.url);
            System.out.println("Same page content : " + curPage.url);
            return;
        }

        // Insert new content in the database
        updateWebPage(curPage);
        // updateWordsDictionary(Utilities.getWordsDictionary(curPage.wordPosMap.keySet()));

        //
        Output.log("Indexed : " + curPage.url);
        System.out.println("Indexed: " + curPage.url);
    }

    /**
     * Inserts the given web page in the search engine inverted database.
     *
     * @param page a web page to be indexed or updated
     */
    public void updateWebPage(WebPage page) {
        // Replace or create new document in the web pages collection
        mWebPagesCollection.replaceOne(
                Filters.eq(Constants.FIELD_URL, page.url),  // Filter document by web page url
                page.toDocument(),                  // Create the web page document to be indexed
                new UpdateOptions().upsert(true)    // Add upsert option
        );
    }

    /**
     * Increments the fetch skip count of the given web page.
     * Used to mange the frequency of fetching the content of the web page.
     *
     * @param id the web page id to update
     */
    public void incrementFetchSkipCount(ObjectId id) {
        mWebPagesCollection.updateOne(
                Filters.eq(Constants.FIELD_ID, id),
                Updates.inc(Constants.FILED_FETCH_SKIP_COUNT, 1)
        );
    }

    /**
     * Updates the fetch skip limit of the given web page and
     * resets the fetch skip count.
     * Used to mange the frequency of fetching the content of the web page.
     *
     * @param id the web page id to update
     */
    public void updateFetchSkipLimit(ObjectId id, int limit) {
        mWebPagesCollection.updateOne(
                Filters.eq(Constants.FIELD_ID, id),
                Updates.combine(
                        Updates.set(Constants.FILED_FETCH_SKIP_LIMIT, limit),
                        Updates.set(Constants.FILED_FETCH_SKIP_COUNT, 0)
                )
        );
    }

    /**
     * Removes the given web page from the indexer.
     *
     * @param id the web page id to remove
     */
    public void removeWebPage(ObjectId id) {
        mWebPagesCollection.deleteOne(Filters.eq(Constants.FIELD_ID, id));
    }

    /**
     * Updates the web pages ranks according to the given inputs.
     *
     * @param pages list of web pages after updating their ranks
     */
    public void updatePageRanks(Collection<WebPage> pages) {
        List<WriteModel<Document>> operations = new ArrayList<>();

        for (WebPage page : pages) {
            operations.add(new UpdateOneModel<>(
                    Filters.eq(Constants.FIELD_ID, page.id),
                    Updates.set(Constants.FIELD_RANK, page.rank)
            ));
        }

        if (operations.isEmpty()) {
            return;
        }

        mWebPagesCollection.bulkWrite(operations);
    }

    /**
     * Returns the indexed web pages graph as a list of web pages.
     * Each web page has a list of connected web pages.
     *
     * @return the web graph represented as adjacency list
     */
    public Map<String, WebPage> getWebGraph() {
        FindIterable<Document> res = mWebPagesCollection
                .find()
                .projection(Projections.include(
                        Constants.FIELD_URL,
                        Constants.FIELD_CONNECTED_TO
                ));

        Map<String, WebPage> map = new HashMap<>();

        for (Document doc : res) {
            WebPage page = new WebPage(doc);
            map.put(page.url, page);
        }

        return map;
    }

    /**
     * Returns the number of web pages documents saved in the database.
     *
     * @return documents count
     */
    public long getDocumentsCount() {
        return mWebPagesCollection.count();
    }

    /**
     * Returns the number of web pages documents saved in the database
     * containing the given filter word in the word index.
     *
     * @param word the word to search for
     * @return documents count
     */
    public long getWordDocumentsCount(String word) {
        return mWebPagesCollection.count(Filters.eq(
                Constants.FIELD_WORDS_INDEX + "." + Constants.FIELD_TERM,
                word
        ));
    }

    /**
     * Returns the number of web pages documents saved in the database
     * containing the given filter word in the stem index.
     *
     * @param stem the stem word to search for
     * @return documents count
     */
    public long getStemDocumentsCount(String stem) {
        return mWebPagesCollection.count(Filters.eq(
                Constants.FIELD_STEMS_INDEX + "." + Constants.FIELD_TERM,
                stem
        ));
    }

    /**
     * Searches for a specific web pages by their ids.
     *
     * @param url         the web page url string to search for
     * @param projections the desired fields to be returned. To return all fields just pass null
     * @return the matching web page, or null if not exist
     */
    public WebPage getWebPageByURL(String url, List<String> projections) {
        Document res = mWebPagesCollection
                .find(Filters.eq(Constants.FIELD_URL, url))
                .projection(Projections.include(projections))
                .first();

        return new WebPage(res);
    }

    /**
     * Searches for a specific web pages by their ids.
     *
     * @param ids         list of web pages ids to search for
     * @param projections the desired fields to be returned. To return all fields just pass null
     * @return list of matching web pages
     */
    public List<WebPage> searchById(List<ObjectId> ids, List<String> projections) {
        FindIterable<Document> res = mWebPagesCollection
                .find(Filters.in(Constants.FIELD_ID, ids))
                .projection(Projections.include(projections));

        return IndexerUtilities.toWebPages(res);
    }

    /**
     * Searches for web pages having any of the given filter words.
     *
     * @param filterWords list of search query words
     * @param filterStems list of search query stems
     * @return list of matching web pages
     */
    public List<WebPage> searchByWord(List<String> filterWords, List<String> filterStems) {
        // Query filter
        Bson filter = Filters.in(
                Constants.FIELD_STEMS_INDEX + "." + Constants.FIELD_TERM,
                filterStems
        );

        // Filter words index array
        Document projectWords = IndexerUtilities.AggregationFilter(
                Constants.FIELD_WORDS_INDEX, Constants.FIELD_TERM, filterWords);

        // Filter stems index array
        Document projectStems = IndexerUtilities.AggregationFilter(
                Constants.FIELD_STEMS_INDEX, Constants.FIELD_TERM, filterStems);

        // Initial fields projections
        Bson projections = Projections.fields(
                Projections.include(Constants.FIELDS_FOR_SEARCH_RANKING),
                projectWords, projectStems
        );

        // Retrieve results
        AggregateIterable<Document> res = mWebPagesCollection.aggregate(Arrays.asList(
                Aggregates.match(filter),
                Aggregates.project(projections)
        ));

        return IndexerUtilities.toWebPages(res);
    }

    /**
     * Searches for web pages having all of the given filter words in the given order.
     *
     * @param filterWords list of search query words
     * @param filterStems list of search query stems
     * @return list of matching web pages
     */
    public List<WebPage> searchByPhrase(List<String> filterWords, List<String> filterStems) {
        // Query filter
        Bson filter = Filters.all(
                Constants.FIELD_WORDS_INDEX + "." + Constants.FIELD_TERM,
                filterWords
        );

        // Filter words index array
        Document projectWords = IndexerUtilities.AggregationFilter(
                Constants.FIELD_WORDS_INDEX, Constants.FIELD_TERM, filterWords);

        // Filter stems index array
        Document projectStems = IndexerUtilities.AggregationFilter(
                Constants.FIELD_STEMS_INDEX, Constants.FIELD_TERM, filterStems);

        // Initial fields projections
        Bson projections = Projections.fields(
                Projections.include(Constants.FIELDS_FOR_SEARCH_RANKING),
                projectWords, projectStems
        );

        // Retrieve results
        AggregateIterable<Document> res = mWebPagesCollection.aggregate(Arrays.asList(
                Aggregates.match(filter),
                Aggregates.project(projections)
        ));

        // Return only web pages having the whole phrase occurred in the same given order
        List<WebPage> ret = new ArrayList<>();

        for (Document doc : res) {
            WebPage page = new WebPage(doc);

            if (IndexerUtilities.checkPhraseOccurred(page.wordPosMap, filterWords)) {
                ret.add(page);
            }
        }

        return ret;
    }

    // ===========================================================================
    //
    // Dictionary Collection methods
    //

    /**
     * Updates the words dictionary in the database.
     * <p>
     * Words dictionary is a map from a stemmed word to
     * all the words having the same stem.
     *
     * @param dictionary the dictionary map form a word to its synonyms to update
     */
    public void updateWordsDictionary(Map<String, List<String>> dictionary) {
        List<WriteModel<Document>> operations = new ArrayList<>();

        // Add upsert option
        UpdateOptions options = new UpdateOptions().upsert(true);

        for (Map.Entry<String, List<String>> it : dictionary.entrySet()) {
            operations.add(new UpdateOneModel<>(
                    Filters.eq(Constants.FIELD_TERM, it.getKey()),
                    Updates.addEachToSet(Constants.FILED_SYNONYMS, it.getValue()),
                    options
            ));
        }

        if (operations.isEmpty()) {
            return;
        }

        synchronized (mLock) {
            // Synchronization needed due to a MongoDB issue reported here:
            // https://jira.mongodb.org/browse/SERVER-14322
            mDictionaryCollection.bulkWrite(operations);
        }
    }

    /**
     * Returns the dictionary of the given list of words.
     *
     * @param words list of words to get their dictionary
     * @return the dictionary map form a word to its synonyms
     */
    public Map<String, List<String>> getWordsDictionary(List<String> words) {
        Map<String, List<String>> dictionary = new HashMap<>();

        FindIterable<Document> res = mDictionaryCollection.find(Filters.in(Constants.FIELD_TERM, words));

        for (Document doc : res) {
            dictionary.put(
                    doc.getString(Constants.FIELD_TERM),
                    (List<String>) doc.get(Constants.FILED_SYNONYMS)
            );
        }

        return dictionary;
    }

    // ===========================================================================
    //
    // Suggestions Collection methods
    //

    public void insertSuggestion(String suggestion) {
        mSuggestionsCollection.updateOne(
                Filters.eq(Constants.FIELD_SUGGESTION, suggestion),
                Updates.set(Constants.FIELD_SUGGESTION, suggestion),
                new UpdateOptions().upsert(true)
        );
    }

    /**
     * Returns all suggestions in the database that
     * the given search query is prefix from.
     *
     * @param query the search query
     * @return a list of matched suggestions sorted in ascending order
     */
    public List<String> getSuggestions(String query) {
        List<String> ret = new ArrayList<>();

        FindIterable<Document> res = mSuggestionsCollection
                .find(Filters.regex(Constants.FIELD_SUGGESTION, query + ".*"))
                .projection(Projections.excludeId())
                .sort(Sorts.ascending(Constants.FIELD_SUGGESTION));

        for (Document doc : res) {
            ret.add("\"" + doc.getString(Constants.FIELD_SUGGESTION) + "\"");
        }

        return ret;
    }
}