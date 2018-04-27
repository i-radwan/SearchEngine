package search.engine.ranker;

import org.bson.types.ObjectId;
import search.engine.indexer.Indexer;
import search.engine.indexer.StemInfo;
import search.engine.indexer.WebPage;
import search.engine.utils.Constants;

import java.util.*;


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
            webPage.rank = calculatePageScoreCosineSimilarity(webPage);
        }

        // Sort webPages
        mWebPages.sort(new Comparator<WebPage>() {
            @Override
            public int compare(WebPage p1, WebPage p2) {
                return Double.compare(p2.rank, p1.rank);
            }
        });

        List<ObjectId> ret = new ArrayList<>();

        int idx = Constants.SINGLE_PAGE_RESULTS_COUNT * (pageNumber - 1);
        int cnt = Math.min(mWebPages.size() - idx, Constants.SINGLE_PAGE_RESULTS_COUNT);

        while (cnt-- > 0) {
            ret.add(mWebPages.get(idx++).id);
        }

//        System.out.println("SORTED");
//        for (ObjectId pageID : ret) {
//            System.out.println(pageID);
//        }

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
     * and the content of the page.
     * The score is calculated as the sum of product of the web page TF and IDF
     * for each search query word.
     *
     * @param webPage the web page to calculate its score
     * @return the calculated web page score
     */
    private double calculatePageScore(WebPage webPage) {
        double pageScore = 0.0; // TF-IDF score

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
            }

            // Synonymous words
            if (stemCnt > 0) {
                TF = stemCnt / (double) webPage.wordsCount;
                IDF = Math.log((double) mTotalDocsCount / mStemsDocsCount[i]);

                score += (TF * IDF) * 0.5;

                wordScore = (double) stemInfo.count / stemCnt;
            }

            // Add the effect of the normalized score of the word
            // The word score is related to its occurances in the HTML
            pageScore += score * wordScore;
        }

        return pageScore * (0.5 * webPage.rank);
    }

    /**
     * Calculates the given web page score rank based on the users's search query
     * and the content of the page.
     * The score is calculated as the cosine similarity score between query words vector
     * and the document words vector.
     * @param webPage the web page to calculate its score
     * @return the calculated web page score
     */
    private double calculatePageScoreCosineSimilarity(WebPage webPage) {
        // Calculate TF IDF for the query words.
        List<Double> queryScoreVector = new ArrayList<>();
        Integer queryWordsCnt = mQueryWords.size();
        Double queryVectorMagnitude = 0.0;

        // Calculate normalized TF-IDF score and the query vector magnitude.
        for (int i = 0; i < queryWordsCnt; i++) {
            Double component = 1.0 * Math.log((double) mTotalDocsCount / mWordsDocsCount[i]) / queryWordsCnt;
            queryScoreVector.add(component);
            queryVectorMagnitude += Math.pow(component, 2);
        }
        queryVectorMagnitude = Math.sqrt(queryVectorMagnitude);

        // Calculate cosine similarity score.
        double pageCosineSimilarityScore = 0.0;
        double dotProduct = 0.0;
        double pageVectorMagnitude = 0.0;

        int numberOfFoundWords = 0;
        // For each word in the query filter words
        for (int i = 0; i < queryWordsCnt; ++i) {
            String word = mQueryWords.get(i);
            String stem = mQueryStems.get(i);

            List<Integer> positions = webPage.wordPosMap.get(word);
            StemInfo stemInfo = webPage.stemMap.getOrDefault(stem, new StemInfo(0, 0));

            int wordCnt = (positions == null ? 0 : positions.size());
            int stemCnt = stemInfo.count;
            double TF, IDF;

            // Exact word
            if (wordCnt > 0) {
                numberOfFoundWords++;
                TF = wordCnt / (double) webPage.wordsCount;
                IDF = Math.log((double) mTotalDocsCount / mWordsDocsCount[i]);
                pageVectorMagnitude += Math.pow(TF * IDF, 2);
                dotProduct += TF * IDF * queryScoreVector.get(i);
            }

            // Synonymous words
            if (stemCnt > 0) {
                TF = stemCnt / (double) webPage.wordsCount;
                IDF = Math.log((double) mTotalDocsCount / mStemsDocsCount[i]);

                pageVectorMagnitude += Math.pow(TF * IDF * 0.5, 2);
                dotProduct += TF * IDF * 0.5 * queryScoreVector.get(i);
            }
        }
        pageVectorMagnitude = Math.sqrt(pageVectorMagnitude);

        //System.out.println("Number of found words: " + numberOfFoundWords + " " + webPage.id +" " + ((1.0 * numberOfFoundWords) + (0.7 * pageCosineSimilarityScore) + (0.5 * webPage.rank)) + " " + webPage.rank);

        pageCosineSimilarityScore = dotProduct / (pageVectorMagnitude * queryVectorMagnitude);
        return 1.0 * numberOfFoundWords/queryWordsCnt + 0.7 * pageCosineSimilarityScore + 0.5 * webPage.rank;
    }
}
