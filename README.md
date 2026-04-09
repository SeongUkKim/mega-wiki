# Mega-Wiki

신입과 주니어 구성원이 사내 정보를 찾느라 시간을 잃지 않도록, 질문을 위키 자산으로 전환하는 Spring Boot API 서버입니다.

## 핵심 시나리오

- 슬랙 질문을 입력하면 AI 1차 답변을 만들고 위키 페이지에 연결합니다.
- 동료가 페이지에 피드백을 남기면 문서가 계속 고도화됩니다.
- 메가원 에스크나 수동 입력으로 들어온 정보도 같은 위키 자산으로 통합합니다.
- 최종 기록은 Notion Data Source에 저장합니다.

## 현재 포함된 기능

- REST API 기반 대시보드 조회
- 신입 질문 등록 -> AI 답변 생성 -> 위키 페이지 자동 생성/연결
- 위키 페이지 직접 생성
- 페이지별 동료 피드백 추가 및 helpful 카운트 반영
- Notion 저장소 어댑터 + local 메모리 fallback
- 실제 Gemini/Slack/Vertex 연동을 교체하기 쉬운 인터페이스 분리

## 패키지 구조

- `config`: 애플리케이션 설정 및 스토리지 설정
- `domain`: 위키 페이지, 질문 스레드, 기여 이력 같은 핵심 도메인
- `repository`: 저장소 포트, 인메모리 구현체, Notion 어댑터
- `service`: 질문 처리 워크플로, AI 초안 생성, 대시보드 집계
- `web`: REST 컨트롤러, 예외 응답, 요청 DTO

## API 엔드포인트

- `GET /api/dashboard`
- `GET /api/pages`
- `GET /api/pages/{pageId}`
- `POST /api/pages`
- `POST /api/pages/{pageId}/contributions`
- `POST /api/pages/{pageId}/helpful`
- `POST /api/questions`

## 실행 방법

기본 프로필은 Notion 저장소를 사용합니다.

필수 환경 변수:

- `NOTION_API_TOKEN`
- `NOTION_PAGES_DATA_SOURCE_ID`
- `NOTION_QUESTIONS_DATA_SOURCE_ID`

로컬 데모는 메모리 저장소로 실행할 수 있습니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Windows PowerShell에서는 아래처럼 실행하면 됩니다.

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

## 참고 문서

- Notion 스키마: `docs/notion-schema.md`

## 다음 단계

- `AiAnswerGenerator`를 Gemini API 어댑터로 교체
- Slack Event API / Slash Command 연결
- Notion 변경 이력을 기반으로 감사 로그 보강
- 사내 문서 검색용 RAG 파이프라인 추가
- 인증/인가와 역할 기반 편집 정책 추가