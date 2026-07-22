package su26.uml.be.features.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import su26.uml.be.features.audit.annotation.Auditable;
import su26.uml.be.features.audit.AuditContext.Holder;
import su26.uml.be.features.audit.entity.AuditLog;
import su26.uml.be.features.user.entity.User;
import su26.uml.be.features.audit.repository.AuditLogRepository;
import su26.uml.be.features.user.repository.UserRepository;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Cơ chế ghi audit tự động cho các method mang {@link Auditable}.
 *
 * <p>GENERIC: aspect chỉ đọc metadata annotation + {@link AuditContext} (nếu service làm giàu),
 * lấy actor từ SecurityContext và IP từ HttpServletRequest, rồi INSERT một dòng {@code audit_log}
 * SAU KHI method chạy thành công. Mọi lỗi trong quá trình ghi audit đều được nuốt lại (log warn)
 * để KHÔNG BAO GIỜ làm hỏng nghiệp vụ chính.</p>
 *
 * <p>Đặt {@link Order} ở mức ngoài cùng để chạy bọc ngoài lớp {@code @Transactional} của nghiệp vụ:
 * ghi audit chỉ diễn ra khi giao dịch nghiệp vụ đã commit, và bản thân {@code save()} chạy trong
 * giao dịch riêng của Spring Data.</p>
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuditAspect {

    AuditLogRepository auditLogRepository;
    UserRepository userRepository;

    ObjectMapper objectMapper = new ObjectMapper();
    ExpressionParser expressionParser = new SpelExpressionParser();
    ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(su26.uml.be.features.audit.annotation.Auditable)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            Object result = joinPoint.proceed();
            safeRecord(joinPoint);
            return result;
        } finally {
            // Bắt buộc dọn ThreadLocal để không rò rỉ sang request kế tiếp trên cùng thread.
            AuditContext.clear();
        }
    }

    /** Ghi audit — mọi ngoại lệ đều bị nuốt để không ảnh hưởng nghiệp vụ. */
    private void safeRecord(ProceedingJoinPoint joinPoint) {
        try {
            Method method = resolveTargetMethod(joinPoint);
            Auditable auditable = method.getAnnotation(Auditable.class);
            if (auditable == null) {
                return;
            }

            Holder context = AuditContext.current();

            String action = context != null && context.getAction() != null
                    ? context.getAction()
                    : auditable.action();

            String targetId = resolveTargetId(auditable, method, joinPoint.getArgs(), context);
            String detailJson = serializeDetail(context);

            AuditLog.AuditLogBuilder builder = AuditLog.builder()
                    .action(action)
                    .targetType(auditable.targetType())
                    .targetId(targetId)
                    .detail(detailJson)
                    .ipAddress(resolveClientIp());

            applyActor(builder);

            auditLogRepository.save(builder.build());
        } catch (Exception e) {
            log.warn("Audit: không ghi được nhật ký cho thao tác quản trị: {}", e.getMessage(), e);
        }
    }

    /** Lấy method thực (ở lớp impl) để đọc annotation + tên tham số cho SpEL. */
    private Method resolveTargetMethod(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        if (method.isAnnotationPresent(Auditable.class)) {
            return method;
        }
        Object target = joinPoint.getTarget();
        return target.getClass().getMethod(signature.getName(), signature.getParameterTypes());
    }

    /** Ưu tiên id do service set qua AuditContext, sau đó tới SpEL trên tham số. */
    private String resolveTargetId(Auditable auditable, Method method, Object[] args, Holder context) {
        if (context != null && context.getTargetId() != null) {
            return context.getTargetId();
        }
        String expression = auditable.targetId();
        if (expression == null || expression.isBlank()) {
            return null;
        }
        try {
            EvaluationContext evaluationContext =
                    new MethodBasedEvaluationContext(null, method, args, parameterNameDiscoverer);
            Object value = expressionParser.parseExpression(expression).getValue(evaluationContext);
            return value == null ? null : value.toString();
        } catch (Exception e) {
            log.warn("Audit: không resolve được targetId '{}': {}", expression, e.getMessage());
            return null;
        }
    }

    private String serializeDetail(Holder context) {
        if (context == null) {
            return null;
        }
        Map<String, Object> detail = context.getDetail();
        if (detail.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (Exception e) {
            log.warn("Audit: không serialize được detail: {}", e.getMessage());
            return null;
        }
    }

    /** Điền actor (id, email, name) từ SecurityContext → tra cứu User. */
    private void applyActor(AuditLog.AuditLogBuilder builder) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return;
        }
        String email = authentication.getName();
        builder.actorEmail(email);
        userRepository.findByEmail(email).ifPresent(user -> fillActorFromUser(builder, user));
    }

    private void fillActorFromUser(AuditLog.AuditLogBuilder builder, User user) {
        builder.actorId(user.getId());
        builder.actorEmail(user.getEmail());
        builder.actorName(user.getFullName());
    }

    /** IP client: ưu tiên X-Forwarded-For (qua proxy), fallback remoteAddr. */
    private String resolveClientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
