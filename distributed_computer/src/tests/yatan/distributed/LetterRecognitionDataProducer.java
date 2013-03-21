package yatan.distributed;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import yatan.ann.AnnData;
import yatan.distributedcomputer.Data;
import yatan.distributedcomputer.contract.data.impl.DataProducer;
import yatan.distributedcomputer.contract.data.impl.DataProducerException;

public class LetterRecognitionDataProducer implements DataProducer {
    private List<AnnData> dataset;

    private static List<AnnData> loadData() {
        List<AnnData> dataset = new ArrayList<AnnData>();
        Scanner scanner =
                new Scanner(LetterRecognitionDataProducer.class.getResourceAsStream("/letter-recognition.data"));
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] tokens = line.split(",");
            if (tokens.length < 2) {
                continue;
            }

            double[] input = new double[tokens.length - 1];
            for (int i = 1; i < tokens.length; i++) {
                input[i - 1] = Double.parseDouble(tokens[i]);
            }

            dataset.add(new AnnData(input, new double[] {(tokens[0].equals("A") || tokens[0].equals("O")
                    || tokens[0].equals("I") || tokens[0].equals("U") || tokens[0].equals("O")) ? 1 : 0}));
        }

        return dataset;
    }

    @Override
    public List<Data> produceData(int size) throws DataProducerException {
        if (this.dataset == null) {
            this.dataset = loadData();
        }

        List<Data> data = new ArrayList<Data>();
        Random random = new Random(new Date().getTime());
        for (int i = 0; i < size; i++) {
            Data d = new Data();
            int index;
            index = random.nextInt((int) (this.dataset.size() * 0.9));
            // case TEST:
            // index = (int) (this.dataset.size() * 0.9 + random.nextInt((int) (this.dataset.size() * 0.1)));

            d.setSerializable(this.dataset.get(index));
            data.add(d);
        }

        return data;
    }
}
