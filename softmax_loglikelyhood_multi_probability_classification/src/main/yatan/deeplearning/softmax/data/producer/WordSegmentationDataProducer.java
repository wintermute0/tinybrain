package yatan.deeplearning.softmax.data.producer;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import yatan.deeplearning.softmax.data.TaggedSentence;
import yatan.deeplearning.softmax.data.TaggedSentenceDataset;
import yatan.deeplearning.wordembedding.data.Dictionary;
import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.contract.data.impl.DataProducer;
import yatan.distributedcomputer.contract.data.impl.DataProducerException;

public class WordSegmentationDataProducer implements DataProducer {
    public static final int WINDOWS_SIZE = 11;

    private static final Logger LOGGER = Logger.getLogger(WordSegmentationDataProducer.class);
    private static final Random RANDOM = new Random(new Date().getTime());

    private final WordSegmentationInstancePool instancePool;
    private final boolean training;

    private int offset;
    private int epoch = 1;

    @Inject
    public WordSegmentationDataProducer(@Named("training") boolean training, WordSegmentationInstancePool instancePool) {
        Preconditions.checkArgument(instancePool != null);
        LOGGER.info("New " + (training ? "training" : "evaluating") + " instance producer.");

        this.training = training;
        this.instancePool = instancePool;
    }

    @Override
    public List<Data> produceData(int size) throws DataProducerException {
        List<WordEmbeddingTrainingInstance> instances = this.instancePool.getInstances();
        if (this.training) {
            LOGGER.info("Epoch: " + this.epoch + ", " + 100.0 * this.offset / instances.size() + "%");
        } else {
            LOGGER.info("Producing evaluating data...");
        }

        List<Data> datas = Lists.newArrayList();
        for (int i = 0; i < size; i++) {
            Data data = new Data();
            int index;
            if (this.training) {
                index = this.offset++;
                if (this.offset >= instances.size()) {
                    this.offset = 0;
                    this.epoch++;
                }
            } else {
                index = RANDOM.nextInt(instances.size());

            }
            data.setSerializable(instances.get(index));
            datas.add(data);
        }

        return datas;
    }

    // private void debug(List<String> characters, List<String> tags) {
    // for (int i = 0; i < characters.size(); i++) {
    // System.out.print(characters.get(i) + "(" + tags.get(i) + ") ");
    // }
    // System.out.println();
    // }

    public static class WordSegmentationInstancePool {
        public static final List<String> TAGS = Lists.newArrayList("U", "B", "L", "I");

        private final Dictionary dictionary;
        private final boolean shuffle;
        private final TaggedSentenceDataset taggedSentenceDataset;

        private List<WordEmbeddingTrainingInstance> instances;

        @Inject
        public WordSegmentationInstancePool(Dictionary dictionary,
                @Named("tagged_sentence_dataset") TaggedSentenceDataset taggedSentenceDataset) {
            this(dictionary, taggedSentenceDataset, true);
        }

        public WordSegmentationInstancePool(Dictionary dictionary, TaggedSentenceDataset taggedSentenceDataset,
                boolean shuffle) {
            Preconditions.checkArgument(dictionary != null, "The parameter 'dictionary' cannot be null.");
            LOGGER.info("New instance pool with dictionary size " + dictionary.size()
                    + ", tagged sentence dataset size " + taggedSentenceDataset.getSentences().size() + ", shuffle is "
                    + shuffle);

            this.dictionary = dictionary;
            this.taggedSentenceDataset = taggedSentenceDataset;
            this.shuffle = shuffle;
        }

        public List<WordEmbeddingTrainingInstance> getInstances() throws DataProducerException {
            loadDataIfNecessary();

            return ImmutableList.copyOf(instances);
        }

