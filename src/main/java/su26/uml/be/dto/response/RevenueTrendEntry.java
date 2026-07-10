package su26.uml.be.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "RevenueTrendEntry", description = "Daily MRR snapshot for revenue trend chart.")
public class RevenueTrendEntry {

    @Schema(description = "Snapshot date.", example = "2026-07-01")
    LocalDate date;

    @Schema(description = "MRR on that date.", example = "1250.00")
    BigDecimal mrr;
}
