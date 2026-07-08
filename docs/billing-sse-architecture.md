# Billing & SSE Architecture

## 1. Tổng quan luồng

```
User chat → DiagramChatServiceImpl.chat()
  → UmlArchitect.chat() → LangChain4j → AnythingLLM → LLM Provider
  → Response<AiMessage> chứa TokenUsage (inputTokenCount, outputTokenCount)
  → Parse text thành DiagramChatResponse → trả về client NGAY (không block)
  → AiBillingEventPublisher.publishBill() (@Async)
      → Đọc SystemConfig cache → modelName + provider
      → Đọc ProviderRate cache → rateInPer1k + rateOutPer1k
      → Tính costUsd = (input/1000 * rateIn) + (output/1000 * rateOut)
      → Save AiGenerationLog (immutable financial record)
```

## 2. Các file liên quan

| File | Chức năng |
|---|---|
| `entity/AiGenerationLog.java` | Entity immutable log (cost_usd DECIMAL 18,12) |
| `entity/ProviderRate.java` | Entity lưu rate theo model (input/output per 1K tokens) |
| `enums/EstimationMethod.java` | PROVIDER / JTOKKIT / CHARS4 / UNKNOWN |
| `repository/AiGenerationLogRepository.java` | SUM cost, count, avg latency queries |
| `repository/ProviderRateRepository.java` | CRUD + lookup by provider+model |
| `service/UmlArchitect.java` | LangChain4j AI Service (return Response<AiMessage>) |
| `service/Impl/DiagramChatServiceImpl.java` | Chat flow: gọi AI → parse → fire async billing |
| `service/Impl/AiBillingEventPublisher.java` | @Async: tính cost từ cache → save log |
| `service/SystemConfigCacheService.java` | Cache Redis cho model hiện tại |
| `service/Impl/SystemConfigCacheServiceImpl.java` | Sync từ AnythingLLM `/v1/system` |
| `service/ProviderRateCacheService.java` | Cache Redis cho provider rates |
| `service/Impl/ProviderRateCacheServiceImpl.java` | Load từ DB vào Redis với TTL 1h |
| `service/Impl/OpenRouterSyncService.java` | Cronjob 00:00 sync OpenRouter → UPSERT provider_rates |
| `service/SseService.java` | Quản lý SseEmitter, broadcast event |
| `service/Impl/SseServiceImpl.java` | CopyOnWriteArrayList emitters, timeout 30ph |
| `controller/SseController.java` | GET /admin/dashboard/events (SSE endpoint) |

## 3. Token extraction priority

1. **PROVIDER** — LangChain4j `Response.tokenUsage()` (nếu không null)
2. **JTOKKIT** — `com.knuddels.jtokkit` estimate tokens (fallback khi tokenUsage null)
3. **CHARS4** — `text.length() / 4` (fallback khi jtokkit fail)

## 4. Redis cache keys

| Key | Value | TTL |
|---|---|---|
| `system_config:active_model` | JSON `{modelName, provider, workspaceModelName}` | 1h |
| `provider_rate:{provider}:{modelName}` | JSON của entity `ProviderRate` | 1h |

## 5. SSE events

| Event name | Trigger | Payload |
|---|---|---|
| `connected` | Client kết nối SSE | `"SSE connection established"` |
| `openrouter_sync` | Cronjob OpenRouterSyncService | `{event, summary, modelsChecked, modelsUpdated}` |
| `message` | `SseService.broadcast(Map)` | Generic payload |

## 6. DB Migration

```sql
-- Plan.price Double → BigDecimal (chạy 1 lần)
ALTER TABLE plans ALTER COLUMN price TYPE NUMERIC(10,2) USING price::numeric(10,2);
```

## 7. .env bổ sung

```env
OPENROUTER_API_KEY=sk-or-v1-...
```
