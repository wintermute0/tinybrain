package yatan.deeplearning.wordembedding.data;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.apache.log4j.Logger;

import scala.actors.threadpool.Arrays;

import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.wiki.dao.ArticleDao;
import yatan.wiki.dao.DaoException;
import yatan.wiki.dao.mysql.ArticleDaoImpl;
import yatan.wiki.entities.Article;

@Deprecated
public class EnWikiTrainingDataProducer {
    private static final int LOAD_ARTICLE_BATCH_SIZE = 500;
    public static final int WINDOWS_SIZE = 11;
    private static final int MAX_ARTICLE_ID = 30000000;
    private static final int INSTANCE_CACHE_SIZE = 100000;
    private Dictionary dictionary;

    private final List<WordEmbeddingTrainingInstance> INSTANCES_CATCH = new ArrayList<WordEmbeddingTrainingInstance>();

    private Logger logger = Logger.getLogger(EnWikiTrainingDataProducer.class);

    private ArticleDao articleDao;

    private Random random = new Random(new Date().getTime());

    public List<WordEmbeddingTrainingInstance> produceInstances(int size) throws TrainingDataProducerException {
        if (INSTANCES_CATCH.size() < size) {
            loadToCache();
        }

        List<WordEmbeddingTrainingInstance> instances = new ArrayList<WordEmbeddingTrainingInstance>();
        for (int i = 0; i < size; i++) {
            instances.add(INSTANCES_CATCH.remove(0));
        }
        return instances;
    }

    @SuppressWarnings("unchecked")
    private void loadToCache() throws TrainingDataProducerException {
        this.logger.info("Loading training data from database...");
        try {
            prepareArticleDao();
            while (INSTANCES_CATCH.size() < INSTANCE_CACHE_SIZE) {
                List<Article> articles =
                        this.articleDao.getArticlesByIdRange(this.random.nextInt(MAX_ARTICLE_ID), MAX_ARTICLE_ID,
                                LOAD_ARTICLE_BATCH_SIZE);
                for (Article article : articles) {
                    Scanner scanner = new Scanner(article.getText());
                    while (scanner.hasNextLine()) {
                        // clean line
                        String line = scanner.nextLine().trim();
                        if (line.startsWith("|") || line.startsWith("{") || line.startsWith("==")
                                || line.startsWith("<!") || line.matches("^\\[\\[.+?\\]\\]$")
                                || line.toUpperCase().startsWith("#REDIRECT") || line.startsWith("*")
                                || line.startsWith("}}") || line.startsWith("!")) {
                            continue;
                        }

                        line =
                                line.replaceAll("<!--.*?-->", "").replaceAll("<ref.*?>.*?</ref>", "")
                                        .replaceAll("</ref>", "").replaceAll("<ref.+?/>", "")
                                        .replaceAll("\\[\\[[^\\]]+?\\|", "").replaceAll("\\[\\[", "")
                                        .replaceAll("\\]\\]", "").replaceAll("\\[.+?\\]", "")
                                        .replaceAll("\\{\\{.+?\\}\\}", "").replaceAll("&nbsp;", " ");

                        if (line.length() < 100) {
                            continue;
                        }

                        // tokenize line
                        String[] sentences = line.split("[\\.\\?]");
                        for (String sentence : sentences) {
                            sentence = sentence.replaceAll("[\\s\\,\\?\\+!\\(\\)\\.\\'\\+\"]+", " ");
                            String[] originalWords = sentence.split(" ");
                            if (originalWords.length < WINDOWS_SIZE / 2) {
                                continue;
                            }

                            List<String> words = new ArrayList<String>(Arrays.asList(originalWords));
                            List<Integer> wordIndecies = new ArrayList<Integer>();
                            for (int i = 0; i < WINDOWS_SIZE / 2; i++) {
                                words.add(0, Dictionary.PADDING_WORD);
                                words.add(Dictionary.PADDING_WORD);
                            }
                            for (String word : words) {
                                wordIndecies.add(this.dictionary.indexOf(word));
                            }

                            for (int i = WINDOWS_SIZE / 2; i < originalWords.length + WINDOWS_SIZE / 2; i++) {
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
                                    } else {
                                        negativeInstance.getInput().add(wordIndecies.get(j));
                                    }
                                }
                                positiveInstance.setOutput(1);
                                negativeInstance.setOutput(-1);

                                INSTANCES_CATCH.add(positiveInstance);
                                INSTANCES_CATCH.add(negativeInstance);
                            }
                        }
                    }
                }
            }
            this.logger.info("Done.");
        } catch (SQLException e) {
            this.articleDao = null;
            throw new TrainingDataProducerException("Error occurred while reading training data.", e);
        } catch (DaoException e) {
            this.articleDao = null;
            throw new TrainingDataProducerException("Error occurred while reading training data.", e);
        }
    }

    private void prepareArticleDao() throws SQLException {
        if (this.articleDao == null) {
            this.articleDao = new ArticleDaoImpl();
            ((ArticleDaoImpl) this.articleDao).setConnection(DriverManager.getConnection(
                    "jdbc:mysql://10.3.8.212/wiki", "root", "topcoder"));
        }
    }
}
