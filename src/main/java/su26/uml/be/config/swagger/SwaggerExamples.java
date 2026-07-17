package su26.uml.be.config.swagger;

public final class SwaggerExamples {
    private SwaggerExamples() {}

    // ─── Auth ───────────────────────────────────────────
    public static final String LOGIN_REQUEST =
            "{\"email\": \"johndoe@example.com\", \"password\": \"Passw0rd\"}";

    public static final String LOGIN_RESPONSE =
            "{\"code\":200,\"result\":{\"token\":\"eyJhbGciOiJIUzUxMiJ9...\",\"authenticated\":true,\"refreshToken\":\"eyJhbGciOiJIUzUxMiJ9...\"}}";

    public static final String REFRESH_REQUEST =
            "{\"token\": \"eyJhbGciOiJIUzUxMiJ9...\"}";

    public static final String REFRESH_RESPONSE =
            "{\"code\":200,\"result\":{\"token\":\"eyJhbGciOiJIUzUxMiJ9...\",\"refreshToken\":\"eyJhbGciOiJIUzUxMiJ9...\",\"authenticated\":true}}";

    public static final String INTROSPECT_REQUEST =
            "{\"token\": \"eyJhbGciOiJIUzUxMiJ9...\"}";

    public static final String INTROSPECT_RESPONSE =
            "{\"code\":200,\"result\":{\"valid\":true}}";

    public static final String LOGOUT_REQUEST =
            "{\"token\": \"eyJhbGciOiJIUzUxMiJ9...\"}";

    public static final String LOGOUT_RESPONSE =
            "{\"code\":0}";

    public static final String ACCOUNT_STATUS_RESPONSE =
            "{\"code\":0,\"result\":{\"locked\":false}}";

    // ─── Users (paginated) ──────────────────────────────
    public static final String GET_USERS_PAGED_RESPONSE = """
            {
              "code": 200,
              "message": "Lấy danh sách người dùng thành công",
              "result": {
                "content": [
                  {
                    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                    "username": "johndoe@example.com",
                    "fullName": "John Doe",
                    "email": "johndoe@example.com",
                    "status": "ACTIVE",
                    "role": "USER"
                  }
                ],
                "page": 0,
                "size": 20,
                "totalElements": 1,
                "totalPages": 1
              }
            }""";

    // ─── Admin ──────────────────────────────────────────
    public static final String ADMIN_REGISTER_REQUEST = """
            {
              "password": "Passw0rd",
              "fullName": "Admin User",
              "email": "admin@gmail.com",
              "phone": "0901234567"
            }""";

    public static final String ADMIN_REGISTER_RESPONSE = """
            {
              "code": 200,
              "message": "Tạo tài khoản Admin thành công",
              "result": {
                "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                "username": "admin@gmail.com",
                "fullName": "Admin User",
                "email": "admin@gmail.com",
                "phone": "0901234567",
                "status": "ACTIVE",
                "role": {
                  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "roleName": "ADMIN",
                  "description": "System administrator"
                }
              }
            }""";

    // ─── Users ──────────────────────────────────────────
    public static final String REGISTER_REQUEST = """
            {
              "password": "Passw0rd",
              "fullName": "John Doe",
              "email": "john.doe@gmail.com",
              "phone": "0901234567"
            }""";

    public static final String DEACTIVATE_ACCOUNT_RESPONSE = """
            {
              "code": 200,
              "message": "Yêu cầu xóa tài khoản đã được ghi nhận",
              "result": {
                "status": "PENDING_DELETE",
                "deletionDate": "2026-07-17T00:00:00",
                "daysRemaining": 30,
                "message": "Tài khoản sẽ bị xóa vĩnh viễn sau 30 ngày. Bạn có thể khôi phục trước thời hạn này."
              }
            }""";

    public static final String RESTORE_ACCOUNT_RESPONSE = """
            {
              "code": 200,
              "message": "Tài khoản đã được khôi phục thành công",
              "result": {
                "status": "ACTIVE",
                "message": "Tài khoản của bạn đã được khôi phục và hoạt động bình thường."
              }
            }""";

    // ─── Change password (authenticated, OTP) ──────────
    public static final String CHANGE_PASSWORD_INIT_REQUEST = """
            {
              "currentPassword": "OldPass@123"
            }""";

    public static final String CHANGE_PASSWORD_INIT_RESPONSE = """
            {
              "code": 200,
              "message": "Mã OTP đổi mật khẩu đã được gửi đến email của bạn",
              "result": null
            }""";

