# Contact Research Automation

AI-powered contact research and list-up automation for VAN.

기업·기관 웹페이지를 자동으로 탐색하여 연락처 정보를 수집하고,  
수집된 결과를 수정·선택·제외·복사·CSV 다운로드할 수 있는 웹 애플리케이션입니다.

> 현재 버전은 Contact Parser, 동일 도메인 Crawler, SSE 진행률, H2 저장, 결과 관리 기능까지 구현되어 있습니다.  
> 검색 키워드를 실제 검색 엔진 또는 URL Discovery 단계와 연결하는 기능은 향후 개선 범위입니다.

---

## 1. 프로젝트 개요

Contact Research Automation은 기업 및 기관의 웹페이지를 순회하면서 다음 정보를 자동으로 추출합니다.

- 기업 / 기관명
- 담당자명
- 담당부서
- 직책
- 이메일
- 전화번호
- 출처 URL

Crawler가 동일 도메인의 페이지를 탐색하고 Parser가 Contact 정보를 추출한 뒤, 정규화와 중복 제거 과정을 거쳐 H2 Database에 저장합니다.

Frontend에서는 Server-Sent Events(SSE)를 통해 수집 진행 상황을 실시간으로 확인할 수 있으며, 수집 완료 후 결과를 직접 수정하거나 제외하고 Clipboard 및 CSV로 내보낼 수 있습니다.

---

## 2. 주요 기능

### Keyword UI

- 검색 키워드 입력
- 여러 키워드 추가
- 등록한 키워드 삭제
- 수집 진행 중 입력 제어

현재 Keyword UI는 구현되어 있지만 검색 키워드를 실제 검색 엔진 또는 URL Discovery에 연결하는 기능은 아직 구현되지 않았습니다.

### Web Page Parser

웹페이지에서 다음 데이터를 추출합니다.

- `organizationName`
- `personName`
- `department`
- `position`
- `email`
- `phone`
- `sourceUrl`

Parser에는 다음 전처리 및 정확도 개선 로직이 포함되어 있습니다.

- `mailto:` 기반 이메일 추출
- `tel:` 기반 전화번호 추출
- 이메일 정규화
- 전화번호 표준 형식 정규화
- 잘못된 전화번호 문자열 오염 방지
- `og:site_name` 기반 기관명 추출
- HTML title fallback 및 breadcrumb 후처리
- 범용 페이지명 제거
- `담당부서` 문맥 기반 부서명 추출

예:

```text
0517151753
→ 051-715-1753
```

```text
소개 < 부니콘(BUNICORN) < 부산창업생태계 < 부산창업포털
→ 부산창업포털
```

### Domain Crawler

- 동일 도메인 내부 링크 순회
- 최대 탐색 페이지 수 제한
- URL 중복 방문 방지
- 외부 도메인 제외
- HTML 후보 URL 필터링
- 요청 간 Rate Limit
- Background Crawl
- 사용자 Stop 지원
- 시작 URL 접속 실패 처리

지원 상태:

```text
RUNNING
COMPLETED
STOPPED
FAILED
```

시작 URL 자체에 접속할 수 없는 경우 전체 Crawl을 `FAILED` 처리합니다.

반면 탐색 중 일부 하위 페이지에서 오류가 발생하면 전체 Crawl을 중단하지 않고 다음 URL을 계속 탐색합니다.

### Contact Deduplication

수집 과정에서 동일 Contact가 반복 저장되는 것을 최소화합니다.

주요 중복 판단 기준:

- Email
- 정규화된 Phone

### Crawl Session / Contact Persistence

수집 작업과 결과는 H2 Database에 저장됩니다.

주요 Entity:

```text
CrawlSession
Contact
```

CrawlSession에는 다음과 같은 정보가 저장됩니다.

```text
jobId
startUrl
status
maxPages
visitedPages
collectedContacts
createdAt
```

Contact에는 다음과 같은 정보가 저장됩니다.

```text
organizationName
personName
department
position
email
phone
sourceUrl
deletedAt
```

### SSE Progress

Frontend는 SSE를 이용해 Backend의 Crawl 진행 상황을 실시간으로 전달받습니다.

주요 이벤트:

