package yatan.distributed.ml.ann.data;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.collect.Lists;

import mnist.tools.MnistManager;

import yatan.ann.AnnData;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.contract.data.impl.DataProducer;
import yatan.distributedcomputer.contract.data.impl.DataProducerException;

public class MNISTHandWrittenDataProducer implements DataProducer {
    private static final Logger LOGGER = Logger.getLogger(MNISTHandWrittenDataProducer.class);
    private MnistManager mnistManager;
    private int batch;

    public MNISTHandWrittenDataProducer(String imageFile, String labelFile) throws IOException {
        this.mnistManager = new MnistManager(imageFile, labelFile);
    }

    @Override
    public List<Data> produceData(int size) throws DataProducerException {
        List<Data> datas = Lists.newArrayList();
        while (datas.size() < size) {
            try {
                int[][] image = mnistManager.readImage();
                int label = mnistManager.readLabel();

                double[] input = new double[image.length * image[0].length];
                for (int i = 0; i < image.length; i++) {
                    for (int j = 0; j < image[i].length; j++) {
                        input[image[0].length * i + j] = image[i][j] / 255.0;
                    }
                }

                double[] output = new double[10];
                output[label] = 1;

                datas.add(new Data(new AnnData(input, output)));
            } catch (EOFException e) {
                mnistManager.setCurrent(0);
                batch++;
                LOGGER.debug("Start batch " + batch + "...");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return datas;
    }
}
