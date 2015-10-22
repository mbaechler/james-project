/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.domainlist.lib;

import javax.inject.Inject;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.api.DomainListException;
import org.apache.james.domainlist.api.DomainListManagementMBean;

public class DomainListManagement extends StandardMBean implements DomainListManagementMBean {

    private DomainList domainList;

    public DomainListManagement() throws NotCompliantMBeanException {
        super(DomainListManagementMBean.class);
    }

    @Inject
    public void setDomainList(DomainList domainList) {
        this.domainList = domainList;
    }

    @Override
    public void addDomain(String domain) throws Exception {
        try {
            domainList.addDomain(domain);
        }
        catch (DomainListException e) {
            throw new Exception(e.getMessage());
        }
    }

    @Override
    public boolean containsDomain(String domain) throws Exception {
        try {
            return domainList.containsDomain(domain);
        }
        catch (DomainListException e) {
            throw new Exception(e.getMessage());
        }
    }

    @Override
    public String[] getDomains() throws Exception {
        try {
            return domainList.getDomains();
        }
        catch (DomainListException e) {
            throw new Exception(e.getMessage());
        }
    }

    @Override
    public void removeDomain(String domain) throws Exception {
        try {
            domainList.removeDomain(domain);
        }
        catch (DomainListException e) {
            throw new Exception(e.getMessage());
        }
    }

    @Override
    public String getDefaultDomain() throws Exception {
        try {
            return domainList.getDefaultDomain();
        }
        catch (DomainListException e) {
            throw new Exception(e.getMessage());
        }

    }

}
