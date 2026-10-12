package com.parkingSystem.exception;

/**
 * A webhook whose signature or timestamp does not check out. Returned as 401.
 */
public class InvalidSignatureException extends RuntimeException {

    public InvalidSignatureException(String message) {
        super(message);
    }
}
