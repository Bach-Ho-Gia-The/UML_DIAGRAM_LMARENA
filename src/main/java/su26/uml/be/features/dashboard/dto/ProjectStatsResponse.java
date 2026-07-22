package su26.uml.be.features.dashboard.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/** Thống kê dự án toàn hệ thống (Admin) — đếm trên toàn bảng, độc lập với phân trang. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectStatsResponse {
    long total;          // tổng dự án chưa xóa
    long onTrack;        // đúng tiến độ = không phải bản nháp
    long needAttention;  // cần chú ý (chưa có tiêu chí nghiệp vụ → 0)
    long drafts;         // bản nháp
}