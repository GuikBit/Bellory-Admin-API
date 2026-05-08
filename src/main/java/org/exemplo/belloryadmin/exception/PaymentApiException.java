package org.exemplo.belloryadmin.exception;

import lombok.Getter;

@Getter
public class PaymentApiException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public PaymentApiException(String message) {
        super(message);
        this.statusCode = 0;
        this.responseBody = null;
    }

    public PaymentApiException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public PaymentApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.responseBody = null;
    }

    public PaymentApiException(String message, int statusCode, String responseBody, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
}
