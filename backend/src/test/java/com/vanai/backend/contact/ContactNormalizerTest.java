package com.vanai.backend.contact;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContactNormalizerTest {

    private final ContactNormalizer normalizer =
            new ContactNormalizer();

    @Test
    void normalizeEmail() {
        assertEquals(
                "contact@example.com",
                normalizer.normalizeEmail(" Contact@Example.com ")
        );

        assertEquals(
                "hello@example.com",
                normalizer.normalizeEmail("mailto:HELLO@example.com")
        );
    }

    @Test
    void normalizePhone() {
        assertEquals(
                "0511234567",
                normalizer.normalizePhone("051-123-4567")
        );

        assertEquals(
                "0511234567",
                normalizer.normalizePhone("051 123 4567")
        );

        assertEquals(
                "0511234567",
                normalizer.normalizePhone("+82 51-123-4567")
        );
    }
}
