# Mega-Wiki

Mega-Wiki는 Slack에서 발생한 사내의 질문을 지식 자산으로 변환하는 Spring Boot 백엔드입니다. Slack에서 질문을 하면 전체 흐름이 시작되고, Gemini가 답변 초안을 생성한 후, 결과를 Notion 저장소에 저장합니다.

## 현재 구현된 기능 목록

- Slack Events API의 `POST /api/integrations/slack/events`
- Slack 요청 서명 검증과 중복 이벤트 방지
- `app_mention` 이벤트 처리 및 Slack 스레드에 응답 게시
- Gemini `generateContent` 연동
- Gemini 호출 실패 또는 비활성화 시 rule-based 답변 생성기로 fallback
- Notion 데이터소스 자동 탐색과 스키마 자동 생성
- Notion 기반 `KnowledgePageRepository`, `QuestionThreadRepository`를 통한 영속 계층
- Notion, Gemini 자격 증명 없이 개발 가능한 인메모리 저장소

## 필수 환경 변수

### Notion

기본적으로 아래 이름의 데이터소스를 자동 탐색합니다.

- `MegaWiki Pages`
- `MegaWiki Questions`

필수:
- `NOTION_API_TOKEN`

선택:
- `NOTION_PAGES_DATA_SOURCE_ID`
- `NOTION_QUESTIONS_DATA_SOURCE_ID`
- `NOTION_PAGES_DATA_SOURCE_NAME`
- `NOTION_QUESTIONS_DATA_SOURCE_NAME`

즉, 먼저 Notion에서 데이터베이스를 만들고 integration을 연결해두면 기본적으로 `NOTION_API_TOKEN`만 넣으면 됩니다. 직접적인 data source id를 주면 자동 탐색보다 우선합니다.

### Slack

- `SLACK_ENABLED=true`
- `SLACK_BOT_TOKEN`
- `SLACK_SIGNING_SECRET`
- `SLACK_BOT_USER_ID`
  봇의 멤버 식별자 확인이 필요하며 선택적 항목입니다.

### Gemini

- `GEMINI_ENABLED=true`
- `GEMINI_API_KEY`
- `GEMINI_MODEL=gemini-2.5-flash`
  필요 시 다른 모델로 변경할 수 있습니다.
- `GEMINI_TEMPERATURE=0.2`
  필요 시 생성 온도를 조정할 수 있습니다.

## Notion 준비 단계

1. `MegaWiki Pages`, `MegaWiki Questions` 데이터베이스를 생성합니다.
2. 각 데이터베이스에 Notion integration을 연결합니다.
3. 어플리케이션 시작 시 Mega-Wiki가 데이터소스를 자동 탐색하고 스키마를 자동 생성하여 초기화합니다.

## Slack 봇 설정

1. Event Subscriptions을 활성화합니다.
2. Request URL을 배포된 Mega-Wiki 서버의 `/api/integrations/slack/events`로 설정합니다.
3. 봇 이벤트에 `app_mention`을 추가합니다.
4. 최소한 `app_mentions:read`, `chat:write` 스코프를 추가합니다.
5. 해당 워크스페이스에 설치하거나 재설치합니다.

## 로컬 실행

로컬 프로파일은 메모리 저장소를 사용하고 외부 연동을 비활성화합니다.

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

실제 Slack, Gemini, Notion 연동까지 확인하려면 해당 서비스에 앱을 설정하고, 적절한 자격 증명을 환경 변수로 설정하면 됩니다.

## 테스트

```powershell
.\gradlew.bat test --no-daemon
```

## API 목록

- `GET /api/dashboard`
- `GET /api/pages`
- `GET /api/pages/{pageId}`
- `POST /api/pages`
- `POST /api/pages/{pageId}/contributions`
- `POST /api/pages/{pageId}/helpful`
- `POST /api/questions`
- `POST /api/integrations/slack/events`

## 운영 참고

- Slack 이벤트는 응답 이후에 별도의 백그라운드 작업으로 처리합니다.
- Gemini가 비활성화되어 있거나 호출에 실패하면 내장 rule-based 답변 생성기로 자동 전환합니다.
- 현재 지식 페이지의 공식 아카이브 시스템 오브 레코드는 Notion입니다.

---

## Snowflake Cortex Search 연동 (FR-05)

Snowflake Cortex Search API를 통해 사내 문서 기반 AI 답변을 제공하는 기능입니다.

### 아키텍처

```
Slack Bot (@멘션) → Back-end Server → Snowflake Cortex Search API → Slack Thread 응답
```

`SNOWFLAKE_ENABLED=true`이면 Slack 질문이 Snowflake Cortex API로 전달되고, `false`이면 기존 Gemini 경로를 사용합니다.

### Snowflake 환경 변수

| 변수 | 필수 | 기본값 | 설명 |
|------|------|--------|------|
| `SNOWFLAKE_ENABLED` | N | `false` | Snowflake 연동 활성화 |
| `SNOWFLAKE_API_URL` | N | `https://mm0292ev17.execute-api.ap-northeast-2.amazonaws.com` | Cortex Search API 엔드포인트 |
| `SNOWFLAKE_SCORE_THRESHOLD` | N | `-7.0` | 관련도 점수 임계값 (이하이면 "등록되지 않은 질문") |
| `SNOWFLAKE_TIMEOUT` | N | `120` | API 응답 대기 시간 (초) |

### Snowflake Cortex Search API 스펙

