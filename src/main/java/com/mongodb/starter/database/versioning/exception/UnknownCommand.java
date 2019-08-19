package com.mongodb.starter.database.versioning.exception;

public class UnknownCommand extends Exception {

    public UnknownCommand() { super(); }

    public UnknownCommand(String message) { super(message); }

    public UnknownCommand(String message, Throwable throwable) { super(message, throwable); }
}
