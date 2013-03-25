package yatan.deeplearning.softmax;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.omg.CosNaming.NamingContextPackage.AlreadyBound;

import yatan.commons.matrix.Matrix;
import yatan.deeplearning.softmax.contract.WordEmbeddingAnnParameterActorContractImpl.PersistableState;
import yatan.deeplearning.wordembedding.data.Dictionary;
import yatan.deeplearning.wordembedding.model.WordEmbedding;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;

public class WordEmbeddingModifier {
    private static final String MODEL_FOLDER = "test_files/results/";

    private static PersistableState state;

    public static void main(String[] args) {
        loadState();

        System.out.println(state.wordEmbedding.getMatrix().rowSize() + ", "
                + state.wordEmbedding.getMatrix().columnSize());

        if (state.wordEmbedding.getDictionary().contains(Dictionary.NUMBER_WORD)) {
            System.out.println("Already has NUMBER_WORD");
            return;
        }

        state.wordEmbedding.getDictionary().add(Dictionary.NUMBER_WORD);
        state.wordEmbedding.getDictionary().add(Dictionary.EN_WORD);

        WordEmbedding newWordEmbedding =
                new WordEmbedding(state.wordEmbedding.getDictionary(), state.wordEmbedding.getWordVectorSize());
        Matrix matrix = newWordEmbedding.getMatrix();
        for (int i = 0; i < matrix.getData().length; i++) {
            for (int j = 0; j < state.wordEmbedding.getMatrix().getData()[i].length; j++) {
                matrix.getData()[i][j] = state.wordEmbedding.getMatrix().getData()[i][j];
            }
        }

        state.wordEmbedding = newWordEmbedding;

        saveState();
    }

    private static void saveState() {
        File stateFile = new File(MODEL_FOLDER + "modified_" + (new Date().getTime()) + ".json");
        System.out.println("Saving parameter server state to " + stateFile + "...");
        FileWriterWithEncoding writer = null;
        try {
            writer = new FileWriterWithEncoding(stateFile, Charsets.UTF_8);
            String json = new Gson().toJson(state);
            writer.write(json);
        } catch (IOException e) {
            System.out.println("Error occurred while trying to save parameter server state: " + e.getMessage());
            e.printStackTrace();
        } finally {
            close(writer);
        }
    }

    private static boolean loadState() {
        System.out.println("Trying to find persisted parameter server state...");
        File stateFile = null;
        for (File file : new File(MODEL_FOLDER).listFiles()) {
            if (file.isFile() && Files.getFileExtension(file.getName()).equals("json")
                    && (stateFile == null || stateFile.getName().compareTo(file.getName()) < 0)) {
                stateFile = file;
            }
        }

        if (stateFile != null) {
            System.out.println("Loading parameter server state from " + stateFile + "...");
            FileInputStream is = null;
            InputStreamReader reader = null;
            try {
                is = new FileInputStream(stateFile);
                reader = new InputStreamReader(is, Charsets.UTF_8);
                state = new Gson().fromJson(reader, PersistableState.class);
            } catch (IOException e) {
                System.out.println("Error occurred while trying to load parameter server state: " + e.getMessage());
                e.printStackTrace();
                return false;
            } finally {
                close(reader, is);
            }

            return true;
        } else {
            System.out.println("Can't find any persisted parameter sever state. Let's start from strach.");
            return false;
        }
    }

    private static void close(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            try {
                if (closeable != null) {
                    closeable.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
