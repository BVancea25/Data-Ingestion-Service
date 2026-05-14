package com.dataflow.dataingestionservice.Errors;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/categories");

    @Test
    void dataIntegrityViolationReturnsSanitizedConflict() {
        var response = handler.handleConflict(
                new DataIntegrityViolationException("Duplicate entry for table categories column user_id"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ApiErrorCode.DATA_CONFLICT);
        assertThat(response.getBody().message()).doesNotContain("categories", "user_id", "Duplicate entry");
    }

    @Test
    void illegalArgumentReturnsValidationError() {
        var response = handler.handleBadRequest(new IllegalArgumentException("raw internal value"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ApiErrorCode.VALIDATION_ERROR);
        assertThat(response.getBody().message()).doesNotContain("raw internal value");
    }
}