```text
connected
progress
completed
stopped
failed
```

Frontend에서 다음 정보를 확인할 수 있습니다.

- 현재 Crawl 상태
- 방문 페이지 수
- 최대 페이지 수
- 진행률
- 수집 Contact 수

### Contact Result Table

수집 완료 후 Session에 저장된 Contact를 자동으로 조회하여 테이블로 표시합니다.

표시 정보:

```text
기업/기관
담당자
부서
직책
이메일
전화번호
출처
관리
```

### Inline Edit

수집된 Contact를 Frontend 테이블에서 직접 수정할 수 있습니다.

수정 가능 항목:

```text
기업/기관명
담당자
부서
직책
이메일
전화번호
```

지원 기능:

- 수정
- 저장
- 취소
- 이메일 Validation
- 전화번호 정규화

### Soft Delete

Contact를 실제 DB에서 삭제하지 않고 `deletedAt`을 기록하는 방식으로 제외합니다.

지원 기능:

- 개별 선택
- 전체 선택
- 선택 Contact 일괄 제외
- Bulk Soft Delete
- 제외 후 UI 즉시 반영

Soft Delete된 Contact는 일반 Session Contact 조회 결과에서 제외됩니다.

### Clipboard

수집된 정보를 Clipboard로 복사할 수 있습니다.

지원 기능:

- Contact 한 건 복사
- 선택한 Contact 여러 건 복사
- 복사 성공 / 실패 메시지

예:

```text
기업/기관: 부산창업포털
담당자:
부서: 글로벌OI팀
직책:
이메일: bkkim@ccei.kr
전화번호: 051-749-8947
출처: https://busanstartup.kr/...
```

### CSV Export

수집 결과를 CSV 파일로 다운로드할 수 있습니다.

지원 기능:

- 전체 Contact 다운로드
- 선택 Contact 다운로드
- UTF-8 BOM
- 한글 Excel 호환
- CSV Escape 처리
- 전화번호 Excel 자동 숫자 변환 대응
- 날짜 / 시간 기반 파일명

CSV 컬럼:

```text
기업/기관,담당자,부서,직책,이메일,전화번호,출처
```

---

## 3. 기술 스택

### Backend

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- Jsoup
- H2 Database
- Server-Sent Events
- Gradle

### Frontend

- React
- Vite
- JavaScript
- CSS
- Fetch API
- EventSource API
- Clipboard API

### Development

- Git
- GitHub
- VS Code
- WSL / Ubuntu

---

## 4. 프로젝트 구조

```text
contact-research-automation
├── backend
│   ├── src
│   │   ├── main
│   │   │   ├── java
│   │   │   │   └── com/vanai/backend
│   │   │   │       ├── config
│   │   │   │       ├── crawler
│   │   │   │       ├── parser
│   │   │   │       └── ...
│   │   │   └── resources
│   │   └── test
│   │       └── resources
│   │           └── application.properties
│   ├── data
│   ├── build.gradle
│   └── gradlew
│
├── frontend
│   ├── src
│   │   ├── components
│   │   │   └── ContactTable.jsx
│   │   ├── App.jsx
│   │   └── App.css
│   └── package.json
│
└── README.md
```

---

## 5. 사전 요구사항

Backend 실행을 위해 Java가 필요합니다.

확인:

```bash
java -version
```

Frontend 실행을 위해 Node.js와 npm이 필요합니다.

확인:

```bash
node -v
npm -v
```

---

## 6. 실행 방법

### Repository 이동

```bash
cd contact-research-automation
```

### Backend 실행

```bash
cd backend
./gradlew bootRun
```

기본 Backend 주소:

```text
http://localhost:8080
```

### Frontend 실행

새 터미널을 열고 프로젝트 루트에서:

```bash
cd frontend
npm install
npm run dev
```

기본 Frontend 주소:

```text
http://localhost:5173
```

5173 포트가 이미 사용 중이면 Vite가 5174 등 다른 포트를 사용할 수 있습니다.

현재 개발용 Backend CORS 설정은 Vite 개발 환경의 5173 / 5174 포트를 지원합니다.

---

## 7. 사용 흐름

