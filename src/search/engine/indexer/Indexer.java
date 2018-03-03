package search.engine.indexer;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Updates.set;
import static com.mongodb.client.model.Updates.setOnInsert;


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
    // TODO: think of better names for the functions. xD

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

        // Get web pages collection from the database
        mWebPagesCollection = database.getCollection(Constants.COLLECTION_WEB_PAGES);
    }

    /**
     * Indexes the given web page in the search engine inverted database.
     *
     * @param url      Web page url
     * @param content  Web page content
     * @param words    List of parsed words.
     * @param scores   List of scores for the parsed words.
     * @param outLinks List of urls the given page is pointing to.
     */
    public void indexWebPage(String url, String content, List<String> words, List<Integer> scores, List<String> outLinks) {
        // Filter document by web page url
        Document filter = new Document(Constants.FIELD_URL, url);

        // Add upsert option
        UpdateOptions options = new UpdateOptions().upsert(true);

        // Create the web page document to be indexed
        Document page = new Document()
                .append(Constants.FIELD_URL, url)
                .append(Constants.FIELD_RANK, 1)
                .append(Constants.FIELD_CONNECTED_TO, getConnectedWebPages(outLinks))
                .append(Constants.FIELD_PAGE_CONTENT, content)
                .append(Constants.FIELD_WORDS_COUNT, words.size())
                .append(Constants.FIELD_DICTIONARY, getDictionary(words, scores));

        // Replace or create new document in the web pages collection
        UpdateResult res = mWebPagesCollection.replaceOne(
                filter,
                page,
                options
        );

        // Log results
        System.out.println(page);
        System.out.println(res);
    }

    /**
     * Returns the dictionary index for the given web page content.
     *
     * @param words  List of parsed words.
     * @param scores List of scores for the parsed words.
     * @return List of document representing the dictionary of the given web page.
     */
    private List<Document> getDictionary(List<String> words, List<Integer> scores) {
        // Map holds list of positions, scores for each distinct word
        Map<String, ArrayList<Integer>> wordPosMap = new TreeMap<>();
        Map<String, ArrayList<Integer>> wordScoreMap = new TreeMap<>();

        // Fill the map
        for (int i = 0; i < words.size(); ++i) {
            String word = words.get(i);

            wordPosMap.putIfAbsent(word, new ArrayList<>());
            wordPosMap.get(word).add(i);

            wordScoreMap.putIfAbsent(word, new ArrayList<>());
            wordScoreMap.get(word).add(scores.get(i));
        }

        // List of word documents
        ArrayList<Document> dictionary = new ArrayList<>();

        for (String word : wordPosMap.keySet()) {
            Document doc = new Document()
                    .append(Constants.FIELD_WORD, word)
                    .append(Constants.FIELD_POSITIONS, wordPosMap.get(word))
                    .append(Constants.FIELD_SCORES, wordScoreMap.get(word));

            dictionary.add(doc);
        }

        return dictionary;
    }

    /**
     * Returns a list of web pages ids matching the given urls.
     *
     * @param links List of urls the web page is pointing to.
     * @return List of web pages ids.
     */
    private List<ObjectId> getConnectedWebPages(List<String> links) {
        List<ObjectId> ret = new ArrayList<>();

        for (String url : links) {
            ret.add(getWebPageId(url));
        }

        return ret;
    }

    /**
     * Updates the web pages ranks according to the given inputs.
     *
     * @param pageRanks the new ranks to be updated.
     */
    public void updatePageRanks(Map<ObjectId, Integer> pageRanks) {
        List<WriteModel<Document>> operations = new ArrayList<>();

        for (ObjectId id : pageRanks.keySet()) {
            operations.add(new UpdateOneModel<>(
                    eq(Constants.FIELD_ID, id),
                    set(Constants.FIELD_RANK, pageRanks.get(id))
            ));
        }

        mWebPagesCollection.bulkWrite(operations);
    }

    /**
     * Returns the indexed web pages graph.
     *
     * @return The web graph represented as adjacency list.
     */
    public Map<ObjectId, List<ObjectId>> getWebGraph() {
        FindIterable<Document> ret = mWebPagesCollection
                .find()
                .projection(fields(include(
                        Constants.FIELD_ID,
                        Constants.FIELD_CONNECTED_TO
                )));

        Map<ObjectId, List<ObjectId>> edges = new HashMap<>();

        for (Document doc : ret) {
            edges.put(
                    doc.getObjectId(Constants.FIELD_ID),
                    (List<ObjectId>) doc.get(Constants.FIELD_CONNECTED_TO) // TODO: find a better way to cast
            );
        }

        return edges;
    }

    /**
     * Returns the id of the given web page url.
     * If the url is not found then a new entry will be inserted in the database.
     * TODO: think about concurrency
     *
     * @param url Web page url.
     * @return The id of the given web page.
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
     * @param ids         List of web pages ids to search for.
     * @param projections The desired fields to be returned.
     * @return List of matching web pages.
     */
    public List<Document> searchById(List<ObjectId> ids, String... projections) {
        List<Document> ret = new ArrayList<>();

        mWebPagesCollection
                .find(in(
                        Constants.FIELD_ID,
                        ids
                ))
                .projection(include(
                        projections
                ))
                .into(ret);

        return ret;
    }

    /**
     * Searches for web pages having any of the given filter words.
     *
     * @param filterWords List of words to search for.
     * @return List of matching web pages.
     */
    public List<Document> searchByWord(List<String> filterWords) {
        List<Document> ret = new ArrayList<>();

        mWebPagesCollection
                .find(in(
                        Constants.FIELD_DICTIONARY + "." + Constants.FIELD_WORD,
                        filterWords
                ))
                .projection(include(
                        Constants.FIELD_ID,
                        Constants.FIELD_URL,
                        Constants.FIELD_RANK,
                        Constants.FIELD_WORDS_COUNT,
                        Constants.FIELD_DICTIONARY
                ))
                .into(ret);

        return ret;
    }

    /**
     * Searches for web pages having all of the given filter words.
     *
     * @param filterWords List of words to search for.
     * @return List of matching web pages.
     */
    public List<Document> searchByPhrase(List<String> filterWords) {
        List<Document> ret = new ArrayList<>();

        mWebPagesCollection
                .find(all(
                        Constants.FIELD_DICTIONARY + "." + Constants.FIELD_WORD,
                        filterWords
                ))
                .projection(include(
                        Constants.FIELD_ID,
                        Constants.FIELD_URL,
                        Constants.FIELD_RANK,
                        Constants.FIELD_WORDS_COUNT,
                        Constants.FIELD_DICTIONARY
                ))
                .into(ret);

        return ret;
    }
}
