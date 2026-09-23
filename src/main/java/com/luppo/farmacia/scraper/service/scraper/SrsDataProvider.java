package com.luppo.farmacia.scraper.service.scraper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SrsDataProvider {
    public void syncSrsReferences() {
        log.info("Sincronizando catálogo de referencia y PVMP con la Superintendencia de Regulación Sanitaria (SRS)...");
    }
}
