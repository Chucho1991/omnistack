package com.omnistack.backend.shared.exception;

import com.omnistack.backend.application.dto.ErrorDetail;
import com.omnistack.backend.application.dto.StatusDetail;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApiErrorResponse {
    boolean isError;
    ErrorDetail error;
    StatusDetail status;
}
