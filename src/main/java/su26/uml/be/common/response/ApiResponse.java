package su26.uml.be.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ApiResponse", description = "Standard response envelope wrapping every endpoint result.")
public class ApiResponse<T> {
    @Schema(description = "Application status code (200 = success; see ErrorCode enum for errors).", example = "200")
    int code;

    @Schema(description = "Human-readable message.", example = "Success")
    String message;

    @Schema(description = "Payload of the response; null when there is no body.")
    T result;

    public static <T> ApiResponse<T> success(String message, T result) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage(message);
        response.setResult(result);
        return response;
    }

    public static <T> ApiResponse<T> success(String message) {
        return success(message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(9999);
        response.setMessage(message);
        return response;
    }

}