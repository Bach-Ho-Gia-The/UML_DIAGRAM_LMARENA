package su26.uml.be.features.subscription.dto;

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

    // ─── Booked downgrade (hướng A) ───
    /** Gói sẽ chuyển sang khi hết kỳ (hạ cấp đã đặt). Không thu tiền ngay. */
    UUID pendingPlanId;

    /** Tên gói sẽ chuyển sang — FE hiện banner "Ngày X bạn sẽ chuyển sang gói Y". */
    String pendingPlanName;

    /** Giá gói sẽ chuyển sang (null nếu gói báo giá). */
    java.math.BigDecimal pendingPrice;

    /** Thời điểm pendingPlanId có hiệu lực (= endDate kỳ hiện tại). */
    LocalDateTime pendingEffectiveAt;
}