package com.mongodb.starter.database.versioning.exception;

public class UnknownCommandException extends Exception {

    public UnknownCommandException() { super(); }

    public UnknownCommandException(String message) { super(message); }

    public UnknownCommandException(String message, Throwable throwable) { super(message, throwable); }
}
