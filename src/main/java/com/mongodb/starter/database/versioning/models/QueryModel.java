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

    public QueryModel(boolean isValidQuery) {
        this.isValidQuery = isValidQuery;
    }

    public QueryModel(String collectionName, String collectionOperation) {
        this.collectionName = collectionName;
        this.collectionOperation = collectionOperation;
        this.useOptions = false;
    }

    public QueryModel(String collectionName, String collectionOperation, Class<?> query) {
        this.collectionName = collectionName;
        this.collectionOperation = collectionOperation;
        this.query = query;
        this.useOptions = false;
    }

    public QueryModel(String collectionName, String collectionOperation, Class<?> query, Class<?> options) {
        this.collectionName = collectionName;
        this.collectionOperation = collectionOperation;
        this.query = query;
        this.options = options;
        this.useOptions = true;
    }

    public void setOptions(Object options) {
        this.options = options;
        this.useOptions = true;
    }
}