```text
검색 키워드 입력
        ↓
START
        ↓
Crawl Job 생성
        ↓
SSE 연결
        ↓
동일 도메인 페이지 탐색
        ↓
Contact Parser 실행
        ↓
Email / Phone 정규화
        ↓
중복 제거
        ↓
Crawl Session / Contact 저장
        ↓
COMPLETED
        ↓
Contact 결과 조회
        ↓
수정 / 선택 / 제외 / 복사 / CSV 다운로드
```

현재 Frontend의 Crawl 시작 URL은 통합 테스트를 위한 URL을 기준으로 동작합니다.

예:

```text
https://busanstartup.kr
```

검색 키워드와 실제 Crawl Target Discovery의 연결은 향후 개선 범위입니다.

---

## 8. 주요 API

### Parser

```http
POST /api/parser
```

Request:

```json
{
  "url": "https://busanstartup.kr"
}
```

### Crawl 시작

```http
POST /api/crawl/start
```

Request 예:

```json
{
  "url": "https://busanstartup.kr",
  "maxPages": 20
}
```

### Crawl 상태 조회

```http
GET /api/crawl/{jobId}
```

### Crawl 중지

```http
POST /api/crawl/{jobId}/stop
```

### SSE 진행률

```http
GET /api/crawl/{jobId}/events
```

### Session 목록

```http
GET /api/sessions
```

### Session 상세

```http
GET /api/sessions/{jobId}
```

### Session Contact 조회

```http
GET /api/sessions/{jobId}/contacts
```

Soft Delete된 Contact는 반환하지 않습니다.

### Contact 수정

```http
PATCH /api/contacts/{contactId}
```

Request 예:

```json
{
  "personName": "김민지",
  "department": "대외협력팀",
  "position": "매니저",
  "email": "minji@example.com",
  "phone": "051 123 4567"
}
```

### Contact 제외

```http
PATCH /api/contacts/exclude
```

Request:

```json
{
  "contactIds": [1, 2]
}
```

---

## 9. Database

개발 환경에서는 파일형 H2 Database를 사용합니다.

Runtime DB 파일은 다음 디렉터리에 생성됩니다.

```text
backend/data/
```

이 DB 파일들은 실행 과정에서 변경될 수 있으므로 코드 변경사항과 함께 Git Commit하지 않습니다.

테스트에서는 실행 중인 개발 DB와 충돌하지 않도록 별도의 In-Memory H2 Database를 사용합니다.

테스트 설정:

```text
backend/src/test/resources/application.properties
```

테스트 DB:

```text
jdbc:h2:mem:contact-test
```

이를 통해 Backend가 실행 중인 상태와 Gradle Test의 DB File Lock 충돌을 방지합니다.

---

## 10. 테스트

### Backend Test

Backend 서버가 실행 중이라면 필요에 따라 종료한 뒤 실행합니다.

```bash
cd backend
./gradlew clean test
```

정상 결과:

```text
BUILD SUCCESSFUL
```

### Frontend Build

```bash
cd frontend
npm run build
```

정상 결과 예:

```text
✓ built
```

---

## 11. 통합 검증 시나리오

### 정상 Crawl

확인 항목:

```text
RUNNING
SSE progress
COMPLETED
Contact DB 저장
Frontend 결과 표시
```

### Stop Flow

확인 항목:

```text
RUNNING
Stop 요청
STOPPED
SSE stopped
Session STOPPED
```

### Failed Flow

접속 불가능한 시작 URL을 이용해 검증할 수 있습니다.

예:

```text
https://this-domain-does-not-exist.invalid
```

정상 동작:

```text
Crawl Job FAILED
Crawl Session FAILED
```

### 결과 관리

확인 항목:

```text
Inline Edit
Email Validation
Checkbox Selection
Soft Delete
Clipboard Copy
CSV Export
```

---

## 12. Parser 검증 사례

### 부산창업포털

Parser 결과 예:

```text
organizationName: 부산창업포털
phone: 051-715-1753
```

### 부산창조경제혁신센터 창업육성팀

검증 페이지에서 다음과 같이 추출됩니다.

```text
organizationName: 부산창업포털
department: 부산창조경제혁신센터 창업육성팀
email: wlszh19@ccei.kr
phone: 051-749-8963
```

