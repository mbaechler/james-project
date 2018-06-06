package org.apache.james.dlp.api;

import java.util.stream.Stream;

import org.apache.james.core.Domain;

public interface DLPConfigurationLoader {
    Stream<DLPConfigurationItem> list(Domain domain);
}
