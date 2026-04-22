package com.omnistack.backend.shared.logging;

import lombok.Builder;
import lombok.Value;

/**
 * Modelo simple para logs estructurados de request.
 */
@Value
@Builder
public class RequestLogEntry {
    String method;
    String path;
    String correlationId;
}