    public static final String CHANGE_PASSWORD_CONFIRM_REQUEST = """
            {
              "otpCode": "123456",
              "newPassword": "NewPass@123",
              "confirmPassword": "NewPass@123"
            }""";

    public static final String CHANGE_PASSWORD_CONFIRM_RESPONSE = """
            {
              "code": 200,
              "message": "Đổi mật khẩu thành công",
              "result": null
            }""";

    public static final String UPDATE_USER_REQUEST = """
            {
              "fullName": "Nguyen Van B",
              "phone": "0909999999",
              "dob": "2000-01-15"
            }""";

    public static final String UPDATE_USER_RESPONSE = """
            {
              "code": 200,
              "message": "Cập nhật thông tin thành công",
              "result": {
                "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                "username": "john.doe@gmail.com",
                "fullName": "Nguyen Van B",
                "email": "john.doe@gmail.com",
                "phone": "0909999999",
                "dob": "2000-01-15",
                "status": "ACTIVE",
                "role": {
                  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "roleName": "USER",
                  "description": "Standard application user"
                }
              }
            }""";

    public static final String REGISTER_RESPONSE = """
            {
              "code": 200,
              "message": "Đăng ký thành công",
              "result": {
                "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                "username": "johndoe",
                "fullName": "John Doe",
                "email": "john.doe@gmail.com",
                "phone": "0901234567",
                "status": "ACTIVE",
                "role": {
                  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "roleName": "USER",
                  "description": "Standard application user"
                }
              }
            }""";

    public static final String COMPLETE_PROFILE_REQUEST = """
            {
              "fullName": "Nguyen Van A",
              "phone": "0901234567",
              "dob": "2000-01-15",
              "password": "Passw0rd!",
              "confirmPassword": "Passw0rd!"
            }""";

    public static final String COMPLETE_PROFILE_RESPONSE = """
            {
              "code": 200,
              "message": "Đã lưu thông tin, email xác nhận đã được gửi",
              "result": {
                "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                "username": "nguyenvana",
                "fullName": "Nguyen Van A",
                "email": "nguyenvana@gmail.com",
                "phone": "0901234567",
                "dob": "2000-01-15",
                "status": "ACTIVE",
                "role": {
                  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "roleName": "USER",
                  "description": "Standard application user"
                }
              }
            }""";

    // ─── Files / Storage ────────────────────────────────
    public static final String UPLOAD_AVATAR_RESPONSE = """
            {
              "code": 200,
              "message": "Tải ảnh đại diện thành công",
              "result": {
                "bucket": "avatars",
                "path": "3fa85f64-5717-4562-b3fc-2c963f66afa6/9f1c2a3b-_avatar.png",
                "url": "https://xyz.supabase.co/storage/v1/object/public/avatars/3fa85f64-5717-4562-b3fc-2c963f66afa6/9f1c2a3b-_avatar.png"
              }
            }""";

    public static final String UPLOAD_DOCUMENT_RESPONSE = """
            {
              "code": 200,
              "message": "Tải tài liệu thành công",
              "result": {
                "bucket": "documents",
                "path": "3fa85f64-5717-4562-b3fc-2c963f66afa6/4d2e1f0a-_report.pdf",
                "url": null
              }
            }""";

    public static final String SIGNED_URL_RESPONSE = """
            {
              "code": 200,
              "message": "Tạo đường dẫn truy cập thành công",
              "result": {
                "signedUrl": "https://xyz.supabase.co/storage/v1/object/sign/documents/3fa85f64-5717-4562-b3fc-2c963f66afa6/4d2e1f0a-_report.pdf?token=eyJ...",
                "expiresInSeconds": 3600
              }
            }""";

    // ─── Projects ───────────────────────────────────────
    public static final String PROJECT_REQUEST = """
            {
              "projectName": "My UML Project",
              "description": "Class diagram for the order module"
            }""";

    public static final String PROJECT_RESPONSE = """
            {
              "code": 200,
              "message": "Tạo dự án thành công",
              "result": {
                "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                "projectName": "My UML Project",
                "description": "Class diagram for the order module",
                "userId": "9f1c2a3b-5717-4562-b3fc-2c963f66afa6",
                "sheets": [
                  {
                    "id": "4fa85f64-5717-4562-b3fc-2c963f66afa7",
                    "name": "Sheet 1",
                    "orderIndex": 0,
                    "diagramData": "{\\"nodes\\": [], \\"edges\\": []}",
                    "projectId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
                  }
                ],
                "createdAt": "2026-06-22T10:15:30",
                "updatedAt": "2026-06-22T10:15:30"
              }
            }""";

