package su26.uml.be.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

/** Trạng thái subscription trả phí hiện tại của user (cho GET /me/subscription, cancel, reactivate). */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "MySubscriptionResponse")
public class MySubscriptionResponse {
    UUID planId;
    String planName;
    String status;
    LocalDateTime startDate;
    /** Ngày kết thúc kỳ — user vẫn dùng Premium tới đây, sau đó về gói base. */
    LocalDateTime endDate;
    /** True = đã hủy gia hạn (vẫn Premium tới endDate). */
    Boolean cancelAtPeriodEnd;
}
