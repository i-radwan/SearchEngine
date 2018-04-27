package search.engine.ranker;

import org.bson.types.ObjectId;
import search.engine.indexer.Indexer;
import search.engine.indexer.StemInfo;
import search.engine.indexer.WebPage;
import search.engine.utils.Constants;

import java.util.ArrayList;
import java.util.List;


public class Ranker {

    //
    // Member variables
    //
    Indexer mIndexer;

    List<WebPage> mWebPages;
    List<String> mQueryWords;
    List<String> mQueryStems;

    long mTotalDocsCount;
    long mWordsDocsCount[];
    long mStemsDocsCount[];

    //
    // Member methods
    //

    /**
     * Constructs a ranker object for the given web pages and search query.
     *
     * @param indexer    a database mIndexer object to get web page statistics
     * @param webPages   the web pages to rank and sort
     * @param queryWords the user's search query after polishing
     * @param queryStems the user's search query after stemming the words
     */
    public Ranker(Indexer indexer, List<WebPage> webPages, List<String> queryWords, List<String> queryStems) {
        mIndexer = indexer;

        mWebPages = webPages;
        mQueryWords = queryWords;
        mQueryStems = queryStems;

        retrieveDocumentsCount();
    }

    /**
     * Ranks the given web pages based on the given search query words
     * and returns a paginated results.
     *
     * @param pageNumber the pagination page number
     * @return list of sorted paginated web pages
     */
    public List<ObjectId> rank(int pageNumber) {
        // For each page calculate its TF-IDF score
        for (WebPage webPage : mWebPages) {
            webPage.rank = calculatePageScore(webPage);
        }

        // Sort webPages
        mWebPages.sort((p1, p2) -> Double.compare(p2.rank, p1.rank));

        List<ObjectId> ret = new ArrayList<>();

        int idx = Constants.SINGLE_PAGE_RESULTS_COUNT * (pageNumber - 1);
        int cnt = Math.min(mWebPages.size() - idx, Constants.SINGLE_PAGE_RESULTS_COUNT);

        while (cnt-- > 0) {
            ret.add(mWebPages.get(idx++).id);
        }

        return ret;
    }

    /**
     * Retrieves the web pages documents count for each of the
     * search query words and stems, along with the total number of documents in the database.
     * <p>
     * Fill:
     * <ul>
     * <li>{@code mWordsDocsCount}</li>
     * <li>{@code mStemsDocsCount}</li>
     * <li>{@code mTotalDocsCount}</li>
     * </ul>
     */
    private void retrieveDocumentsCount() {
        // Get the total number of documents in the database
        mTotalDocsCount = mIndexer.getDocumentsCount();

        // Get the total number of documents containing each of the search query words
        mWordsDocsCount = new long[mQueryWords.size()];
        mStemsDocsCount = new long[mQueryWords.size()];

        for (int i = 0; i < mQueryWords.size(); ++i) {
            mWordsDocsCount[i] = mIndexer.getWordDocumentsCount(mQueryWords.get(i));
            mStemsDocsCount[i] = mIndexer.getStemDocumentsCount(mQueryStems.get(i));
        }
    }

    /**
     * Calculates the given web page score rank based on the users's search query
     * and the relevance of the content of the page.
     * The score is calculated as the sum of product of the web page TF and IDF
     * for each search query word.
     *
     * @param webPage the web page to calculate its score
     * @return the calculated web page score
     */
    private double calculatePageScore(WebPage webPage) {
        double pageScore = 0.0; // TF-IDF score
        int foundWordsCount = 0;

        // For each word in the query filter words
        for (int i = 0; i < mQueryWords.size(); ++i) {
            String word = mQueryWords.get(i);
            String stem = mQueryStems.get(i);

            List<Integer> positions = webPage.wordPosMap.get(word);
            StemInfo stemInfo = webPage.stemMap.getOrDefault(stem, new StemInfo(0, 0));

            int wordCnt = (positions == null ? 0 : positions.size());
            int stemCnt = stemInfo.count;
            double TF, IDF, score = 0, wordScore = 0;

            // Exact word
            if (wordCnt > 0) {
                TF = wordCnt / (double) webPage.wordsCount;
                IDF = Math.log((double) mTotalDocsCount / mWordsDocsCount[i]);

                score += TF * IDF;

                foundWordsCount++;
            }

            // Synonymous words
            if (stemCnt > 0) {
                TF = stemCnt / (double) webPage.wordsCount;
                IDF = Math.log((double) mTotalDocsCount / mStemsDocsCount[i]);

                score += (TF * IDF) * 0.5;

                wordScore = (double) stemInfo.score / stemCnt;
            }

            // Add the effect of the normalized score of the word
            // The word score is related to its occurrences in the HTML
            pageScore += score * wordScore;
        }

        return (pageScore + webPage.rank) * (foundWordsCount);
    }

    /**
     * Calculates the given web page score rank based on the users's search query
     * and the relevance of the content of the page.
     * The score is calculated as the cosine similarity score between query words vector
     * and the document words vector.
     *
     * @param webPage the web page to calculate its score
     * @return the calculated web page score
     */
    private double calculatePageScoreCosineSimilarity(WebPage webPage) {
        // Calculate cosine similarity score.
        int queryWordsCnt = mQueryWords.size();
        int foundWordsCount = 0;

        double dotProduct = 0.0;
        double queryVectorMagnitude = 0.0;
        double pageVectorMagnitude = 0.0;

        // For each word in the query filter words
        for (int i = 0; i < queryWordsCnt; ++i) {
            String word = mQueryWords.get(i);
            String stem = mQueryStems.get(i);

            //
            // Query Word Score
            //
            double queryScoreComponent = Math.log((double) mTotalDocsCount / mWordsDocsCount[i]) / queryWordsCnt;
            queryVectorMagnitude += queryScoreComponent * queryScoreComponent;

            //
            // Page Content Relevance
            //
            List<Integer> positions = webPage.wordPosMap.get(word);
            StemInfo stemInfo = webPage.stemMap.getOrDefault(stem, new StemInfo(0, 0));

            int wordCnt = (positions == null ? 0 : positions.size());
            int stemCnt = stemInfo.count;
            double TF, IDF, wordScore = (double) stemInfo.score / stemCnt;

            // Exact word
            if (wordCnt > 0) {
                TF = wordCnt / (double) webPage.wordsCount;
                IDF = Math.log((double) mTotalDocsCount / mWordsDocsCount[i]);

                double pageScoreComponent = TF * IDF * wordScore;
                pageVectorMagnitude += pageScoreComponent * pageScoreComponent;
                dotProduct += pageScoreComponent * queryScoreComponent;

                foundWordsCount++;
            }

            // Synonymous words
            if (stemCnt > 0) {
                TF = stemCnt / (double) webPage.wordsCount;
                IDF = Math.log((double) mTotalDocsCount / mStemsDocsCount[i]);

                double pageScoreComponent = TF * IDF * wordScore * 0.5;
                pageVectorMagnitude += pageScoreComponent * pageScoreComponent;
                dotProduct += pageScoreComponent * queryScoreComponent;
            }
        }

        queryVectorMagnitude = Math.sqrt(queryVectorMagnitude);
        pageVectorMagnitude = Math.sqrt(pageVectorMagnitude);

        double pageCosineSimilarityScore = dotProduct / (pageVectorMagnitude * queryVectorMagnitude);
        double pageScore = (1.0 * foundWordsCount / queryWordsCnt) * (pageCosineSimilarityScore) * (0.3 * webPage.rank);

        //System.out.println("Number of found words: " + foundWordsCount + " " + webPage.id + " " + pageScore);

        return pageScore;
    }
}