package yatan.ann;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import yatan.ann.AnnConfiguration.ActivationFunction;


public class Demo {
    public static void main(String[] args) {
        List<AnnData> dataset = loadData();

        List<AnnData> training = dataset.subList(0, (int) (dataset.size() * 0.9));
        List<AnnData> test = dataset.subList((int) (dataset.size() * 0.9), dataset.size());

        AnnConfiguration configuration = new AnnConfiguration(16);
        configuration.addLayer(10, ActivationFunction.SIGMOID);
        configuration.addLayer(1, ActivationFunction.SIGMOID);
        DefaultAnnModel model = new DefaultAnnModel(configuration);

        AnnTrainer trainer = new AnnTrainer();

        List<AnnData> batch = new ArrayList<AnnData>();
        while (true) {
            for (AnnData data : training) {
                batch.add(data);
                if (batch.size() == 20) {
                    trainer.trainWithMiniBatch(model, batch);
                    batch.clear();
                }
            }
            System.out.println(evaluate(model, test));
        }
    }

    private static List<AnnData> loadData() {
        List<AnnData> dataset = new ArrayList<AnnData>();
        Scanner scanner = new Scanner(Demo.class.getResourceAsStream("/letter-recognition.data"));
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

    private static double evaluate(DefaultAnnModel model, List<AnnData> testSet) {
        AnnTrainer trainer = new AnnTrainer();
        int accurate = 0;
        for (AnnData data : testSet) {
            double[][] output = trainer.run(model, data.getInput(), new double[model.getLayerCount()][]);
            if (Math.round(output[model.getLayerCount() - 1][0]) == data.getOutput()[0]) {
                accurate++;
            }
        }

        return 1.0 * accurate / testSet.size();
    }
}
