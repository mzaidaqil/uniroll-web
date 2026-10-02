package org.zayed.unirollweb.auth;

// One message for both "no such email" and "wrong password", so attackers can't learn which emails exist
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
