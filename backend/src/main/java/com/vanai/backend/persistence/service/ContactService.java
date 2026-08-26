package com.vanai.backend.persistence.service;

import com.vanai.backend.persistence.dto.ContactResponse;
import com.vanai.backend.persistence.dto.ContactUpdateRequest;
import com.vanai.backend.persistence.entity.Contact;
import com.vanai.backend.persistence.repository.ContactRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

@Service
public class ContactService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile(
                    "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
            );

    private final ContactRepository contactRepository;

    public ContactService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @Transactional
    public ContactResponse updateContact(
            Long contactId,
            ContactUpdateRequest request
    ) {

        Contact contact = contactRepository
                .findById(contactId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "존재하지 않는 연락처입니다."
                        )
                );

        String email = normalizeEmail(request.email());
        String phone = normalizePhone(request.phone());

        contact.update(
                request.organizationName(),
                request.personName(),
                request.department(),
                request.position(),
                email,
                phone
        );

        return ContactResponse.from(contact);
    }

    private String normalizeEmail(String email) {

        if (email == null) {
            return null;
        }

        String value = email.trim();

        if (value.isEmpty()) {
            return "";
        }

        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "올바른 이메일 형식을 입력해주세요."
            );
        }

        return value.toLowerCase();
    }

    private String normalizePhone(String phone) {

        if (phone == null) {
            return null;
        }

        String value = phone.trim();

        if (value.isEmpty()) {
            return "";
        }

        value = value.replaceAll("[^0-9+]", "");

        if (value.startsWith("+82")) {
            value = "0" + value.substring(3);
        }

        String digits = value.replaceAll("[^0-9]", "");

        if (digits.length() == 9
                && digits.startsWith("02")) {

            return digits.substring(0, 2)
                    + "-"
                    + digits.substring(2, 5)
                    + "-"
                    + digits.substring(5);
        }

        if (digits.length() == 10) {

            if (digits.startsWith("02")) {
                return digits.substring(0, 2)
                        + "-"
                        + digits.substring(2, 6)
                        + "-"
                        + digits.substring(6);
            }

            return digits.substring(0, 3)
                    + "-"
                    + digits.substring(3, 6)
                    + "-"
                    + digits.substring(6);
        }

        if (digits.length() == 11) {
            return digits.substring(0, 3)
                    + "-"
                    + digits.substring(3, 7)
                    + "-"
                    + digits.substring(7);
        }

        return phone.trim();
    }
}
