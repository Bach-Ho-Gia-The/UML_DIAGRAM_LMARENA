package su26.uml.be.features.audit.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Dánh d?u m?t method (thu?ng ? t?ng service) là m?t thao tác qu?n tr? c?n ghi nh?t ký.
 *
 * <p>H? t?ng audit là GENERIC: ch? c?n thêm annotation này lên m?t action DÃ CÓ endpoint th?t,
 * {@code AuditAspect} s? t? d?ng ghi m?t dòng vào b?ng {@code audit_log} sau khi method ch?y
 * thành công - không ph?i s?a b?ng/API audit.</p>
 *
 * <ul>
 *   <li>{@link #action()} - tên hành d?ng c? d?nh (vd. {@code "AI_CONFIG_UPDATE"}). Service có th?
 *       ghi dè d?ng qua {@code AuditContext.setAction(...)} cho các tru?ng h?p toggle
 *       (vd. khóa/m? ? {@code USER_LOCK}/{@code USER_UNLOCK}).</li>
 *   <li>{@link #targetType()} - lo?i d?i tu?ng b? tác d?ng (vd. {@code "USER"}, {@code "AI_CONFIG"}).</li>
 *   <li>{@link #targetId()} - SpEL trên tham s? method d? l?y id d?i tu?ng (vd. {@code "#userId"}).
 *       B? tr?ng n?u không có, ho?c service t? set qua {@code AuditContext.setTargetId(...)}.</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /** Tên hành d?ng m?c d?nh luu vào c?t {@code action}. */
    String action();

    /** Lo?i d?i tu?ng b? tác d?ng, luu vào c?t {@code target_type}. */
    String targetType();

    /** Bi?u th?c SpEL trên tham s? method d? l?y {@code target_id} (vd. {@code "#userId"}). */
    String targetId() default "";
}
