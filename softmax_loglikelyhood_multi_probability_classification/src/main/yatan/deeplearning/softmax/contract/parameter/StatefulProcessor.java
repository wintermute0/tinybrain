package yatan.deeplearning.softmax.contract.parameter;

import java.io.Serializable;

import java.util.Map;

import com.google.gson.JsonObject;

public interface StatefulProcessor {
    public void save(Map<String, Serializable> state);

    public void load(JsonObject jsonElement);
}
