package su26.uml.be.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST(1000, "Yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_KEY(1001, "Invalid message key", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(401, "Bạn chưa được xác thực", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(403, "Bạn không có quyền truy cập", HttpStatus.FORBIDDEN),

    //USER ERRORS
    USER_EXISTED(1002, "Username đã được sử dụng, hãy sử dụng username khác!", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED(1003, "Email đã được sử dụng, hãy sử dụng email khác", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1004, "Tên người dùng phải có ít nhất 3 ký tự", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1005, "Mật khẩu phải có ít nhất 6 ký tự, bao gồm chữ cái và số", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1006, "Tên đăng nhập hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(1007, "Tên đăng nhập hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    USERNAME_REQUIRED(1008, "Username không được để trống", HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED(1009, "Password không được để trống", HttpStatus.BAD_REQUEST),
    FULLNAME_REQUIRED(1010, "Họ tên không được để trống", HttpStatus.BAD_REQUEST),
    EMAIL_REQUIRED(1011, "Email không được để trống", HttpStatus.BAD_REQUEST),
    PHONE_REQUIRED(1012, "Số điện thoại không được để trống", HttpStatus.BAD_REQUEST),
    ROLE_ID_REQUIRED(1013, "Role ID không được để trống", HttpStatus.BAD_REQUEST),
    INVALID_PHONE(1014, "Số điện thoại phải có 10-11 chữ số", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL_FORMAT(1015, "Email không đúng định dạng (ví dụ: 112@gmail.com)", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1016, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    INVALID_FULLNAME(1017, "Họ tên không được vượt quá 255 ký tự", HttpStatus.BAD_REQUEST),
    INVALID_AVATAR_URL(1028, "Đường dẫn ảnh đại diện không được vượt quá 500 ký tự", HttpStatus.BAD_REQUEST),
    CANNOT_MODIFY_SELF(1029, "Bạn không thể thay đổi vai trò hoặc trạng thái của chính mình", HttpStatus.BAD_REQUEST),
    INVALID_STATUS(1030, "Trạng thái tài khoản không hợp lệ", HttpStatus.BAD_REQUEST),
    USER_LIST_EMPTY(1018, "Không có tài khoản nào trong hệ thống!!!", HttpStatus.NOT_FOUND),
    INVALID_DOB(1025, "Ngày sinh không hợp lệ, phải là ngày trong quá khứ", HttpStatus.BAD_REQUEST),
    ACCOUNT_ALREADY_PENDING_DELETE(1026, "Tài khoản đã trong trạng thái chờ xóa", HttpStatus.BAD_REQUEST),
    ACCOUNT_NOT_PENDING_DELETE(1027, "Tài khoản không trong trạng thái chờ xóa", HttpStatus.BAD_REQUEST),

    // ROLE ERRORS
    ROLE_NOT_FOUND(1019, "Role không tồn tại", HttpStatus.NOT_FOUND),
    DEFAULT_ROLE_NOT_FOUND(1020, "Không tìm thấy role mặc định", HttpStatus.NOT_FOUND),
    USER_INACTIVE(1021, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên để mở lại.", HttpStatus.FORBIDDEN),

    DELETE_ADMIN_INVALID(1022, "Không thể xóa tài khoản Quản trị viên hệ thống", HttpStatus.BAD_REQUEST),
    DELETE_SELF_INVALID(1023, "Bạn không thể tự khóa tài khoản của chính mình!", HttpStatus.BAD_REQUEST),
    DELETE_OTHER_ADMIN_INVALID(1024, "Bạn không thể xóa tài khoản Quản trị viên khác!", HttpStatus.BAD_REQUEST),

    //reset password
    EMAIL_NOT_FOUND(1057, "Email không tồn tại trong hệ thống", HttpStatus.NOT_FOUND),
    OTP_REQUIRED(1058, "Mã OTP không được để trống", HttpStatus.BAD_REQUEST),
    INVALID_OTP(1059, "Mã OTP phải có 6 chữ số", HttpStatus.BAD_REQUEST),
    OTP_NOT_FOUND(1060, "Mã OTP không hợp lệ hoặc đã hết hạn", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(1061, "Mã OTP đã hết hạn", HttpStatus.BAD_REQUEST),
    OTP_ALREADY_USED(1062, "Mã OTP đã được sử dụng", HttpStatus.BAD_REQUEST),
    INVALID_OTP_OR_EMAIL(1063, "Mã OTP hoặc email không chính xác", HttpStatus.BAD_REQUEST),
    TOKEN_NOT_FOUND(1064, "Token không hợp lệ", HttpStatus.BAD_REQUEST),
    PASSWORDS_NOT_MATCH(1065, "Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST),
    CONFIRM_PASSWORD_REQUIRED(1066, "Vui lòng xác nhận mật khẩu", HttpStatus.BAD_REQUEST),

    CHAT_SESSION_ID_REQUIRED(1067, "Session chat không được để trống", HttpStatus.BAD_REQUEST),
    CHAT_SESSION_PROCESSING_ERROR(1068, "Không thể xử lý dữ liệu lịch sử chat", HttpStatus.INTERNAL_SERVER_ERROR),
    CHAT_SESSION_TITLE_REQUIRED(1069, "Tiêu đề phiên chat không được để trống", HttpStatus.BAD_REQUEST),
    CHAT_SESSION_NOT_FOUND(1077, "Không tìm thấy phiên chat", HttpStatus.NOT_FOUND),
    CHAT_MESSAGE_REQUIRED(1078, "Tin nhắn không được để trống", HttpStatus.BAD_REQUEST),
    ANYTHING_LLM_ERROR(1079, "Không thể kết nối hoặc xử lý phản hồi từ AnythingLLM", HttpStatus.INTERNAL_SERVER_ERROR),

    AI_SYSTEM_CONFIG_FAILED(1080, "Không thể lấy cấu hình hệ thống AnythingLLM", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_UPDATE_CONFIG_FAILED(1081, "Không thể cập nhật cấu hình AnythingLLM", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_CONNECTION_FAILED(1082, "Không thể kết nối đến AnythingLLM", HttpStatus.SERVICE_UNAVAILABLE),
    AI_WORKSPACE_NOT_FOUND(1083, "Không tìm thấy workspace trên AnythingLLM", HttpStatus.NOT_FOUND),
    AI_WORKSPACE_UPDATE_FAILED(1084, "Không thể cập nhật workspace", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_WORKSPACE_CREATE_FAILED(1089, "Không thể tạo workspace", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_WORKSPACE_DELETE_FAILED(1090, "Không thể xoá workspace", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_DOCUMENT_UPLOAD_FAILED(1085, "Tải document lên AnythingLLM thất bại", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_DOCUMENT_DELETE_FAILED(1086, "Xoá document thất bại", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_RE_EMBED_FAILED(1087, "Re-embed documents thất bại", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_PROVIDER_MODELS_FAILED(1088, "Không thể lấy danh sách model từ provider", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_DOCUMENT_CONTENT_NOT_FOUND(1091, "Không tìm thấy nội dung document", HttpStatus.NOT_FOUND),
    AI_PROVIDER_API_KEY_MISSING(1093, "Vui lòng cấu hình API key trước khi phát hiện model", HttpStatus.BAD_REQUEST),
    AI_PROVIDER_AUTH_FAILED(1094, "API key không hợp lệ hoặc hết hạn cho provider này", HttpStatus.UNAUTHORIZED),
    AI_PROVIDER_UPSTREAM_ERROR(1095, "Lỗi từ provider AI: ", HttpStatus.BAD_GATEWAY),
    AI_OLLAMA_EMBEDDING_FAILED(1096, "[500] Ollama embedding không tìm thấy hoặc chưa được khởi động", HttpStatus.INTERNAL_SERVER_ERROR),

    // AUDIT LOG
    INVALID_DATE_RANGE(1092, "Khoảng thời gian không hợp lệ: 'from' phải trước hoặc bằng 'to'", HttpStatus.BAD_REQUEST),

    PASSWORD_INCORRECT(1070, "Mật khẩu hiện tại không chính xác", HttpStatus.BAD_REQUEST),
    PASSWORD_CHANGE_LIMIT(1071, "Bạn chỉ được phép đổi mật khẩu 1 lần trong vòng 7 ngày", HttpStatus.BAD_REQUEST),
    OTP_ATTEMPTS_EXCEEDED(1072, "Bạn đã nhập sai mã OTP quá số lần cho phép. Vui lòng yêu cầu mã mới.", HttpStatus.BAD_REQUEST),
    OTP_NOT_VERIFIED(1073, "Vui lòng xác thực mã OTP trước khi đặt lại mật khẩu", HttpStatus.BAD_REQUEST),

    // ONBOARDING (complete-profile)
    WEAK_PASSWORD(1074, "Mật khẩu quá yếu. Cần tối thiểu 8 ký tự và đạt mức Trung bình (gồm chữ hoa, chữ thường, số hoặc ký tự đặc biệt).", HttpStatus.BAD_REQUEST),
    PROFILE_ALREADY_COMPLETED(1075, "Hồ sơ của bạn đã được hoàn tất trước đó", HttpStatus.BAD_REQUEST),
    DOB_REQUIRED(1076, "Ngày sinh không được để trống", HttpStatus.BAD_REQUEST),


    // NOTIFICATION & FEEDBACK ERRORS
    NOTIFICATION_NOT_FOUND(1600, "Không tìm thấy thông báo", HttpStatus.NOT_FOUND),
    FEEDBACK_NOT_FOUND(1601, "Không tìm thấy nhận xét", HttpStatus.NOT_FOUND),

    // OAUTH2 ERRORS
    OAUTH2_EMAIL_NOT_VERIFIED(2001, "Email Google chưa được xác thực. Vui lòng xác thực email trước khi đăng nhập.", HttpStatus.BAD_REQUEST),
    OAUTH2_PROCESSING_ERROR(2002, "Lỗi xử lý đăng nhập Google. Vui lòng thử lại.", HttpStatus.INTERNAL_SERVER_ERROR),

    // FILE / STORAGE ERRORS
    FILE_REQUIRED(1700, "File không được để trống", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE(1701, "Định dạng file không được hỗ trợ", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(1702, "Kích thước file vượt quá giới hạn cho phép", HttpStatus.BAD_REQUEST),
    FILE_READ_FAILED(1703, "Không thể đọc nội dung file", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_UPLOAD_FAILED(1704, "Tải file lên thất bại, vui lòng thử lại", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DELETE_FAILED(1705, "Xóa file thất bại, vui lòng thử lại", HttpStatus.INTERNAL_SERVER_ERROR),
    SIGNED_URL_FAILED(1706, "Không thể tạo đường dẫn truy cập file", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_PATH_REQUIRED(1707, "Đường dẫn file không được để trống", HttpStatus.BAD_REQUEST),

    // PROJECT ERRORS
    PROJECT_NAME_REQUIRED(1800, "Tên dự án không được để trống", HttpStatus.BAD_REQUEST),
    PROJECT_NAME_TOO_LONG(1801, "Tên dự án không được vượt quá 255 ký tự", HttpStatus.BAD_REQUEST),
    DESCRIPTION_TOO_LONG(1802, "Mô tả không được vượt quá 1000 ký tự", HttpStatus.BAD_REQUEST),
    PROJECT_NOT_FOUND(1803, "Không tìm thấy dự án", HttpStatus.NOT_FOUND),
    PROJECT_ACCESS_DENIED(1804, "Bạn không có quyền truy cập dự án này", HttpStatus.FORBIDDEN),

    // SHEET ERRORS
    SHEET_NOT_FOUND(1900, "Không tìm thấy trang biểu đồ", HttpStatus.NOT_FOUND),
    SHEET_NAME_REQUIRED(1901, "Tên trang không được để trống", HttpStatus.BAD_REQUEST),

    // PAYMENT ERRORS
    PLAN_NOT_FOUND(2100, "Gói dịch vụ không tồn tại", HttpStatus.NOT_FOUND),
    PAYMENT_LINK_CREATION_FAILED(2101, "Không thể tạo liên kết thanh toán, vui lòng thử lại", HttpStatus.INTERNAL_SERVER_ERROR),
    TRANSACTION_NOT_FOUND(2102, "Không tìm thấy giao dịch", HttpStatus.NOT_FOUND),
    PAYMENT_ALREADY_PROCESSED(2103, "Giao dịch đã được xử lý trước đó", HttpStatus.BAD_REQUEST),

    // PLAN MANAGEMENT ERRORS
    PLAN_NAME_REQUIRED(2110, "Tên gói không được để trống", HttpStatus.BAD_REQUEST),
    PLAN_PRICE_REQUIRED(2111, "Giá gói không được để trống", HttpStatus.BAD_REQUEST),
    PLAN_PRICE_INVALID(2112, "Giá gói phải lớn hơn hoặc bằng 0", HttpStatus.BAD_REQUEST),
    PLAN_NAME_EXISTED(2113, "Tên gói đã tồn tại", HttpStatus.BAD_REQUEST),
    PLAN_HAS_SUBSCRIBERS(2114, "Không thể xoá gói đang có người đăng ký. Hãy chuyển sang trạng thái archived.", HttpStatus.BAD_REQUEST),

    // FEATURE CATALOG ERRORS
    FEATURE_LABEL_REQUIRED(2120, "Tên tính năng không được để trống", HttpStatus.BAD_REQUEST),
    FEATURE_NOT_FOUND(2121, "Không tìm thấy tính năng", HttpStatus.NOT_FOUND),
    FEATURE_LABEL_EXISTED(2122, "Tên tính năng đã tồn tại", HttpStatus.BAD_REQUEST),

    // QUOTA / RATE LIMIT ERRORS
    QUOTA_EXCEEDED(2130, "Bạn đã dùng hết lượt AI trong kỳ. Hãy nâng gói hoặc đợi kỳ sau.", HttpStatus.PAYMENT_REQUIRED),
    RATE_LIMIT_EXCEEDED(2131, "Bạn đang yêu cầu quá nhiều lần, vui lòng chờ.", HttpStatus.TOO_MANY_REQUESTS),
    PLAN_LIMIT_EXCEEDED(2132, "Bạn đã đạt giới hạn của gói. Hãy nâng gói để dùng thêm.", HttpStatus.FORBIDDEN),
    ;

    private int code;
    private String message;
    private HttpStatusCode statusCode;

}
