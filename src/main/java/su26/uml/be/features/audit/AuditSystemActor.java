package su26.uml.be.features.audit;

/**
 * Actor cố định cho các thao tác do HỆ THỐNG thực hiện (job nền, không có admin/HTTP request).
 *
 * <p>Chỉ có {@link #NAME} (luôn non-null) để FE hiển thị cột "Người thực hiện" như log thường.
 * {@code actorId}/{@code actorEmail} của dòng hệ thống để null — không có principal thật, và
 * {@code actor_id} chỉ là cột mô tả (không bao giờ dùng cho xác thực/phân quyền). FE phân biệt
 * dòng hệ thống bằng: {@code actorName != null && actorId == null}.</p>
 */
public final class AuditSystemActor {

    private AuditSystemActor() {
    }

    /** Tên hiển thị ổn định cho actor hệ thống. */
    public static final String NAME = "UMLAdminSystem";
}
