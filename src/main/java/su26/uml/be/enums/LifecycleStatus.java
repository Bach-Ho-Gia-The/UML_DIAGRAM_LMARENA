package su26.uml.be.enums;

/**
 * Vòng đời của tài nguyên (Project/Sheet) cho Archive/Purge (thiết kế §2.4).
 * Lưu dạng {@code @Enumerated(EnumType.STRING)} vào cột VARCHAR(30) → cùng biểu diễn DB với String cũ.
 */
public enum LifecycleStatus {
    /** Đang hoạt động bình thường, tính vào capacity. */
    ACTIVE,
    /** Bị archive do vượt limit gói (read-only 30 ngày → purge). */
    ARCHIVED_OVER_LIMIT,
    /** User tự xóa vào thùng rác. */
    TRASHED_BY_USER,
    /** Đã purge vĩnh viễn. */
    PURGED
}
