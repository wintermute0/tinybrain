package yatan.deeplearning.wordembedding.data;

import java.sql.DriverManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import yatan.deeplearning.wordembedding.model.Dictionary;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.contract.data.impl.DataProducer;
import yatan.distributedcomputer.contract.data.impl.DataProducerException;
import yatan.wiki.dao.ArticleDao;
import yatan.wiki.dao.DaoException;
import yatan.wiki.dao.mysql.ArticleDaoImpl;
import yatan.wiki.entities.Article;

public class ZhWikiTrainingDataProducer implements DataProducer {
    private final Logger logger = Logger.getLogger(ZhWikiTrainingDataProducer.class);
    private final List<Data> INSTANCES_CATCH = Lists.newArrayList();
    private final Dictionary dictionary;

    private ArticleDao articleDao;

    public static final int WINDOWS_SIZE = 11;
    private static final int LOAD_ARTICLE_BATCH_SIZE = 500;
    private static final int MAX_ARTICLE_ID = 3449448;
    private Random random = new Random(new Date().getTime());

    public static int FREQUENCEY_RANK_BOUND = -1;

    @Inject
    public ZhWikiTrainingDataProducer(Dictionary dictionary) {
        Preconditions.checkArgument(dictionary != null);

        this.dictionary = dictionary;
    }

    @Override
    public List<Data> produceData(int size) throws DataProducerException {
        this.logger.info("Loading training data from database...");
        try {
            // WikiModel wikiModel =
            // new WikiModel("http://www.mywiki.com/wiki/${image}", "http://www.mywiki.com/wiki/${title}");
            prepareArticleDao();
            while (INSTANCES_CATCH.size() < size) {
                List<Article> articles =
                        this.articleDao.getArticlesByIdRange(this.random.nextInt(MAX_ARTICLE_ID), MAX_ARTICLE_ID,
                                LOAD_ARTICLE_BATCH_SIZE);
                for (Article article : articles) {
                    Scanner scanner = new Scanner(article.getText());
                    while (scanner.hasNextLine()) {
                        // clean line
                        String line = scanner.nextLine().trim();
                        // line = wikiModel.render(new PlainTextConverter(), line);
                        line = line.replaceAll("\\{\\{.+?\\}\\}", "");
                        System.out.println(line);
                        if (line.length() < 100) {
                            continue;
                        }

                        // tokenize line
                        String sentence = line;
                        if (sentence.length() < WINDOWS_SIZE / 2) {
                            continue;
                        }

                        List<String> words = Lists.newArrayList();
                        for (int i = 0; i < sentence.length(); i++) {
                            words.add(String.valueOf(sentence.charAt(i)));
                        }
                        List<Integer> wordIndecies = new ArrayList<Integer>();
                        for (int i = 0; i < WINDOWS_SIZE / 2; i++) {
                            words.add(0, Dictionary.PADDING_WORD);
                            words.add(Dictionary.PADDING_WORD);
                        }
                        for (String word : words) {
                            wordIndecies.add(this.dictionary.indexOf(word));
                        }

                        for (int i = WINDOWS_SIZE / 2; i < sentence.length() + WINDOWS_SIZE / 2; i++) {
                            if (wordIndecies.get(i) == this.dictionary.indexOf(Dictionary.NO_SUCH_WORD_PLACE_HOLDER)) {
                                continue;
                            }

                            WordEmbeddingTrainingInstance positiveInstance = new WordEmbeddingTrainingInstance();
                            WordEmbeddingTrainingInstance negativeInstance = new WordEmbeddingTrainingInstance();
                            positiveInstance.setInput(new ArrayList<Integer>());
                            negativeInstance.setInput(new ArrayList<Integer>());
                            for (int j = i - WINDOWS_SIZE / 2; j < i + WINDOWS_SIZE / 2 + 1; j++) {
                                // check if this word meet the frequency lower bound

                                positiveInstance.getInput().add(wordIndecies.get(j));
                                if (j == i) {
                                    int negativeWord =
                                            this.dictionary
                                                    .sampleWordUniformlyAboveFrequenceRank(FREQUENCEY_RANK_BOUND);
                                    while (negativeWord == wordIndecies.get(j)) {
                                        negativeWord =
                                                this.dictionary
                                                        .sampleWordUniformlyAboveFrequenceRank(FREQUENCEY_RANK_BOUND);
                                    }
                                    negativeInstance.getInput().add(negativeWord);
                                    // int negativeWord = this.dictionary.sampleWord();
                                    // while (negativeWord == wordIndecies.get(j)) {
                                    // negativeWord = this.dictionary.sampleWord();
                                    // }
                                    // negativeInstance.getInput().add(negativeWord);
                                } else {
                                    negativeInstance.getInput().add(wordIndecies.get(j));
                                }
                            }
                            positiveInstance.setOutput(1);
                            negativeInstance.setOutput(-1);

                            boolean invalidWindow = false;
                            for (int index : positiveInstance.getInput()) {
                                invalidWindow = isWordInvalid(index);
                                if (invalidWindow) {
                                    break;
                                }
                            }

                            if (invalidWindow) {
                                continue;
                            }

                            INSTANCES_CATCH.add(new Data(positiveInstance));
                            INSTANCES_CATCH.add(new Data(negativeInstance));

                        }
                    }
                }
            }
            this.logger.info("Done.");
        } catch (SQLException e) {
            this.articleDao = null;
            throw new DataProducerException("Error occurred while reading training data.", e);
        } catch (DaoException e) {
            this.articleDao = null;
            throw new DataProducerException("Error occurred while reading training data.", e);
        }

        List<Data> data = Lists.newArrayList(INSTANCES_CATCH.subList(0, size));
        INSTANCES_CATCH.subList(0, size).clear();
        Collections.shuffle(data);
        return data;
    }

    private boolean isWordInvalid(int wordIndex) {
        return wordIndex == this.dictionary.indexOf(Dictionary.NO_SUCH_WORD_PLACE_HOLDER)
                || (FREQUENCEY_RANK_BOUND > 0 && this.dictionary.frenquencyRank(wordIndex) > FREQUENCEY_RANK_BOUND);
    }

    private void prepareArticleDao() throws SQLException {
        if (this.articleDao == null) {
            this.articleDao = new ArticleDaoImpl();
            ((ArticleDaoImpl) this.articleDao).setConnection(DriverManager.getConnection(
                    "jdbc:mysql://10.3.7.40:3306/wiki_zh", "topcoder", "topcoder"));
        }
    }
}
