package org.apache.james.modules.mailrepository;

import org.apache.james.mailrepository.api.MailRepository;
import org.apache.james.mailrepository.api.MailRepositoryUrl;

public interface MailRepositoryFactory {

    Class<? extends MailRepository> fqdn();

    MailRepository create(MailRepositoryUrl url);
}
