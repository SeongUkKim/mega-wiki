# Notion Data Source Schema

`mega-wiki.storage=notion` 모드에서는 아래 두 개의 Notion Data Source를 준비해야 합니다.

## 1. Pages Data Source

필수 속성 이름:

- `Name` : Title
- `Slug` : Rich text
- `Summary` : Rich text
- `Content` : Rich text
- `ContributionsJson` : Rich text
- `SourceType` : Select (`SLACK_THREAD`, `MEGAONE_ASK`, `MANUAL`)
- `Status` : Select (`DRAFT`, `CURATED`, `VERIFIED`)
- `Tags` : Multi-select
- `LinkedQuestions` : Number
- `HelpfulCount` : Number

## 2. Questions Data Source

필수 속성 이름:

- `Name` : Title
- `Author` : Rich text
- `Channel` : Rich text
- `Question` : Rich text
- `AiAnswer` : Rich text
- `LinkedPageId` : Rich text
- `SourceType` : Select (`SLACK_THREAD`, `MEGAONE_ASK`, `MANUAL`)

## 환경 변수

- `NOTION_API_TOKEN`
- `NOTION_PAGES_DATA_SOURCE_ID`
- `NOTION_QUESTIONS_DATA_SOURCE_ID`
- 선택: `NOTION_API_VERSION`

## 실행 예시

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

로컬 데모는 `local` 프로필로 메모리 저장소를 사용하고, 실제 기록은 기본 프로필의 Notion 저장소를 사용합니다.