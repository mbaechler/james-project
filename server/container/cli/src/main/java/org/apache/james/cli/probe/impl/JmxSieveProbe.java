package org.apache.james.cli.probe.impl;

import java.io.IOException;

import javax.management.MalformedObjectNameException;

import org.apache.james.mailbox.store.probe.SieveProbe;
import org.apache.james.sieverepository.api.SieveRepositoryManagementMBean;

public class JmxSieveProbe implements SieveProbe, JmxProbe {
    
    private final static String SIEVEMANAGER_OBJECT_NAME = "org.apache.james:type=component,name=sievemanagerbean";
    
    private SieveRepositoryManagementMBean sieveRepositoryManagement;
    
    public JmxSieveProbe connect(JmxConnection jmxc) throws IOException {
        try {
            jmxc.retrieveBean(SieveRepositoryManagementMBean.class, SIEVEMANAGER_OBJECT_NAME);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Invalid ObjectName? Please report this as a bug.", e);
        }
        return this;
    }

    @Override
    public long getSieveQuota() throws Exception {
        return sieveRepositoryManagement.getQuota();
    }

    @Override
    public void setSieveQuota(long quota) throws Exception {
        sieveRepositoryManagement.setQuota(quota);
    }

    @Override
    public void removeSieveQuota() throws Exception {
        sieveRepositoryManagement.removeQuota();
    }

    @Override
    public long getSieveQuota(String user) throws Exception {
        return sieveRepositoryManagement.getQuota(user);
    }

    @Override
    public void setSieveQuota(String user, long quota) throws Exception {
        sieveRepositoryManagement.setQuota(user, quota);
    }

    @Override
    public void removeSieveQuota(String user) throws Exception {
        sieveRepositoryManagement.removeQuota(user);
    }

    @Override
    public void addActiveSieveScript(String user, String name, String script) throws Exception {
    }
    
}