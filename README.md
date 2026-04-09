# Mega-Wiki

Mega-Wiki is a Spring Boot backend that turns Slack questions into reusable team knowledge. A Slack app mention triggers the question flow, Gemini drafts the answer, and the result is stored in Notion through the existing repositories.

## What is implemented

- `POST /api/integrations/slack/events` for Slack Events API
- Slack request signature verification and duplicate event protection
- `app_mention` event processing with threaded Slack replies
- Gemini `generateContent` integration with rule-based fallback when Gemini is disabled or fails
- Notion-backed `KnowledgePageRepository` and `QuestionThreadRepository` reuse for durable storage
- Local memory profile for development without Notion or Gemini credentials

## Required environment variables

### Notion

- `NOTION_API_TOKEN`
- `NOTION_PAGES_DATA_SOURCE_ID`
- `NOTION_QUESTIONS_DATA_SOURCE_ID`

### Slack

- `SLACK_ENABLED=true`
- `SLACK_BOT_TOKEN`
- `SLACK_SIGNING_SECRET`
- `SLACK_BOT_USER_ID` optional for future mention-specific behavior

### Gemini

- `GEMINI_ENABLED=true`
- `GEMINI_API_KEY`
- `GEMINI_MODEL=gemini-2.5-flash` optional
- `GEMINI_TEMPERATURE=0.2` optional

## Slack app setup

1. Enable Event Subscriptions.
2. Set the Request URL to `/api/integrations/slack/events` on your deployed Mega-Wiki server.
3. Subscribe to the `app_mention` bot event.
4. Add bot scopes at least `app_mentions:read` and `chat:write`.
5. Install or reinstall the app to the workspace.

## Run locally

Local profile keeps storage in memory and disables external integrations.

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

To run the full integration profile, provide real Slack, Gemini, and Notion credentials and start without the local profile.

## Test

```powershell
.\gradlew.bat test --no-daemon
```

## API summary

- `GET /api/dashboard`
- `GET /api/pages`
- `GET /api/pages/{pageId}`
- `POST /api/pages`
- `POST /api/pages/{pageId}/contributions`
- `POST /api/pages/{pageId}/helpful`
- `POST /api/questions`
- `POST /api/integrations/slack/events`

## Notes

- Slack processing is acknowledged immediately and handled on a background task executor.
- If Gemini is disabled or its call fails, Mega-Wiki falls back to the existing rule-based answer generator.
- Notion remains the system of record for knowledge pages and archived question threads.
