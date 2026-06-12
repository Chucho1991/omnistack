package com.omnistack.backend.infrastructure.adapter.oracle;

import com.omnistack.backend.application.port.out.WsExtLogPort;
import com.omnistack.backend.domain.model.ProviderCallLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnMissingBean(WsExtLogPort.class)
public class NoOpWsExtLogAdapter implements WsExtLogPort {

    @Override
    public void log(ProviderCallLog entry) {
        log.debug("WsExtLog [no-op] uuid={} wsKey={} isError={}",
                entry.getUuid(), entry.getWsKey(), entry.isError());
    }
}
