package com.vanai.backend.contact;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailValidatorTest {

    private final EmailValidator validator = new EmailValidator();

    @Test
    void validEmails() {
        assertTrue(validator.isValid("user@example.com"));
        assertTrue(validator.isValid("admin@company.co.kr"));
        assertTrue(validator.isValid("contact.team@example.org"));
    }

    @Test
    void invalidEmails() {
        assertFalse(validator.isValid("abc"));
        assertFalse(validator.isValid("example@"));
        assertFalse(validator.isValid("@example.com"));
        assertFalse(validator.isValid("test@@example.com"));
        assertFalse(validator.isValid(""));
        assertFalse(validator.isValid(null));
    }
}
