package org.zayed.unirollweb.common;

// 403: logged in, but not allowed to touch this resource (e.g. another lecturer's subject)
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
