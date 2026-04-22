package com.omnistack.backend.application.port.in;

import com.omnistack.backend.application.dto.BusinessLinesRequest;
import com.omnistack.backend.application.dto.BusinessLinesResponse;

/**
 * Puerto de entrada para consultar la oferta comercial.
 */
public interface BusinessLinesUseCase {

    BusinessLinesResponse getBusinessLines(BusinessLinesRequest request);
}
