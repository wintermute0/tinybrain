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
    private static final int MAX_ARTICLE_ID = 30000000;
    private Random random = new Random(new Date().getTime());

    @Inject
    public ZhWikiTrainingDataProducer(Dictionary dictionary) {
        Preconditions.checkArgument(dictionary != null);

        this.dictionary = dictionary;
    }

    @Override
    public List<Data> produceData(int size) throws DataProducerException {
        this.logger.info("Loading training data from database...");
        try {
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
                        line = line.replaceAll("\\{\\{.+?\\}\\}", "");
                        if (line.length() < 100) {
                            continue;
                        }

                        // tokenize line
                        String[] sentences = line.split("[\\。\\！\\？]");
                        for (String sentence : sentences) {
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
                                if (wordIndecies.get(i) == this.dictionary
                                        .indexOf(Dictionary.NO_SUCH_WORD_PLACE_HOLDER)) {
                                    continue;
                                }

                                WordEmbeddingTrainingInstance positiveInstance = new WordEmbeddingTrainingInstance();
                                WordEmbeddingTrainingInstance negativeInstance = new WordEmbeddingTrainingInstance();
                                positiveInstance.setInput(new ArrayList<Integer>());
                                negativeInstance.setInput(new ArrayList<Integer>());
                                for (int j = i - WINDOWS_SIZE / 2; j < i + WINDOWS_SIZE / 2 + 1; j++) {
                                    positiveInstance.getInput().add(wordIndecies.get(j));
                                    if (j == i) {
                                        negativeInstance.getInput().add(this.random.nextInt(this.dictionary.size()));
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
                                negativeInstance.setOutput(0);

                                int noSuchWordCount = 0;
                                for (int index : positiveInstance.getInput()) {
                                    if (index == this.dictionary.indexOf(Dictionary.NO_SUCH_WORD_PLACE_HOLDER)) {
                                        noSuchWordCount++;
                                    }
                                }
                                if (noSuchWordCount > 0) {
                                    continue;
                                }

                                INSTANCES_CATCH.add(new Data(positiveInstance));
                                INSTANCES_CATCH.add(new Data(negativeInstance));
                            }
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

    private void prepareArticleDao() throws SQLException {
        if (this.articleDao == null) {
            this.articleDao = new ArticleDaoImpl();
            ((ArticleDaoImpl) this.articleDao).setConnection(DriverManager.getConnection(
                    "jdbc:mysql://10.3.8.212/wiki_zh", "root", "topcoder"));
        }
    }
}
