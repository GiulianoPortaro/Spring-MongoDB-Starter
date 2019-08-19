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

    public final static String COL_CREATE_INDEX = "createIndex";
    public final static String COL_CREATE_INDEXES = "createIndexes";
    public final static String COL_DELETE_ONE = "deleteOne";
    public final static String COL_DELETE_MANY = "deleteMany";
    public final static String COL_DROP = "drop";
    public final static String COL_DROP_INDEX = "dropIndex";
    public final static String COL_DROP_INDEXES = "dropIndexes";
    public final static String COL_INSERT = "insert";
    public final static String COL_INSERT_ONE = "insertOne";
    public final static String COL_INSERT_MANY = "insertMany";
    public final static String COL_REMOVE = "remove";
    public final static String COL_RENAME_COLLECTION = "renameCollection";
    public final static String COL_SAVE = "save";
    public final static String COL_UPDATE = "update";
    public final static String COL_UPDATE_ONE = "updateOne";
    public final static String COL_UPDATE_MANY = "updateMany";
}
