package com.mongodb.starter.database.versioning.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class QueryModel {
    private String collectionName;
    private String collectionOperation;
    private List<Param> params = new ArrayList<>();
    private boolean isValidQuery = true;
    private boolean global = false;

    public QueryModel(boolean isValidQuery) {
        this.isValidQuery = isValidQuery;
    }

    public QueryModel(String collectionName, String collectionOperation) {
        this.collectionName = collectionName;
        this.collectionOperation = collectionOperation;
    }

    public void addParam(Param param) {
        this.params.add(param);
    }
}
