package com.vanai.backend.persistence.service;

import com.vanai.backend.persistence.dto.ContactExcludeRequest;
import com.vanai.backend.persistence.dto.ContactExcludeResponse;
import com.vanai.backend.persistence.dto.ContactResponse;
import com.vanai.backend.persistence.dto.ContactUpdateRequest;
import com.vanai.backend.persistence.entity.Contact;
import com.vanai.backend.persistence.repository.ContactRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

        if (contact.isDeleted()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "이미 제외된 연락처입니다."
            );
        }

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

    @Transactional
    public ContactExcludeResponse excludeContacts(
            ContactExcludeRequest request
    ) {

        if (request == null
                || request.contactIds() == null
                || request.contactIds().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "제외할 연락처를 선택해주세요."
            );
        }

        Set<Long> uniqueIds =
                new LinkedHashSet<>(request.contactIds());

        if (uniqueIds.contains(null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "올바르지 않은 연락처 ID가 포함되어 있습니다."
            );
        }

        List<Contact> contacts =
                contactRepository.findAllById(uniqueIds);

        if (contacts.size() != uniqueIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "존재하지 않는 연락처가 포함되어 있습니다."
            );
        }

        int excludedCount = 0;

        for (Contact contact : contacts) {
            if (!contact.isDeleted()) {
                contact.softDelete();
                excludedCount++;
            }
        }

        return new ContactExcludeResponse(
                excludedCount,
                List.copyOf(uniqueIds)
        );
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