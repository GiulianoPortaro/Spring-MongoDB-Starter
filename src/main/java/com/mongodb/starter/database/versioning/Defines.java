package com.mongodb.starter.database.versioning;

public class Defines {

    public final static String SUB_VERSION_DELIMITER = "_";
    public final static String VERSION_REGEX = "(?<=^|)\\d+(\\.\\d+)?(?=$|)";

    public enum CollectionOperations {
        createCollection,
        createIndex,
        createIndexes,
        deleteOne,
        deleteMany,
        drop,
        dropIndex,
        dropIndexes,
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

    public enum UpdateOptionsField {
        upsert,
        collation,
        arrayFilters,
        multi
    }
}
