package org.zayed.unirollweb.common;

// 409: the request is valid but clashes with current data (duplicate code, subject full, ...)
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
