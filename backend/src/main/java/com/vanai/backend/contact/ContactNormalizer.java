package com.vanai.backend.contact;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ContactNormalizer {

    public String normalizeEmail(String email) {

        if (email == null) {
            return null;
        }

        String normalized = email.trim();

        if (normalized.toLowerCase(Locale.ROOT).startsWith("mailto:")) {
            normalized = normalized.substring(7);
        }

        normalized = normalized.trim()
                .toLowerCase(Locale.ROOT);

        while (normalized.endsWith(".")
                || normalized.endsWith(",")
                || normalized.endsWith(";")) {
            normalized = normalized.substring(
                    0,
                    normalized.length() - 1
            );
        }

        return normalized.isBlank() ? null : normalized;
    }

    public String normalizePhone(String phone) {

        if (phone == null || phone.isBlank()) {
            return null;
        }

        String normalized =
                phone.replaceAll("[^0-9+]", "");

        if (normalized.startsWith("+82")) {
            normalized = "0" + normalized.substring(3);
        }

        return normalized.isBlank() ? null : normalized;
    }
}
