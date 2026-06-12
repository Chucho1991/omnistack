package com.omnistack.backend.application.service;

import com.omnistack.backend.application.port.out.WsExtLogPort;
import com.omnistack.backend.domain.model.ProviderCallLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WsExtLogService {

    private final WsExtLogPort wsExtLogPort;

    @Async("loggingExecutor")
    public void log(ProviderCallLog entry) {
        try {
            wsExtLogPort.log(entry);
        } catch (Exception ex) {
            log.warn("Error al registrar WS_EXT log uuid={} wsKey={}: {}",
                    entry.getUuid(), entry.getWsKey(), ex.getMessage(), ex);
        }
    }
}
