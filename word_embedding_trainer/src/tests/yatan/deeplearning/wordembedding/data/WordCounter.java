package yatan.deeplearning.wordembedding.data;

import java.io.FileInputStream;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import scala.annotation.bridge;

import yatan.wiki.dao.mysql.ArticleDaoImpl;
import yatan.wiki.entities.Article;

public class WordCounter {
    public static void main(String[] args) throws Exception {
        Set<String> dict = loadDictionary();
        int hitCount = 0;
        int wordCount = 0;

        final Map<String, Integer> wordCounts = new HashMap<String, Integer>();

        int i = 0;

        ArticleDaoImpl articleDao = new ArticleDaoImpl();
        articleDao.setConnection(DriverManager.getConnection("jdbc:mysql://localhost/wiki", "root", "topcoder"));
        for (Article article : articleDao.getArticlesByIdRange(2000000, 3000000, 1000)) {
            System.out.println(++i);
            Scanner scanner = new Scanner(article.getText());
            while (scanner.hasNextLine()) {
                // clean line
                String line = scanner.nextLine().trim();
                if (line.startsWith("|") || line.startsWith("{") || line.startsWith("==") || line.startsWith("<!")
                        || line.matches("^\\[\\[.+?\\]\\]$") || line.toUpperCase().startsWith("#REDIRECT")
                        || line.startsWith("*") || line.startsWith("}}") || line.startsWith("!")) {
                    continue;
                }

                line =
                        line.replaceAll("<!--.*?-->", "").replaceAll("<ref.*?>.*?</ref>", "").replaceAll("</ref>", "")
                                .replaceAll("<ref.+?/>", "").replaceAll("\\[\\[[^\\]]+?\\|", "")
                                .replaceAll("\\[\\[", "").replaceAll("\\]\\]", "").replaceAll("\\[.+?\\]", "")
                                .replaceAll("\\{\\{.+?\\}\\}", "").replaceAll("&nbsp;", " ");

                if (line.length() < 100) {
                    continue;
                }

                // tokenize line
                line = line.replaceAll("[\\s\\,\\?\\+!\\(\\)\\.\\'\\+\"]+", " ");
                for (String word : line.split(" ")) {
                    wordCount++;
                    if (dict.contains(word)) {
                        hitCount++;
                        System.out.print("-");
                    } else {
                        System.out.print("*");
                    }
                    /*
                    word = word.toLowerCase();
                    if (wordCounts.containsKey(word)) {
                        wordCounts.put(word, wordCounts.get(word) + 1);
                    } else {
                        wordCounts.put(word, 1);
                    }*/
                }
                System.out.println();
            }
        }
        System.out.println(1.0 * hitCount / wordCount);
        /*
        List<String> words = new ArrayList<String>(wordCounts.keySet());
        Collections.sort(words, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return -wordCounts.get(o1) + wordCounts.get(o2);
            }
        });

        i = 1;
        for (String word : words) {
            if (i > 10000) {
                break;
            }
            System.out.println(i++ + ": " + word + " : " + wordCounts.get(word));
        }*/
    }

    private static Set<String> loadDictionary() throws Exception {
        Set<String> dict = new HashSet<String>();
        FileInputStream is = new FileInputStream("test_files/dict.txt");
        Scanner scanner = new Scanner(is);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            dict.add(line.split(":")[1].trim());
            if (dict.size() >= 5000) {
                break;
            }
        }
        return dict;
    }
}
