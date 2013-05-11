package yatan.distributed.ml.ann.data;

import java.io.IOException;

public class MNISTHandWrittenTrainingDataProducer extends MNISTHandWrittenDataProducer {
    public MNISTHandWrittenTrainingDataProducer() throws IOException {
        super("test_files/mnist/train-images.idx3-ubyte", "test_files/mnist/train-labels.idx1-ubyte");
    }
}
