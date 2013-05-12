package yatan.deeplearning;

import java.io.File;

import org.junit.Test;

import yatan.deeplearning.wordembedding.data.ZhWikiTrainingDataProducer;
import yatan.deeplearning.wordembedding.model.Dictionary;

public class TestBed {
    @Test
    public void test1() throws Exception {
        new ZhWikiTrainingDataProducer(Dictionary.create(new File("test_files/zh_dict.txt"))).produceData(1000);
    }
}
