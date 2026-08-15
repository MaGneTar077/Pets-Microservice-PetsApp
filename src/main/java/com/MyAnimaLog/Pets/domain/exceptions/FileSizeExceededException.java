package com.MyAnimaLog.Pets.domain.exceptions;

public class FileSizeExceededException extends RuntimeException {
    public FileSizeExceededException(String message) {
        super(message);
    }
}
