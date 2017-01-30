package org.apache.james.mailbox.store.probe;

public interface SieveProbe {

    long getSieveQuota() throws Exception;

    void setSieveQuota(long quota) throws Exception;

    void removeSieveQuota() throws Exception;

    long getSieveQuota(String user) throws Exception;

    void setSieveQuota(String user, long quota) throws Exception;

    void removeSieveQuota(String user) throws Exception;

    void addActiveSieveScript(String user, String name, String script) throws Exception;

}