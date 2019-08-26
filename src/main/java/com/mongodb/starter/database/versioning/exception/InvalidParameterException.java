package com.mongodb.starter.database.versioning.exception;

public class InvalidParameterException extends Exception {

    public InvalidParameterException() { super(); }

    public InvalidParameterException(String message) { super(message); }

    public InvalidParameterException(String message, Throwable throwable) { super(message, throwable); }
}
