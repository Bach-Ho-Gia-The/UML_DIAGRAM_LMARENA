package su26.uml.be.features.audit;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kênh phụ (ThreadLocal) để service làm giàu bản ghi audit của lời gọi hiện tại — vẫn giữ
 * {@code AuditAspect} generic (aspect không cần biết chi tiết từng nghiệp vụ).
 *
 * <p>Dùng khi annotation tĩnh không đủ:
 * <ul>
 *   <li>{@link #setAction(String)} — ghi đè action động (vd. toggle khóa/mở →
 *       {@code USER_LOCK}/{@code USER_UNLOCK}).</li>
 *   <li>{@link #setTargetId(Object)} — đặt id đối tượng khi nó chỉ có sau khi thực thi
 *       (vd. user vừa được tạo).</li>
 *   <li>{@link #putBeforeAfter(Object, Object)} / {@link #putDetail(String, Object)} — nhồi
 *       detail JSON.</li>
 * </ul>
 *
 * <p>{@code AuditAspect} luôn {@link #clear()} sau mỗi lời gọi để tránh rò rỉ giữa các request
 * (thread pool tái sử dụng).</p>
 */
public final class AuditContext {

    private static final ThreadLocal<Holder> HOLDER = new ThreadLocal<>();

    private AuditContext() {
    }

    private static Holder holder() {
        Holder holder = HOLDER.get();
        if (holder == null) {
            holder = new Holder();
            HOLDER.set(holder);
        }
        return holder;
    }

    /** Ghi đè tên action mặc định của {@code @Auditable}. */
    public static void setAction(String action) {
        holder().action = action;
    }

    /** Đặt target id (chuyển sang String); bỏ qua nếu null. */
    public static void setTargetId(Object targetId) {
        holder().targetId = targetId == null ? null : targetId.toString();
    }

    /** Thêm một cặp key/value vào detail JSON. */
    public static void putDetail(String key, Object value) {
        holder().detail.put(key, value);
    }

    /** Tiện ích cho detail dạng {before, after}. */
    public static void putBeforeAfter(Object before, Object after) {
        putDetail("before", before);
        putDetail("after", after);
    }

    /** Aspect đọc trạng thái đã tích lũy (null nếu service không set gì). */
    public static Holder current() {
        return HOLDER.get();
    }

    /** Dọn ThreadLocal — bắt buộc gọi ở cuối mỗi lời gọi. */
    public static void clear() {
        HOLDER.remove();
    }

    /** Bộ giữ trạng thái nội bộ. */
    public static final class Holder {
        private String action;
        private String targetId;
        private final Map<String, Object> detail = new LinkedHashMap<>();

        public String getAction() {
            return action;
        }

        public String getTargetId() {
            return targetId;
        }

        public Map<String, Object> getDetail() {
            return detail;
        }
    }
}
