package com.vanai.backend.parser;

import com.vanai.backend.parser.dto.ParserResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ParserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+82[-\\s]?)?(?:0\\d{1,2})[-\\s)]?\\d{3,4}[-\\s]?\\d{4}"
    );

    private static final Pattern PERSON_PATTERN = Pattern.compile(
            "([가-힣]{2,4})\\s*(?:팀장|담당자|매니저|과장|대리|부장|이사|대표|CEO)"
    );

    public ParserResponse parse(String url) {
        validateUrl(url);

        try {
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 ContactResearchBot/1.0")
                    .timeout(10000)
                    .followRedirects(true)
                    .get();

            Set<String> emails = extractEmails(document);
            Set<String> phones = extractPhones(document);

            String organizationName = extractOrganizationName(document);
            String personName = extractPersonName(document);
            String department = extractDepartment(document);
            String position = extractPosition(document);

            return new ParserResponse(
                    organizationName,
                    personName,
                    department,
                    position,
                    firstOrNull(emails),
                    firstOrNull(phones),
                    url
            );

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "웹페이지를 불러올 수 없습니다."
            );
        }
    }

    private void validateUrl(String url) {
        try {
            URI uri = URI.create(url);

            String scheme = uri.getScheme();

            if (scheme == null ||
                    (!scheme.equalsIgnoreCase("http") &&
                     !scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException();
            }

            if (uri.getHost() == null) {
                throw new IllegalArgumentException();
            }

            String host = uri.getHost().toLowerCase();

            if (host.equals("localhost")
                    || host.equals("127.0.0.1")
                    || host.equals("::1")) {
                throw new IllegalArgumentException();
            }

        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "올바른 http/https URL을 입력해주세요."
            );
        }
    }

    private Set<String> extractEmails(Document document) {
        Set<String> emails = new LinkedHashSet<>();

        for (Element element : document.select("a[href^=mailto:]")) {
            String email = element.attr("href")
                    .replaceFirst("(?i)^mailto:", "")
                    .split("\\?")[0]
                    .trim();

            if (EMAIL_PATTERN.matcher(email).matches()) {
                emails.add(email.toLowerCase());
            }
        }

        Matcher matcher = EMAIL_PATTERN.matcher(document.text());

        while (matcher.find()) {
            emails.add(matcher.group().toLowerCase());
        }

        return emails;
    }

    private Set<String> extractPhones(Document document) {
        Set<String> phones = new LinkedHashSet<>();

        for (Element element : document.select("a[href^=tel:]")) {
            String phone = element.attr("href")
                    .replaceFirst("(?i)^tel:", "")
                    .trim();

            if (!phone.isBlank()) {
                phones.add(normalizePhone(phone));
            }
        }

        Matcher matcher = PHONE_PATTERN.matcher(document.text());

        while (matcher.find()) {
            phones.add(normalizePhone(matcher.group()));
        }

        return phones;
    }

    private String extractOrganizationName(Document document) {
        Element siteName = document.selectFirst("meta[property=og:site_name]");

        if (siteName != null && !siteName.attr("content").isBlank()) {
            return siteName.attr("content").trim();
        }

        String title = document.title();

        if (title == null || title.isBlank()) {
            return null;
        }

        String[] parts = title.split("\\s[-|]\\s");

        return parts[0].trim();
    }

    private String extractPersonName(Document document) {
        for (Element element : document.select("a[href^=mailto:]")) {
            String nearbyText = element.parent() != null
                    ? element.parent().text()
                    : element.text();

            Matcher matcher = PERSON_PATTERN.matcher(nearbyText);

            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }

    private String extractDepartment(Document document) {
        return null;
    }

    private String extractPosition(Document document) {
        return null;
    }

    private String normalizePhone(String phone) {
        return phone
                .replaceAll("[^0-9+]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private String firstOrNull(Set<String> values) {
        return values.stream().findFirst().orElse(null);
    }
}
