package com.mongodb.starter.database.versioning;

public class Defines {

    public final static String SUB_VERSION_DELIMITER = "_";

    public final static String DB_GET_COLLECTION = "getCollection";

    public enum CollectionOperations {
        createIndex,
        createIndexes,
        deleteOne,
        deleteMany,
        drop,
        dropIndex,
        dropIndexes,
        insert,
        insertOne,
        insertMany,
        remove,
        renameCollection,
        save,
        update,
        updateOne,
        updateMany
    }

    public enum CollationField {
        locale,
        caseLevel,
        caseFirst,
        strength,
        numericOrdering,
        alternate,
        maxVariable,
        normalization,
        backwards
    }
}