        private static List<String> breakWord(String word, List<String> originalWordContainer) {
            List<String> tokens = Lists.newArrayList();
            boolean lastCharacterIsNumber = false;
            boolean lastCharacterIsLetter = false;

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < word.length(); i++) {
                char ch = word.charAt(i);
                boolean isEnLetter = (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
                if (Character.isDigit(ch) || (lastCharacterIsNumber && ch == '.')) {
                    lastCharacterIsNumber = true;
                    sb.append(ch);
                } else if (isEnLetter) {
                    lastCharacterIsLetter = true;
                    sb.append(ch);
                } else {
                    if (lastCharacterIsNumber) {
                        tokens.add(Dictionary.NUMBER_WORD);
                        lastCharacterIsNumber = false;

                        if (originalWordContainer != null) {
                            originalWordContainer.add(sb.toString());
                            sb = new StringBuilder();
                        }
                    } else if (lastCharacterIsLetter) {
                        tokens.add(Dictionary.EN_WORD);
                        lastCharacterIsLetter = false;

                        if (originalWordContainer != null) {
                            originalWordContainer.add(sb.toString());
                            sb = new StringBuilder();
                        }
                    }

                    tokens.add(String.valueOf(ch));
                    if (originalWordContainer != null) {
                        originalWordContainer.add(String.valueOf(ch));
                    }
                }
            }

            // FIXME: if this is a single number or a single letter, is not gonna be dealt with
            if (lastCharacterIsNumber) {
                tokens.add(Dictionary.NUMBER_WORD);
                lastCharacterIsNumber = false;

                if (originalWordContainer != null) {
                    originalWordContainer.add(sb.toString());
                }
            } else if (lastCharacterIsLetter) {
                tokens.add(Dictionary.EN_WORD);
                lastCharacterIsLetter = false;

                if (originalWordContainer != null) {
                    originalWordContainer.add(sb.toString());
                }
            }

            return tokens;
        }

        public static List<WordEmbeddingTrainingInstance> convertTaggedSentenceToWordEmbeddingTrainingInstance(
                Dictionary dictionary, TaggedSentence sentence, List<String> originalWordContainer) {
            List<WordEmbeddingTrainingInstance> results = Lists.newArrayList();

            // covert words sentence to character sentence
            List<String> characters = Lists.newArrayList();
            List<String> tags = Lists.newArrayList();
            for (String word : sentence.words()) {
                List<String> tokens = breakWord(word, originalWordContainer);
                for (int i = 0; i < tokens.size(); i++) {
                    characters.add(tokens.get(i));
                    if (tokens.size() == 1) {
                        tags.add("U");
                    } else if (i == 0) {
                        tags.add("B");
                    } else if (i == tokens.size() - 1) {
                        tags.add("L");
                    } else {
                        tags.add("I");
                    }
                }
            }
            // debug(characters, tags);

            // put padding words before and after the sentence
            for (int i = 0; i < WINDOWS_SIZE / 2; i++) {
                characters.add(0, Dictionary.PADDING_WORD);
                characters.add(Dictionary.PADDING_WORD);
            }

            List<Integer> characterIndecies = Lists.newArrayList();
            for (String character : characters) {
                characterIndecies.add(dictionary.indexOf(character));
            }

            for (int i = WINDOWS_SIZE / 2; i < tags.size() + WINDOWS_SIZE / 2; i++) {
                WordEmbeddingTrainingInstance instance = new WordEmbeddingTrainingInstance();
                instance.setInput(new ArrayList<Integer>());
                for (int j = i - WINDOWS_SIZE / 2; j < i + WINDOWS_SIZE / 2 + 1; j++) {
                    instance.getInput().add(characterIndecies.get(j));
                }
                instance.setOutput(TAGS.indexOf(tags.get(i - WINDOWS_SIZE / 2)));
                results.add(instance);
            }

            return results;
        }

        private void loadDataIfNecessary() throws DataProducerException {
            if (instances == null) {
                instances = Lists.newArrayList();

                LOGGER.info("Coverting data to training instance...");
                for (TaggedSentence sentence : this.taggedSentenceDataset.getSentences()) {
                    instances.addAll(convertTaggedSentenceToWordEmbeddingTrainingInstance(this.dictionary, sentence,
                            null));
                }

                if (this.shuffle) {
                    LOGGER.info("Shuffling instances...");
                    Collections.shuffle(instances);
                }

                LOGGER.info("Total instances: " + instances.size());
            }
        }
    }
}
