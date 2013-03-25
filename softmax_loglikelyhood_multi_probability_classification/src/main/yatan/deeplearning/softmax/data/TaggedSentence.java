package yatan.deeplearning.softmax.data;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class TaggedSentence {
    private final List<String> words = Lists.newArrayList();
    private final List<String> tags = Lists.newArrayList();

    public void addWord(String word, String tag) {
        Preconditions.checkArgument(word != null);
        Preconditions.checkArgument(tag != null);

        this.words.add(word);
        this.tags.add(tag);
    }

    public List<String> words() {
        return ImmutableList.copyOf(this.words);
    }

    public List<String> tags() {
        return ImmutableList.copyOf(this.tags);
    }

    public int length() {
        return this.words.size();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.words.size(); i++) {
            sb.append(this.words.get(i) + "/" + this.tags.get(i) + "  ");
        }
        return sb.toString();
    }
}
