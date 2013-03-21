package yatan.distributedcomputer.contract.data.impl;

import java.util.List;

import yatan.distributedcomputer.Data;

public interface DataProducer {
    public List<Data> produceData(int size) throws DataProducerException;
}
