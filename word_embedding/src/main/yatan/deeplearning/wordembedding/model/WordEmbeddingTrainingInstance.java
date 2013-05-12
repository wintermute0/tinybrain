package yatan.deeplearning.wordembedding.model;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public class WordEmbeddingTrainingInstance implements Serializable {
    private int output;
    private List<Integer> input;

    public List<Integer> getInput() {
        return input;
    }

    public void setInput(List<Integer> input) {
        this.input = input;
    }

    public int getOutput() {
        return output;
    }

    public void setOutput(int output) {
        this.output = output;
    }
}
