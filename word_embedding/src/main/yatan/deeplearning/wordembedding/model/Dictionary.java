package yatan.deeplearning.wordembedding.model;

import java.io.File;

import java.io.IOException;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

public final class Dictionary {
    public static final String PADDING_WORD = "PEDINGWORDINTHEBEGINNINGANDENDDINGOFASETENCE";
    public static final String NO_SUCH_WORD_PLACE_HOLDER = "NOSUCHWORDINTHISDICTIONARY";

    public static final String NUMBER_WORD = "$NUM$";
    public static final String EN_WORD = "$EN$";

    private final List<String> words = Lists.newArrayList();
    private final Map<String, Integer> wordIndecies = Maps.newHashMap();
    private final Map<String, Double> wordLikelyhood = Maps.newHashMap();
    private final List<String> sortedWords = Lists.newArrayList();
    private final Random random = new Random(new Date().getTime());

    private Dictionary(Collection<String> words, Map<String, Double> wordLikelyhood) {
        this.words.addAll(words);
        this.words.add(NO_SUCH_WORD_PLACE_HOLDER);
        this.words.add(PADDING_WORD);
        if (!this.words.contains(NUMBER_WORD)) {
            this.words.add(NUMBER_WORD);
        }
        if (!this.words.contains(EN_WORD)) {
            this.words.add(EN_WORD);
        }

        for (int i = 0; i < this.words.size(); i++) {
            wordIndecies.put(this.words.get(i), i);
        }

        this.wordLikelyhood.putAll(wordLikelyhood);

        this.sortedWords.addAll(this.words);
        Collections.sort(this.sortedWords, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                double freq1 =
                        Dictionary.this.wordLikelyhood.containsKey(o1) ? Dictionary.this.wordLikelyhood.get(o1)
                                : Double.MIN_VALUE;
                double freq2 =
                        Dictionary.this.wordLikelyhood.containsKey(o2) ? Dictionary.this.wordLikelyhood.get(o2)
                                : Double.MIN_VALUE;
                return freq1 - freq2 > 0 ? -1 : 1;
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static Dictionary create(File file, int limit) {
        Object[] objects = loadDictionary(file);
        List<String> words = (List<String>) objects[0];
        final Map<String, Double> wordLikelyhood = (Map<String, Double>) objects[1];

        if (words.size() > limit) {
            Collections.sort(words, new Comparator<String>() {
                @Override
                public int compare(String arg0, String arg1) {
                    return wordLikelyhood.get(arg0) - wordLikelyhood.get(arg1) > 0 ? -1 : 1;
                }
            });

            words = words.subList(0, limit);
            for (String word : Lists.newArrayList(wordLikelyhood.keySet())) {
                if (!words.contains(word)) {
                    wordLikelyhood.remove(word);
                }
            }
        }

        return create(words, wordLikelyhood);
    }

    public static Dictionary create(File file) {
        return create(file, Integer.MAX_VALUE);
    }

    public static Dictionary create(Collection<String> words, Map<String, Double> wordLikelyhood) {
        return new Dictionary(words, wordLikelyhood);
    }

    /*
     * public static Dictionary create() { return new Dictionary(loadDictionary(new File("zh_dict.txt"))); }
     */

    public int indexOf(String word) {
        Integer index = wordIndecies.get(word);
        if (index == null) {
            return wordIndecies.get(NO_SUCH_WORD_PLACE_HOLDER);
        }

        return index;
    }

    public int frenquencyRank(int wordIndex) {
        // int result = 0;
        //
        // if (!this.wordLikelyhood.containsKey(this.words.get(wordIndex))) {
        // return Integer.MAX_VALUE;
        // }
        //
        // double frequency = this.wordLikelyhood.get(this.words.get(wordIndex));
        // for (Entry<String, Double> entry : wordLikelyhood.entrySet()) {
        // if (entry.getValue() > frequency) {
        // result++;
        // }
        // }
        //
        // return result;

        return this.sortedWords.indexOf(this.words.get(wordIndex));
    }

    public List<String> words() {
        return Collections.unmodifiableList(words);
    }

    public int size() {
        return words.size();
    }

    public int sampleWord() {
        double ratio = this.random.nextDouble();
        Map<String, Double> wordLikelyhood = this.wordLikelyhood;
        double accumulatedRatio = 0;
        String lastWord = null;
        for (Entry<String, Double> entry : wordLikelyhood.entrySet()) {
            lastWord = entry.getKey();
            if (accumulatedRatio > ratio) {
                return this.wordIndecies.get(entry.getKey());
            }

            accumulatedRatio += entry.getValue();
        }

        return this.wordIndecies.get(lastWord);
    }

    public int sampleWordUniformlyAboveFrequenceRank(int rankBound) {
        if (rankBound > 0) {
            return this.words.indexOf(this.sortedWords.get(this.random.nextInt(rankBound)));
        } else {
            return this.random.nextInt(this.words.size());
        }
    }

    private static Object[] loadDictionary(File file) {
        List<String> words = Lists.newArrayList();
        Map<String, Double> wordLikelyhood = Maps.newHashMap();
        try {
            for (String line : Files.readLines(file, Charsets.UTF_8)) {
                List<String> token =
                        Lists.newArrayList(Splitter.onPattern(":|\\,").trimResults().omitEmptyStrings().split(line));
                if (token.size() != 2) {
                    System.out.println("Invalid line " + line);
                    continue;
                }

                words.add(token.get(0));
                wordLikelyhood.put(token.get(0), Double.parseDouble(token.get(1)));
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return new Object[] {words, wordLikelyhood};
    }
}
