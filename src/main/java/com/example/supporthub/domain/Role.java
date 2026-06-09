package com.example.supporthub.domain;

/**
 * System roles. ADMIN can do anything; AGENT owns several CUSTOMERs; CUSTOMER opens tickets.
 */
public enum Role {
    ADMIN,
    AGENT,
    CUSTOMER
}
