package su26.uml.be.features.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {
    private String checkoutUrl;
    private Long orderCode;
    private String qrCode;
    private BigDecimal amount;
    private String transactionType;
}