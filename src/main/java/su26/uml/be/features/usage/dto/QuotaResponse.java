package su26.uml.be.features.usage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "QuotaResponse", description = "Trạng thái quota AI Request của user trong kỳ hiện tại.")
public class QuotaResponse {

    @Schema(description = "Số AI request đã dùng trong kỳ.", example = "10")
    int used;

    @Schema(description = "Hạn mức AI request của gói; -1 = không giới hạn.", example = "50")
    int limit;

    @Schema(description = "Thời điểm quota reset về 0.", example = "2026-08-11T00:00:00")
    LocalDateTime resetAt;

    @Schema(description = "Hạn mức danh nghĩa của gói cho 1 kỳ đầy đủ.", example = "600")
    Integer nominalLimit;

    @Schema(description = "Hạn mức thực tế kỳ hiện tại (sau proration nếu upgrade giữa kỳ).", example = "1050")
    Integer effectiveLimit;

    @Schema(description = "Thời điểm bắt đầu kỳ quota.", example = "2026-07-01T00:00:00")
    LocalDateTime periodStart;

    @Schema(description = "Thời điểm kết thúc kỳ quota.", example = "2026-07-31T23:59:59")
    LocalDateTime periodEnd;

    @Schema(description = "ID của plan đang áp dụng cho quota.", example = "uuid")
    UUID planId;

    @Schema(description = "ID của subscription (null nếu đang dùng base plan).", example = "uuid")
    UUID subscriptionId;
}