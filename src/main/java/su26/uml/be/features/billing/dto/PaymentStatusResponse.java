package su26.uml.be.features.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentStatusResponse {
    private Long orderCode;
    private String status; // PENDING, PAID, CANCELLED
    private String planName;
}