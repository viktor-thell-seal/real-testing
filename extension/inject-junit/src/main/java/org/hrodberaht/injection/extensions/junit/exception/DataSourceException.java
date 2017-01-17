package org.hrodberaht.injection.extensions.junit.exception;

public class DataSourceException extends RuntimeException {

    public DataSourceException(Throwable cause) {
        super(cause);
    }

    public DataSourceException(String message) {
        super(message);
    }

    public DataSourceException(String message, Throwable e) {
        super(message, e);
    }
}