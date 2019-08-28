package com.mongodb.starter.database.versioning.exception;

public class UnknownCollectionOperationException extends Exception {

    public UnknownCollectionOperationException() { super(); }

    public UnknownCollectionOperationException(String message) { super(message); }

    public UnknownCollectionOperationException(String message, Throwable throwable) { super(message, throwable); }
}