### 글로벌OI팀

검증 결과:

```text
organizationName: 부산창업포털
department: 글로벌OI팀
email: bkkim@ccei.kr
phone: 051-749-8947
```

페이지에 실제 담당자명이나 직책 정보가 존재하지 않는 경우 `personName`과 `position`은 오탐 방지를 위해 `null`을 유지합니다.

---

## 13. 주요 예외 처리

```text
잘못된 URL
→ HTTP 400

빈 URL
→ HTTP 400

maxPages = 0
→ HTTP 400

존재하지 않는 Crawl Job
→ HTTP 404

존재하지 않는 Session
→ HTTP 404

존재하지 않는 Contact
→ HTTP 404

잘못된 이메일
→ HTTP 400

시작 URL 접속 실패
→ Crawl FAILED

Soft Delete된 Contact
→ 일반 Session Contact 조회에서 제외
```

---

## 14. 알려진 제한사항

1. Keyword UI와 실제 검색 엔진 / Crawl Target Discovery가 아직 연결되어 있지 않습니다.

2. 현재 Frontend의 시작 URL은 통합 테스트 목적의 대상 URL을 기준으로 구성되어 있습니다.

3. Jsoup 기반 Crawler이므로 JavaScript 렌더링 후 생성되는 데이터는 추출하지 못할 수 있습니다.

4. 로그인, CAPTCHA, 인증 또는 접근 제한이 필요한 페이지는 지원하지 않습니다.

5. 웹사이트마다 DOM 구조가 다르기 때문에 모든 사이트의 담당자명, 담당부서, 직책을 완벽하게 추출할 수 없습니다.

6. 한 페이지에 여러 이메일 또는 전화번호가 존재할 경우 현재 우선순위 정책에 따라 첫 번째 유효 값이 선택될 수 있습니다.

7. 여러 담당자가 동일 이메일 또는 전화번호를 공유하면 Deduplication 과정에서 하나의 Contact로 처리될 가능성이 있습니다.

8. 페이지 새로고침 시 Frontend의 현재 Job 상태가 자동 복원되지는 않습니다.

9. Soft Delete된 Contact를 복구하는 Frontend UI는 현재 구현되어 있지 않습니다.

10. 이미 DB에 저장된 과거 Contact는 Parser 로직 변경 후 자동으로 재처리되지 않습니다.

11. 외부 사이트의 HTML 구조가 변경될 경우 Parser 정확도에 영향을 줄 수 있습니다.

12. 파일형 H2 Database는 개발 및 테스트 목적이며 Production 환경용 Database는 별도로 구성해야 합니다.

---

## 15. 향후 개선 방향

```text
Keyword → Search / Discovery 연결
자동 Crawl Target 탐색
사이트별 Parser Rule
담당자명 추출 정확도 개선
직책 추출 정확도 개선
Contact Confidence Score
결과 검색
결과 정렬
결과 필터
Pagination
XLSX Export
Soft Delete 복구
Session 자동 복원
자동 E2E Test
Production Database
Structured Logging
Monitoring
배포 환경 구성
```

---

## 16. 개발 진행 범위

본 프로젝트는 다음 단계로 개발되었습니다.

```text
T1  개발환경 / API 기반 구축
T2  Keyword UI
T3  Web Page Parser
T4  Domain Crawler
T5  Contact Deduplication / Validation
T6  Crawl Session / Contact Persistence
T7  SSE Progress
T8  Contact Result Table / Inline Edit
T9  Selection / Soft Delete
T10 Clipboard / CSV Export
T11 Integration / Exception Handling / Parser Accuracy
T12 README / Run Guide / Final Verification
```

---

## 17. 최종 상태

현재 구현된 주요 흐름:

```text
Frontend 입력
→ Crawl 시작
→ Background Crawler
→ SSE Progress
→ Contact Parsing
→ Deduplication
→ DB Persistence
→ Result Table
→ Inline Edit
→ Soft Delete
→ Clipboard
→ CSV Export
```

현재 단계에서는 개발·통합 검증 가능한 MVP 수준의 Contact Research Automation을 제공합니다.