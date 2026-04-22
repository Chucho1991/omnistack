package com.omnistack.backend.application.port.in;

import com.omnistack.backend.application.dto.ExecuteRequest;
import com.omnistack.backend.application.dto.ExecuteResponse;
import com.omnistack.backend.application.dto.PrecheckRequest;
import com.omnistack.backend.application.dto.PrecheckResponse;
import com.omnistack.backend.application.dto.ReverseRequest;
import com.omnistack.backend.application.dto.ReverseResponse;
import com.omnistack.backend.application.dto.VerifyRequest;
import com.omnistack.backend.application.dto.VerifyResponse;

/**
 * Puerto de entrada para operaciones transaccionales internas.
 */
public interface TransactionUseCase {

    PrecheckResponse precheck(PrecheckRequest request);

    ExecuteResponse execute(ExecuteRequest request);

    VerifyResponse verify(VerifyRequest request);

    ReverseResponse reverse(ReverseRequest request);
}
