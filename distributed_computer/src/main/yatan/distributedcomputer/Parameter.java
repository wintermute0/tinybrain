package yatan.distributedcomputer;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public class Parameter implements Serializable {
    private ParameterIndexPath startPosition;
    private ParameterIndexPath endPosition;

    private Serializable serializable;

    /**
     * Just like NSIndexPath.
     * @author eveningsun
     */
    public static class ParameterIndexPath {
        private List<Integer> indecis;
    }

    public Serializable getSerializable() {
        return serializable;
    }

    public void setSerializable(Serializable serializable) {
        this.serializable = serializable;
    }
}
