package org.apache.james.modules.mailrepository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.james.mailrepository.api.MailRepository;
import org.apache.james.mailrepository.api.MailRepositoryStore;
import org.apache.james.mailrepository.api.MailRepositoryUrl;
import org.apache.james.mailrepository.memory.MailRepositoryLoader;

public class MultiBoundGuiceRepositoryLoader implements MailRepositoryLoader {
    private final Map<String, Function<MailRepositoryUrl, MailRepository>> factoriesByType;

    @Inject
    private MultiBoundGuiceRepositoryLoader(Set<MailRepositoryFactory> factories) {
        factoriesByType = factories.stream().collect(Collectors.toMap(factory -> factory.fqdn().getName(), it -> it::create));
    }

    public MailRepository load(String fullyQualifiedClassName, MailRepositoryUrl url) throws MailRepositoryStore.MailRepositoryStoreException {
        return Optional.ofNullable(factoriesByType.get(fullyQualifiedClassName))
                .map(factory -> factory.apply(url))
                .orElseThrow(() -> new MailRepositoryStore.MailRepositoryStoreException("no factory for " + fullyQualifiedClassName));
    }
}
