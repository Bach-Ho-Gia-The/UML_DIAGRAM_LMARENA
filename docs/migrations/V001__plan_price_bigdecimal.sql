-- Migration V001: Plan.price DOUBLE → NUMERIC(10,2)
-- Chạy script này một lần để chuyển đổi an toàn, không mất dữ liệu.
-- Hibernate ddl-auto=update sẽ tự tạo cột mới cho ProviderRate, AiGenerationLog, Notification.

ALTER TABLE plans ALTER COLUMN price TYPE NUMERIC(10,2) USING price::numeric(10,2);
