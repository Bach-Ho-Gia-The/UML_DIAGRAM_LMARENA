package su26.uml.be.features.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "DashboardStatResponse", description = "Aggregated stat for a dashboard metric (users, projects, diagrams, etc.).")
public class DashboardStatResponse {
    @Schema(description = "Total count within the selected time range.", example = "2847")
    long total;

    @Schema(description = "Percentage change vs the preceding period of the same length.", example = "12.5")
    double delta;

    @Schema(description = "Direction of the delta.", example = "up", allowableValues = {"up", "down"})
    String trend;

    @Schema(description = "Sparkline data: 12 evenly-spaced data points within the range.",
            example = "[12, 18, 15, 22, 28, 24, 33, 30, 42, 38, 47, 55]")
    List<Long> sparkline;
}