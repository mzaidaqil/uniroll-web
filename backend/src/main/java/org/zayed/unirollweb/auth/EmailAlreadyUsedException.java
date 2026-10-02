package org.zayed.unirollweb.auth;

import org.zayed.unirollweb.common.ConflictException;

public class EmailAlreadyUsedException extends ConflictException {

    public EmailAlreadyUsedException(String email) {
        super("Email is already registered: " + email);
    }
}
