# AI-Augmented Automation Testing

This project shows, in simple terms, how to let an AI help you understand why UI tests fail. There are only two moving parts:
- `ai-service`: a small Spring Boot web service that calls the OpenAI API to turn raw failure data into a short, human-friendly analysis.
- `ui-tests`: a Selenium + TestNG sample test that will fail on purpose, then send the failure details to the AI service and print the AI’s advice in the test report.

## How it works
1. Run the AI service on your machine (defaults to port 8085).
2. Run the sample UI test. It fails by design.
3. A TestNG listener (`com.shanthan.ai.ui.listener.AiFailureListener`) grabs the failure data and POSTs it to the AI service at `/api/ai/analyze-failure`.
4. The AI service asks OpenAI for a short analysis: what kind of failure it is, likely cause, what to try next, and a ready-to-use Jira summary. The response is echoed in the Maven/TestNG report (`ui-tests/target/surefire-reports/emailable-report.html`).

## Prerequisites
- Java 17 and Maven 3.9+ installed.
- Google Chrome and a compatible ChromeDriver on your PATH (the tests launch `ChromeDriver` directly).
- An OpenAI API key in `OPENAI_API_KEY` (optional but recommended; without it the service will return a “key not configured” message).

## Quick start
### 1) Start the AI service
From the project root:
```
mvn -pl ai-service spring-boot:run
```
Notes:
- Default port: `8085` (change with `-Dserver.port=9090` or `SERVER_PORT=9090`).
- OpenAI settings can be overridden with `-Dopenai.apiKey=...`, `-Dopenai.baseUrl=...`, `-Dopenai.model=...`.

### 2) Run the sample UI test
In a second terminal, from the project root:
```
mvn -pl ui-tests test -Dai.service.url=http://localhost:8085
```
What to expect:
- The login test intentionally fails on a fake locator.
- The AI analysis is printed in the test output and is also visible in `ui-tests/target/surefire-reports/emailable-report.html`.

## API quick reference
- **Endpoint:** `POST /api/ai/analyze-failure`
- **Port:** `8085` by default
- **Request body (JSON):**
```json
{
  "testName": "Login_failure_with_invalid_credentials",
  "suiteName": "AI-Augmented-UI-Suite",
  "failureMessage": "no such element: Unable to locate element: #fake-username-field",
  "stackTrace": "Full stack trace text...",
  "feature": "Login",
  "environment": "local",
  "tags": ["ui", "login"],
  "rawLogSnippet": "Last URL: <not captured>"
}
```
- **Response body (JSON):**
```json
{
  "classifiedFailureType": "LOCATOR_ISSUE",
  "rootCauseSummary": "The username field locator does not exist on the page.",
  "recommendedNextSteps": "Open the page, inspect the real locator, and update the test.",
  "severityScore": 2,
  "jiraSummaryTemplate": "[Login] Fix locator for username input",
  "similarFailures": [
    {
      "id": "WF-101",
      "shortDescription": "Login button click fails on slow/staging environment",
      "suspectedRootCause": "Element not interactable due to missing explicit wait",
      "link": "https://your-jira-or-ado-instance/browse/WF-101"
    }
  ]
}
```

## Project layout
- `ai-service/pom.xml` — Spring Boot service that calls OpenAI and exposes `/api/ai/analyze-failure`.
- `ai-service/src/main/resources/application.yml` — Default port and OpenAI settings.
- `ui-tests/src/test/java/com/shanthan/ai/ui/tests/LoginTest.java` — Sample failing test.
- `ui-tests/src/test/java/com/shanthan/ai/ui/listener/AiFailureListener.java` — Sends failures to the AI service and logs the AI response.
- `ui-tests/testng.xml` — Runs the login test with the AI listener.

## What the code actually does
- `ai-service` (Spring Boot):
  - `AiServiceApplication` boots the web server (default port `8085`).
  - `FailureAnalysisController` exposes `POST /api/ai/analyze-failure`.
  - `FailureAnalysisService` collects failure info, finds seeded “similar failures,” crafts a prompt, and asks OpenAI for a JSON-formatted analysis (type, root cause, next steps, severity, Jira summary).
  - `SimilarityStore` returns two sample past failures to add context.
  - `OpenAiClient` wraps the chat-completions call; if no API key is set it returns a helpful message instead of failing.
- `ui-tests` (Selenium + TestNG):
  - `BaseTest` starts a local `ChromeDriver` and maximizes the window.
  - `LoginTest` deliberately uses fake locators so the test fails.
  - `AiFailureListener` (TestNG listener) captures failure details, adds tags/env/feature info, and POSTs them to the AI service. The AI response is echoed into the TestNG/Maven report.
  - Reports live under `ui-tests/target/surefire-reports/` after a test run.

## Config knobs you can set
- `OPENAI_API_KEY` or `-Dopenai.apiKey=...` — API key for OpenAI.
- `-Dopenai.baseUrl=...` — Override the OpenAI endpoint.
- `-Dopenai.model=...` — Override the model (defaults to `gpt-4.1-mini` in `application.yml`).
- `-Dserver.port=9090` — Change the AI service port.
- `-Dai.service.url=http://localhost:8085` — Where the UI tests send failure payloads.

## Handy commands
- Run everything (from repo root): `mvn test`
- Run only the AI service: `mvn -pl ai-service spring-boot:run`
- Run only the UI tests: `mvn -pl ui-tests test -Dai.service.url=http://localhost:8085`

## Troubleshooting
- ChromeDriver errors: ensure ChromeDriver matches your Chrome version and is on the PATH.
- AI service says “API key not configured”: set `OPENAI_API_KEY` or pass `-Dopenai.apiKey=your-key` when starting the service.
- Nothing printed from the AI: confirm the service is running on the same port you passed with `-Dai.service.url`.
