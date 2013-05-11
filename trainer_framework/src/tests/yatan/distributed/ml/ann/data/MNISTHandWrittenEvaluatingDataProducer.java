package yatan.distributed.ml.ann.data;

import java.io.IOException;

public class MNISTHandWrittenEvaluatingDataProducer extends MNISTHandWrittenDataProducer {
    public MNISTHandWrittenEvaluatingDataProducer() throws IOException {
        super("test_files/mnist/t10k-images.idx3-ubyte", "test_files/mnist/t10k-labels.idx1-ubyte");
    }
}
