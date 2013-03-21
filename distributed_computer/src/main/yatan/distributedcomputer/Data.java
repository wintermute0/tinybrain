package yatan.distributedcomputer;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Data implements Serializable {
    private Serializable serializable;

    public Data() {
    }

    public Data(Serializable serializable) {
        this.serializable = serializable;
    }

    public Serializable getSerializable() {
        return serializable;
    }

    public void setSerializable(Serializable serializable) {
        this.serializable = serializable;
    }
}
