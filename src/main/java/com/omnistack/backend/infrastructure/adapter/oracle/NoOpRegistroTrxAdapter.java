package com.omnistack.backend.infrastructure.adapter.oracle;

import com.omnistack.backend.application.port.out.RegistroTrxPort;
import com.omnistack.backend.domain.model.RegistroTrx;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnMissingBean(RegistroTrxPort.class)
public class NoOpRegistroTrxAdapter implements RegistroTrxPort {

    @Override
    public void save(RegistroTrx entry) {
        log.debug("RegistroTrx [no-op] uuid={} capability={} codEstado={}",
                entry.getUuid(), entry.getCapability(), entry.getCodEstado());
    }

    @Override
    public Optional<String> findOriginalAuthByHomologatedCode(String homologatedCode) {
        log.debug("findOriginalAuthByHomologatedCode [no-op] homologatedCode={}", homologatedCode);
        return Optional.empty();
    }
}
