package yatan.data.sequence;

import yatan.deeplearning.wordembedding.model.WordEmbeddingTrainingInstance;

@SuppressWarnings("serial")
public class TaggedTrainingInstance extends WordEmbeddingTrainingInstance {
    private int lastTag;

    public int getLastTag() {
        return lastTag;
    }

    public void setLastTag(int lastTag) {
        this.lastTag = lastTag;
    }
}
