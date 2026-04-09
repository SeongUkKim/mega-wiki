# Mega-Wiki

Mega-Wiki는 Slack에서 발생한 질문을 재사용 가능한 팀 지식으로 전환하는 Spring Boot 백엔드입니다. Slack에서 봇을 멘션하면 질문 흐름이 시작되고, Gemini가 답변 초안을 생성한 뒤, 결과를 기존 Notion 저장소에 기록합니다.

## 현재 구현된 기능

- Slack Events API용 `POST /api/integrations/slack/events`
- Slack 요청 서명 검증과 중복 이벤트 방지
- `app_mention` 이벤트 처리 및 Slack 스레드 답글 전송
- Gemini `generateContent` 연동
- Gemini 호출 실패 또는 비활성화 시 rule-based 답변 생성기로 fallback
- Notion 기반 `KnowledgePageRepository`, `QuestionThreadRepository`를 통한 영속 저장
- Notion, Gemini 자격 증명 없이 개발 가능한 로컬 메모리 프로필

## 필수 환경 변수

### Notion

- `NOTION_API_TOKEN`
- `NOTION_PAGES_DATA_SOURCE_ID`
- `NOTION_QUESTIONS_DATA_SOURCE_ID`

### Slack

- `SLACK_ENABLED=true`
- `SLACK_BOT_TOKEN`
- `SLACK_SIGNING_SECRET`
- `SLACK_BOT_USER_ID`
  향후 멘션 식별 로직 확장용이며 현재는 선택값입니다.

### Gemini

- `GEMINI_ENABLED=true`
- `GEMINI_API_KEY`
- `GEMINI_MODEL=gemini-2.5-flash`
  필요 시 다른 모델로 변경할 수 있습니다.
- `GEMINI_TEMPERATURE=0.2`
  필요 시 생성 성향 조정에 사용합니다.

## Slack 앱 설정

1. Event Subscriptions를 활성화합니다.
2. Request URL을 배포된 Mega-Wiki 서버의 `/api/integrations/slack/events`로 설정합니다.
3. 봇 이벤트로 `app_mention`을 구독합니다.
4. 최소한 `app_mentions:read`, `chat:write` 스코프를 추가합니다.
5. 앱을 워크스페이스에 설치하거나 재설치합니다.

## 로컬 실행

로컬 프로필은 메모리 저장소를 사용하고 외부 연동을 비활성화합니다.

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

실제 Slack, Gemini, Notion 연동까지 확인하려면 로컬 프로필 없이 실행하고, 실제 자격 증명을 환경 변수로 주입하면 됩니다.

## 테스트

```powershell
.\gradlew.bat test --no-daemon
```

## API 요약

- `GET /api/dashboard`
- `GET /api/pages`
- `GET /api/pages/{pageId}`
- `POST /api/pages`
- `POST /api/pages/{pageId}/contributions`
- `POST /api/pages/{pageId}/helpful`
- `POST /api/questions`
- `POST /api/integrations/slack/events`

## 참고 사항

- Slack 이벤트는 즉시 응답한 뒤 백그라운드 작업으로 처리합니다.
- Gemini가 비활성화되어 있거나 호출에 실패하면 기존 rule-based 답변 생성기로 자동 전환합니다.
- 최종 지식 페이지와 질문 아카이브의 시스템 오브 레코드는 Notion입니다.