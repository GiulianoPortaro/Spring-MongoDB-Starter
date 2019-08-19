package com.mongodb.starter.database.versioning.exception;

public class UnknownCollectionOperation extends Exception {

    public UnknownCollectionOperation() { super(); }

    public UnknownCollectionOperation(String message) { super(message); }

    public UnknownCollectionOperation(String message, Throwable throwable) { super(message, throwable); }
}
