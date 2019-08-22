package com.mongodb.starter.database.versioning.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class QueryModel {
    private String collectionName;
    private String collectionOperation;
    private Object query;
    private Object options;
    private boolean isValidQuery = true;
    private boolean useOptions = false;
    private boolean useQuery = false;

    public QueryModel(boolean isValidQuery) {
        this.isValidQuery = isValidQuery;
    }

    public QueryModel(String collectionName, String collectionOperation) {
        this.collectionName = collectionName;
        this.collectionOperation = collectionOperation;
    }

    public void setOptions(Object options) {
        this.options = options;
        this.useOptions = true;
    }

    public void setQuery(Object query) {
        this.query = query;
        this.useQuery = true;
    }
}
