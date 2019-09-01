package com.mongodb.starter.database.versioning;

class Defines {

    final static String SUB_VERSION_DELIMITER = "_";
    final static String VERSION_REGEX = "(?<=^|)\\d+(\\.\\d+)?(?=$|)";
    final static String GET_COLLECTION = "getCollection";

    enum CollectionOperations {
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

    enum CollationField {
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

    enum UpdateOptionsField {
        upsert,
        collation,
        arrayFilters,
        multi
    }
}
