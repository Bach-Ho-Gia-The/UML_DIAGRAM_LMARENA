    package su26.uml.be.exception;

    import lombok.Getter;
    import lombok.Setter;

    @Getter
    @Setter
    public class AppException extends RuntimeException {

        private ErrorCode errorCode;
        private String details;

        public AppException() {}

        public AppException(ErrorCode errorCode) {
            this.errorCode = errorCode;
        }

        public AppException(ErrorCode errorCode, String details) {
            this.errorCode = errorCode;
            this.details = details;
        }

        @Override
        public String getMessage() {
            if (details != null) return errorCode != null ? errorCode.getMessage() + ": " + details : details;
            return errorCode != null ? errorCode.getMessage() : null;
        }
    }