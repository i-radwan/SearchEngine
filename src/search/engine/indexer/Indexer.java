package search.engine.indexer;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import search.engine.models.WebPage;
import search.engine.utils.Constants;

import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Updates.*;


public class Indexer {
    //
    // Member variables
    //

    /**
     * Mongo database collection holding the indexed web pages.
     */
    private MongoCollection<Document> mWebPagesCollection;

    /**
     * Mongo database collection holding the crawled dictionary.
     */
    private MongoCollection<Document> mDictionaryCollection;

    //
    // Member methods
    //

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
    }

    /**
     * Indexes the given web page in the search engine inverted database.
     *
     * @param page a web page to be indexed or updated
     */
    public void indexWebPage(WebPage page) {
        // Filter document by web page url
        Document filter = new Document(Constants.FIELD_URL, page.url);
        // Create the web page document to be indexed
        Document update = page.toDocument();
        // Add upsert option
        UpdateOptions options = new UpdateOptions().upsert(true);

        // Replace or create new document in the web pages collection
        mWebPagesCollection.replaceOne(
                filter,
                update,
                options
        );
    }

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
                    eq(Constants.FIELD_WORD, it.getKey()),
                    addEachToSet(Constants.FILED_SYNONYMS, it.getValue()),
                    options
            ));
        }

        mDictionaryCollection.bulkWrite(operations);
    }

    /**
     * Returns the dictionary of the given list of words.
     *
     * @param words list of words to get their dictionary
     * @return the dictionary map form a word to its synonyms
     */
    public Map<String, List<String>> getWordsDictionary(List<String> words) {
        Map<String, List<String>> dictionary = new HashMap<>();

        FindIterable<Document> res = mDictionaryCollection.find(in(Constants.FIELD_WORD, words));

        for (Document doc : res) {
            dictionary.put(
                    doc.getString(Constants.FIELD_WORD),
                    (List<String>) doc.get(Constants.FILED_SYNONYMS)
            );
        }

        return dictionary;
    }

    /**
     * Updates the web pages ranks according to the given inputs.
     * TODO: To be used by @Samir
     *
     * @param pages list of web pages after updating their ranks
     */
    public void updatePageRanks(Collection<WebPage> pages) {
        List<WriteModel<Document>> operations = new ArrayList<>();

        for (WebPage page : pages) {
            operations.add(new UpdateOneModel<>(
                    eq(Constants.FIELD_ID, page.id),
                    set(Constants.FIELD_RANK, page.rank)
            ));
        }

        mWebPagesCollection.bulkWrite(operations);
    }

    /**
     * Returns the indexed web pages graph as a list of web pages.
     * Each web page has a list of connected web pages.
     * TODO: To be used by @Samir
     *
     * @return the web graph represented as adjacency list
     */
    public Map<String, WebPage> getWebGraph() {
        FindIterable<Document> res = mWebPagesCollection
                .find()
                .projection(include(
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
     * Returns the id of the given web page url.
     * If the url is not found then a new entry will be inserted in the database.
     * TODO: Think about concurrency
     * TODO: NOT USED! to be removed if not needed.
     *
     * @param url web page URL string
     * @return the id of the given web page
     */
    public ObjectId getWebPageId(String url) {
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
        options.upsert(true);
        options.returnDocument(ReturnDocument.AFTER);
        options.projection(include(Constants.FIELD_ID));

        Document doc = mWebPagesCollection.findOneAndUpdate(
                eq(Constants.FIELD_URL, url),
                setOnInsert(Constants.FIELD_URL, url),
                options
        );

        return doc.getObjectId(Constants.FIELD_ID);
    }

    /**
     * Searches for a specific web pages by their ids.
     *
     * @param ids         list of web pages ids to search for
     * @param projections the desired fields to be returned. To return all fields just pass null
     * @return list of matching web pages
     */
    public List<WebPage> searchById(List<ObjectId> ids, String... projections) {
        FindIterable<Document> res = mWebPagesCollection
                .find(in(Constants.FIELD_ID, ids))
                .projection(include(
                        projections
                ));

        return toWebPages(res);
    }

    /**
     * Searches for web pages having any of the given filter words.
     *
     * @param filterWords list of words to search for
     * @return list of matching web pages
     */
    public List<WebPage> searchByWord(List<String> filterWords) {
        FindIterable<Document> res = mWebPagesCollection
                .find(in(
                        Constants.FIELD_WORDS_INDEX + "." + Constants.FIELD_WORD,
                        filterWords
                ))
                .projection(include(
                        Constants.FIELD_ID,
                        Constants.FIELD_URL,
                        Constants.FIELD_RANK,
                        Constants.FIELD_WORDS_COUNT,
                        Constants.FIELD_WORDS_INDEX
                ));

        return toWebPages(res);
    }

    /**
     * Searches for web pages having all of the given filter words.
     *
     * @param filterWords list of words to search for
     * @return list of matching web pages
     */
    public List<WebPage> searchByPhrase(List<String> filterWords) {
        FindIterable<Document> res = mWebPagesCollection
                .find(all(
                        Constants.FIELD_WORDS_INDEX + "." + Constants.FIELD_WORD,
                        filterWords
                ))
                .projection(include(
                        Constants.FIELD_ID,
                        Constants.FIELD_URL,
                        Constants.FIELD_RANK,
                        Constants.FIELD_WORDS_COUNT,
                        Constants.FIELD_WORDS_INDEX
                ));

        return toWebPages(res);
    }

    /**
     * Converts the results of find query into a list of web pages.
     *
     * @param documents List of documents to be converted
     * @return list of web pages
     */
    private List<WebPage> toWebPages(FindIterable<Document> documents) {
        List<WebPage> ret = new ArrayList<>();

        for (Document doc : documents) {
            ret.add(new WebPage(doc));
        }

        return ret;
    }
}