**요청:**
```bash
curl -X POST https://mm0292ev17.execute-api.ap-northeast-2.amazonaws.com \
  -H "Content-Type: application/json" \
  -d '{"question": "명함 신청하는 방법이 궁금해"}'
```

**응답:**
```json
{
  "title": "8. 명함 신청하기",
  "answer": "그룹웨어 기안 양식 중 명함신청서 양식을 통해 기안해 주세요.",
  "reranker_score": -3.212328
}
```

- `reranker_score >= -7.0`: 관련 문서 있음 → 답변 반환
- `reranker_score < -7.0`: 관련 문서 없음 → "등록되어 있지 않은 질문" 반환
- 응답 시간: 최대 120초
- 언어: 한국어 입력 권장
- 현재 DB: 9가지 주제 (IT 장비, 버디, PoPs, 에스크, 그룹웨어, 이메일 서명, 명함, 프린터/스캐너, 인사카드)

---

## Slack Socket Mode

공개 URL 없이 WebSocket으로 Slack에 연결하는 방식입니다. 로컬 개발 환경에서 ngrok 없이 바로 테스트 가능합니다.

### Socket Mode vs HTTP Webhook

| | HTTP Webhook (`mode: http`) | Socket Mode (`mode: socket`) |
|---|---|---|
| 연결 방식 | Slack → 서버 HTTP POST | 서버 → Slack WebSocket |
| 공개 URL | 필요 (배포 or ngrok) | 불필요 |
| 필요 토큰 | `SLACK_SIGNING_SECRET` | `SLACK_APP_TOKEN` (`xapp-...`) |
| 용도 | 운영 환경 | 로컬 개발/테스트 |

### Socket Mode 설정

#### 1. Slack App 설정

1. [api.slack.com/apps](https://api.slack.com/apps) → 앱 선택
2. **App Home** → Display Name 설정 (예: `Mega-Wiki Bot`)
3. **OAuth & Permissions** → Bot Token Scopes:
   - `app_mentions:read`
   - `chat:write`
4. **Install to Workspace** → Bot Token (`xoxb-...`) 복사
5. **Socket Mode** → **Enable Socket Mode** 켜기
6. **Basic Information** → App-Level Tokens → **Generate Token**:
   - Token Name: `socket-mode`
   - Scope: `connections:write`
   - `xapp-...` 토큰 복사
7. **Event Subscriptions** → **Enable Events** 켜기
8. **Subscribe to bot events** → `app_mention` 추가 → **Save Changes**
9. 봇을 테스트할 채널에 초대: `/invite @Mega-Wiki Bot`

#### 2. 환경 변수

```bash
# .env 파일
SLACK_ENABLED=true
SLACK_MODE=socket
SLACK_BOT_TOKEN=xoxb-your-bot-token
SLACK_APP_TOKEN=xapp-your-app-level-token
SLACK_SIGNING_SECRET=your-signing-secret
SLACK_BOT_USER_ID=U_YOUR_BOT_ID
SNOWFLAKE_ENABLED=true
GEMINI_ENABLED=false
```

Bot User ID 조회:
```bash
curl -s -X POST https://slack.com/api/auth.test \
  -H "Authorization: Bearer xoxb-your-bot-token" \
  -H "Content-Type: application/json" | python3 -m json.tool
```

#### 3. 서버 실행

```bash
export $(cat .env | grep -v '^#' | xargs)
./gradlew bootRun --args='--spring.profiles.active=snowflake'
```

로그에 다음이 출력되면 연결 성공:
```
New session is open (session id: ...)
Slack Socket Mode connected successfully
```

#### 4. 테스트

Slack 채널에서:
```
@Mega-Wiki Bot 명함 신청하는 방법이 궁금해
```

### Slack 추가 환경 변수

| 변수 | 필수 | 기본값 | 설명 |
|------|------|--------|------|
| `SLACK_MODE` | N | `http` | `http` (Webhook) 또는 `socket` (Socket Mode) |
| `SLACK_APP_TOKEN` | Y* | | App-Level Token (`xapp-...`) |

\* Socket Mode 사용 시 필수

---

## Spring Profiles

| Profile | 저장소 | Slack | AI 답변 | 용도 |
|---------|--------|-------|---------|------|
| (기본) | Notion | HTTP Webhook | Gemini | 운영 |
| `local` | 인메모리 | 비활성화 | 비활성화 | 로컬 개발 |
| `snowflake` | 인메모리 | Socket Mode | Snowflake Cortex | Snowflake 테스트 |

## 프로젝트 구조

```
src/main/java/com/megawiki/
├── MegaWikiApplication.java
├── config/                           # 설정 (SlackProperties, SnowflakeProperties, ...)
├── domain/                           # 도메인 모델 (KnowledgePage, QuestionThread)
├── integration/
│   ├── slack/                        # Slack 연동
│   │   ├── SlackEventController.java # HTTP Webhook 엔드포인트
│   │   ├── SlackEventService.java    # 이벤트 처리 (Snowflake/Gemini 분기)
│   │   ├── SlackSocketModeRunner.java# Socket Mode WebSocket 연결
│   │   ├── SlackApiClient.java       # Slack API 호출
│   │   ├── SlackSignatureVerifier.java
│   │   └── SlackEventDeduplicator.java
│   └── snowflake/                    # Snowflake 연동
│       ├── SnowflakeCortexClient.java# Cortex Search API 호출
│       └── SnowflakeCortexResponse.java
├── repository/                       # 데이터 저장소 (Notion/InMemory)
├── service/                          # 비즈니스 로직
└── web/                              # REST API 컨트롤러
```
