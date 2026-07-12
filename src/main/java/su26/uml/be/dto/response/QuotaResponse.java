package su26.uml.be.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

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
}
