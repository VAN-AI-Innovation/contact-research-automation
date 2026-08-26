package com.vanai.backend.contact;

import com.vanai.backend.parser.dto.ParserResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ContactDeduplicationService {

    private final ContactNormalizer normalizer;
    private final EmailValidator emailValidator;

    public ContactDeduplicationService(
            ContactNormalizer normalizer,
            EmailValidator emailValidator
    ) {
        this.normalizer = normalizer;
        this.emailValidator = emailValidator;
    }

    public List<ParserResponse> deduplicate(
            List<ParserResponse> candidates
    ) {

        List<ParserResponse> result = new ArrayList<>();

        if (candidates == null) {
            return result;
        }

        for (ParserResponse candidate : candidates) {

            ParserResponse cleaned = sanitize(candidate);

            if (cleaned == null) {
                continue;
            }

            int duplicateIndex =
                    findDuplicateIndex(result, cleaned);

            if (duplicateIndex < 0) {
                result.add(cleaned);
            } else {

                ParserResponse existing =
                        result.get(duplicateIndex);

                result.set(
                        duplicateIndex,
                        merge(existing, cleaned)
                );
            }
        }

        return result;
    }

    private ParserResponse sanitize(
            ParserResponse response
    ) {

        if (response == null) {
            return null;
        }

        String email =
                normalizer.normalizeEmail(response.email());

        if (!emailValidator.isValid(email)) {
            email = null;
        }

        String normalizedPhone =
                normalizer.normalizePhone(response.phone());

        if (email == null && normalizedPhone == null) {
            return null;
        }

        String displayPhone =
                trimToNull(response.phone());

        return new ParserResponse(
                trimToNull(response.organizationName()),
                trimToNull(response.personName()),
                trimToNull(response.department()),
                trimToNull(response.position()),
                email,
                displayPhone,
                trimToNull(response.sourceUrl())
        );
    }

    private int findDuplicateIndex(
            List<ParserResponse> contacts,
            ParserResponse target
    ) {

        String targetEmail =
                normalizer.normalizeEmail(target.email());

        String targetPhone =
                normalizer.normalizePhone(target.phone());

        for (int i = 0; i < contacts.size(); i++) {

            ParserResponse existing = contacts.get(i);

            String existingEmail =
                    normalizer.normalizeEmail(
                            existing.email()
                    );

            String existingPhone =
                    normalizer.normalizePhone(
                            existing.phone()
                    );

            boolean sameEmail =
                    targetEmail != null
                            && existingEmail != null
                            && targetEmail.equals(existingEmail);

            boolean samePhone =
                    targetPhone != null
                            && existingPhone != null
                            && targetPhone.equals(existingPhone);

            if (sameEmail || samePhone) {
                return i;
            }
        }

        return -1;
    }

    private ParserResponse merge(
            ParserResponse first,
            ParserResponse second
    ) {

        return new ParserResponse(
                pick(first.organizationName(),
                        second.organizationName()),

                pick(first.personName(),
                        second.personName()),

                pick(first.department(),
                        second.department()),

                pick(first.position(),
                        second.position()),

                pick(first.email(),
                        second.email()),

                pick(first.phone(),
                        second.phone()),

                pick(first.sourceUrl(),
                        second.sourceUrl())
        );
    }

    private String pick(
            String first,
            String second
    ) {

        String firstValue = trimToNull(first);
        String secondValue = trimToNull(second);

        if (firstValue == null) {
            return secondValue;
        }

        if (secondValue == null) {
            return firstValue;
        }

        return secondValue.length() > firstValue.length()
                ? secondValue
                : firstValue;
    }

    private String trimToNull(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isBlank()
                ? null
                : trimmed;
    }
}
