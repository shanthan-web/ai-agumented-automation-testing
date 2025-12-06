# API Tests

Minimal API test module that reuses shared models from `ai-service` and reports failures to the AI analysis endpoint.

## Structure
- `src/test/java/com/shanthan/ai/api/base/ApiBaseTest.java` — OkHttp helpers and captured request/response context.
- `src/test/java/com/shanthan/ai/api/tests/UserApiTest.java` — intentional failing test to exercise AI triage.
- `testng.xml` — wires the shared UI listener (`com.shanthan.ai.ui.listener.AiFailureListener`) and test class.

## Running
```
cd api-tests
mvn test -Dapi.baseUrl=http://localhost:8080 -Dai.service.url=http://localhost:8085
```

`ai.service.url` points to your running Spring AI service; `api.baseUrl` is the API under test. Both default to localhost.