    public static final String PROJECT_LIST_RESPONSE = """
            {
              "code": 200,
              "message": "Lấy danh sách dự án thành công",
              "result": [
                {
                  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "projectName": "My UML Project",
                  "description": "Class diagram for the order module",
                  "userId": "9f1c2a3b-5717-4562-b3fc-2c963f66afa6",
                  "sheets": [],
                  "createdAt": "2026-06-22T10:15:30",
                  "updatedAt": "2026-06-22T10:15:30"
                }
              ]
            }""";

    public static final String DELETE_PROJECT_REQUEST = """
            {
              "ids": [
                "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                "9f1c2a3b-5717-4562-b3fc-2c963f66afa6"
              ]
            }""";

    public static final String DELETE_PROJECT_RESPONSE = """
            {
              "code": 200,
              "message": "Xóa các dự án thành công"
            }""";

    // ─── Workspace Items (project file tree) ──────────────────────────────
    public static final String WORKSPACE_ITEM_RESPONSE = """
            {
              "code": 200,
              "message": "Tạo mục workspace thành công",
              "result": {
                "id": "6fa85f64-5717-4562-b3fc-2c963f66afa1",
                "projectId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                "parentId": null,
                "name": "Checkout Sequence",
                "kind": "DIAGRAM",
                "orderIndex": 0,
                "sheetId": "4fa85f64-5717-4562-b3fc-2c963f66afa7",
                "diagramType": "sequence",
                "version": 0,
                "createdAt": "2026-07-17T10:00:00",
                "updatedAt": "2026-07-17T10:00:00"
              }
            }""";

    public static final String WORKSPACE_ITEM_LIST_RESPONSE = """
            {
              "code": 200,
              "message": "Lấy danh sách mục workspace thành công",
              "result": [
                {
                  "id": "5fa85f64-5717-4562-b3fc-2c963f66afa0",
                  "projectId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "parentId": null,
                  "name": "Requirements",
                  "kind": "FOLDER",
                  "orderIndex": 0,
                  "version": 0,
                  "createdAt": "2026-07-17T10:00:00",
                  "updatedAt": "2026-07-17T10:00:00"
                },
                {
                  "id": "7fa85f64-5717-4562-b3fc-2c963f66afa2",
                  "projectId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "parentId": "5fa85f64-5717-4562-b3fc-2c963f66afa0",
                  "name": "requirements.md",
                  "kind": "MARKDOWN",
                  "orderIndex": 0,
                  "content": "# Requirements",
                  "version": 2,
                  "createdAt": "2026-07-17T10:00:00",
                  "updatedAt": "2026-07-17T10:05:00"
                }
              ]
            }""";

    public static final String DELETE_WORKSPACE_ITEMS_RESPONSE = """
            {
              "code": 200,
              "message": "Xóa các mục workspace thành công"
            }""";

    // ─── Diagram Versions ──────────────────────────────────────────────────
    public static final String DIAGRAM_VERSION_RESPONSE = """
            {
              "code": 200,
              "message": "Tạo phiên bản sơ đồ thành công",
              "result": {
                "id": "8fa85f64-5717-4562-b3fc-2c963f66afa3",
                "sheetId": "4fa85f64-5717-4562-b3fc-2c963f66afa7",
                "versionNumber": 5,
                "name": "Before payment refactor",
                "note": "Stable checkout workflow",
                "source": "MANUAL",
                "diagramData": "{\\"schemaVersion\\":1,\\"diagramType\\":\\"sequence\\",\\"nodes\\":[],\\"edges\\":[]}",
                "contentHash": "9b74c9897bac770ffc029102a200c5de",
                "schemaVersion": 1,
                "createdAt": "2026-07-17T10:00:00"
              }
            }""";

