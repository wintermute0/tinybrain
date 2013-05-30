package yatan.deeplearning.softmax.contract.parameter;

import java.io.Serializable;
import java.util.Map;

public interface StatefulProcessor {
    public void save(Map<String, Serializable> state);

    public void load(Map<String, Serializable> state);
}
