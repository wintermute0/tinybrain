package yatan.data.sequence;

import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class TaggedSentenceDataset {
    private final Set<String> tags = Sets.newHashSet();
    private final Set<String> words = Sets.newHashSet();
    private final Set<String> characters = Sets.newHashSet();
    private final List<TaggedSentence> sentences = Lists.newArrayList();

    public List<String> getTags() {
        return ImmutableList.copyOf(tags);
    }

    public Set<String> getWords() {
        return ImmutableSet.copyOf(words);
    }

    public Set<String> getCharacters() {
        return ImmutableSet.copyOf(characters);
    }

    public List<TaggedSentence> getSentences() {
        return ImmutableList.copyOf(sentences);
    }

    public void addSentences(TaggedSentence sentence) {
        Preconditions.checkArgument(sentence != null);

        this.sentences.add(sentence);
        this.tags.addAll(sentence.tags());
        this.words.addAll(sentence.words());
        for (String word : sentence.words()) {
            for (int i = 0; i < word.length(); i++) {
                this.characters.add(String.valueOf(word.charAt(i)));
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (TaggedSentence sentence : this.sentences) {
            sb.append(sentence).append("\n");
        }
        return sb.toString();
    }
}
