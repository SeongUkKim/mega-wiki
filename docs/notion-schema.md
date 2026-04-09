# Notion 데이터소스 스키마

`mega-wiki.storage=notion` 모드에서는 Notion에 아래 두 개의 데이터소스가 필요합니다.

- `MegaWiki Pages`
- `MegaWiki Questions`

이제는 `NOTION_API_TOKEN`만 있어도 애플리케이션이 위 이름의 데이터소스를 자동 탐색하고, 필요한 스키마를 자동으로 보정합니다. 이미 data source id를 알고 있으면 환경 변수로 직접 지정해도 됩니다.

## 1. Pages 데이터소스

필수 속성:

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

## 2. Questions 데이터소스

필수 속성:

- `Name` : Title
- `Author` : Rich text
- `Channel` : Rich text
- `Question` : Rich text
- `AiAnswer` : Rich text
- `LinkedPageId` : Rich text
- `SourceType` : Select (`SLACK_THREAD`, `MEGAONE_ASK`, `MANUAL`)

## 환경 변수

필수:

- `NOTION_API_TOKEN`

선택:

- `NOTION_PAGES_DATA_SOURCE_ID`
- `NOTION_QUESTIONS_DATA_SOURCE_ID`
- `NOTION_PAGES_DATA_SOURCE_NAME`
- `NOTION_QUESTIONS_DATA_SOURCE_NAME`
- `NOTION_API_VERSION`

## 실행 예시

로컬 메모리 저장소를 쓰는 경우:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

실제 Notion 저장소를 쓰는 경우:

```powershell
$env:NOTION_API_TOKEN="your-token"
.\gradlew.bat bootRun
```

앱은 시작 시 Notion 데이터소스를 검색하고, 현재 코드가 기대하는 속성 구조를 자동으로 맞춥니다.
