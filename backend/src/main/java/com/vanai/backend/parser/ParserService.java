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

            String rawValue = element.attr("href")
                    .replaceFirst("(?i)^tel:", "")
                    .trim();

            Matcher telMatcher =
                    PHONE_PATTERN.matcher(rawValue);

            while (telMatcher.find()) {

                String normalized =
                        normalizePhone(telMatcher.group());

                if (normalized != null
                        && !normalized.isBlank()) {
                    phones.add(normalized);
                }
            }
        }

        Matcher matcher = PHONE_PATTERN.matcher(document.text());

        while (matcher.find()) {
            phones.add(normalizePhone(matcher.group()));
        }

        return phones;
    }

    private String extractOrganizationName(Document document) {

        // 1순위: og:site_name
        Element siteName =
                document.selectFirst("meta[property=og:site_name]");

        if (siteName != null) {
            String value = siteName.attr("content").trim();

            if (!value.isBlank()) {
                return value;
            }
        }

        // 2순위: application-name
        Element applicationName =
                document.selectFirst("meta[name=application-name]");

        if (applicationName != null) {
            String value =
                    applicationName.attr("content").trim();

            if (!value.isBlank()) {
                return value;
            }
        }

        // 3순위: title을 breadcrumb 단위로 분리
        String title = document.title();

        if (title == null || title.isBlank()) {
            return null;
        }

        String[] parts = title.split(
                "\\s*(?:<|>|\\||·|–|—)\\s*|\\s+-\\s+"
        );

        // 사이트명은 보통 title의 오른쪽에 있으므로
        // 뒤에서부터 의미 있는 값을 찾음
        for (int i = parts.length - 1; i >= 0; i--) {

            String candidate = parts[i].trim();

            if (candidate.isBlank()) {
                continue;
            }

            if (isGenericPageLabel(candidate)) {
                continue;
            }

            return candidate;
        }

        return title.trim();
    }

    private boolean isGenericPageLabel(String value) {

        String normalized = value
                .replaceAll("\\s+", "")
                .toLowerCase();

        return normalized.equals("소개")
                || normalized.equals("회사소개")
                || normalized.equals("기관소개")
                || normalized.equals("센터소개")
                || normalized.equals("공지사항")
                || normalized.equals("지원사업")
                || normalized.equals("접수마감")
                || normalized.equals("상세")
                || normalized.equals("상세보기")
                || normalized.equals("홈")
                || normalized.equals("home");
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

        String text = document.text();

        Pattern departmentPattern = Pattern.compile(
                "(?:담당부서|담당 부서)\\s*[:：]?\\s*(.{2,100}?)(?=\\s*(?:접수기간|신청기간|사업기간|문의처|담당자|접수방법|$))"
        );

        Matcher matcher = departmentPattern.matcher(text);

        if (!matcher.find()) {
            return null;
        }

        String department = matcher.group(1)
                .replaceAll("\\s+", " ")
                .trim();

        if (department.isBlank()) {
            return null;
        }

        return department;
    }

    private String extractPosition(Document document) {
        return null;
    }

    private String normalizePhone(String phone) {

    if (phone == null || phone.isBlank()) {
        return null;
    }

    String value = phone.trim()
            .replaceAll("[^0-9+]", "");

    if (value.startsWith("+82")) {
        value = "0" + value.substring(3);
    }

    String digits = value.replaceAll("[^0-9]", "");

    if (digits.length() == 9 && digits.startsWith("02")) {
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

    private String firstOrNull(Set<String> values) {
        return values.stream().findFirst().orElse(null);
    }
}
