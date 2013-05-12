package yatan.data.parser.bakeoff2005;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import yatan.data.sequence.TaggedSentence;
import yatan.data.sequence.TaggedSentenceDataset;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;

public class ICWB2Parser {
    public TaggedSentenceDataset parse(File file) throws FileNotFoundException {
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            return parse(is);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public TaggedSentenceDataset parse(InputStream is) {
        TaggedSentenceDataset dataset = new TaggedSentenceDataset();

        Scanner scanner = new Scanner(is, Charsets.UTF_8.name());
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.trim().isEmpty()) {
                continue;
            }

            TaggedSentence sentence = new TaggedSentence();
            for (String word : Splitter.on("  ").trimResults().omitEmptyStrings().split(line)) {
                sentence.addWord(word, "no-tag");
            }
            dataset.addSentences(sentence);
        }

        return dataset;
    }
}