    public static final String DIAGRAM_VERSION_LIST_RESPONSE = """
            {
              "code": 200,
              "message": "Lấy danh sách phiên bản sơ đồ thành công",
              "result": [
                {
                  "id": "8fa85f64-5717-4562-b3fc-2c963f66afa3",
                  "sheetId": "4fa85f64-5717-4562-b3fc-2c963f66afa7",
                  "versionNumber": 5,
                  "name": "Before payment refactor",
                  "source": "MANUAL",
                  "contentHash": "9b74c9897bac770ffc029102a200c5de",
                  "schemaVersion": 1,
                  "createdAt": "2026-07-17T10:00:00"
                },
                {
                  "id": "7fa85f64-5717-4562-b3fc-2c963f66afa9",
                  "sheetId": "4fa85f64-5717-4562-b3fc-2c963f66afa7",
                  "versionNumber": 4,
                  "name": "Auto checkpoint",
                  "source": "AUTO",
                  "contentHash": "1a74c9897bac770ffc029102a200c111",
                  "schemaVersion": 1,
                  "createdAt": "2026-07-17T09:45:00"
                }
              ]
            }""";

    // ─── Audit Log ──────────────────────────────────────
    /**
     * Danh mục action được ghi log — dùng trong mô tả Swagger. Dropdown lọc nên lấy ĐỘNG từ
     * GET /admin/audit-logs/actions (không hardcode). Action "reserved" sẽ TỰ ĐỘNG vào log khi
     * endpoint tương ứng ra đời (chỉ cần thêm @Auditable) — không phải đổi bảng/API audit.
     */
    public static final String AUDIT_ACTIONS_CATALOG = """
            Các action ĐANG được ghi log tự động:
            • USER_LOCK, USER_UNLOCK — target USER (detail: {before, after})
            • ADMIN_CREATE — target USER (detail: {email, role})
            • AI_CONFIG_UPDATE — target AI_CONFIG
            • AI_WORKSPACE_CREATE / AI_WORKSPACE_UPDATE / AI_WORKSPACE_DELETE — target AI_WORKSPACE
            • AI_DOCUMENT_UPLOAD / AI_DOCUMENT_DELETE / AI_DOCUMENT_REEMBED — target AI_DOCUMENT
            • ACCOUNT_AUTO_DELETE — target USER; actor HỆ THỐNG (actorName='UMLAdminSystem', actorId=null)

            Reserved — CHƯA kích hoạt (còn mock ở FE, chưa có endpoint). Khi có endpoint thật, chỉ cần
            thêm @Auditable là action tự xuất hiện trong DB + dropdown; không đổi bảng/API audit:
            • SUBSCRIPTION_CREATE / SUBSCRIPTION_UPDATE / SUBSCRIPTION_DELETE — target SUBSCRIPTION
            • TEMPLATE_CREATE / TEMPLATE_UPDATE / TEMPLATE_DELETE — target TEMPLATE""";

    public static final String AUDIT_LOGS_RESPONSE = """
            {
              "code": 200,
              "message": "Lấy nhật ký thao tác thành công",
              "result": {
                "content": [
                  {
                    "id": 1024,
                    "actorId": "3f1e9c2a-8b7d-4a11-9f0e-2c6d5b4a3e21",
                    "actorName": "Nguyễn Minh Anh",
                    "actorEmail": "admin@uml.studio",
                    "action": "USER_LOCK",
                    "targetType": "USER",
                    "targetId": "42",
                    "detail": { "before": "ACTIVE", "after": "LOCKED" },
                    "createdAt": "2026-07-01T14:32:10Z"
                  },
                  {
                    "id": 1023,
                    "actorName": "UMLAdminSystem",
                    "action": "ACCOUNT_AUTO_DELETE",
                    "targetType": "USER",
                    "targetId": "9f1c2a3b-5717-4562-b3fc-2c963f66afa6",
                    "detail": { "email": "olduser@gmail.com", "deletionDate": "2026-06-01T00:00:00" },
                    "createdAt": "2026-07-01T00:00:00Z"
                  }
                ],
                "page": 0,
                "size": 20,
                "totalElements": 1024,
                "totalPages": 52
              }
            }""";

    public static final String AUDIT_ACTIONS_RESPONSE = """
            {
              "code": 200,
              "message": "Lấy danh sách action thành công",
              "result": [
                "ACCOUNT_AUTO_DELETE",
                "ADMIN_CREATE",
                "AI_CONFIG_UPDATE",
                "AI_DOCUMENT_DELETE",
                "AI_DOCUMENT_REEMBED",
                "AI_DOCUMENT_UPLOAD",
                "AI_WORKSPACE_CREATE",
                "AI_WORKSPACE_DELETE",
                "AI_WORKSPACE_UPDATE",
                "USER_LOCK",
                "USER_UNLOCK"
              ]
            }""";
}