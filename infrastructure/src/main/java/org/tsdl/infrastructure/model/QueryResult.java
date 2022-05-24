package org.tsdl.infrastructure.model;

import org.tsdl.infrastructure.model.impl.TsdlQueryResult;

import java.util.List;

public interface QueryResult {

    List<DataPoint> getItems();

    static QueryResult of(List<DataPoint> items) {
        return new TsdlQueryResult(items);
    }
}