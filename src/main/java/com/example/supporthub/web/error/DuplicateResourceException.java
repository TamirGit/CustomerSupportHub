package com.example.supporthub.web.error;

/** Thrown when creating a resource that conflicts with an existing one (e.g. taken username). Mapped to HTTP 409. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
