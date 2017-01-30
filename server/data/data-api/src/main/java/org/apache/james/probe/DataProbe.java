package org.apache.james.probe;

import java.util.List;
import java.util.Map;

import org.apache.james.rrt.lib.Mappings;

public interface DataProbe {

    void addUser(String userName, String password) throws Exception;

    void removeUser(String username) throws Exception;

    void setPassword(String userName, String password) throws Exception;

    String[] listUsers() throws Exception;

    void addDomain(String domain) throws Exception;

    boolean containsDomain(String domain) throws Exception;

    String getDefaultDomain() throws Exception;

    void removeDomain(String domain) throws Exception;

    List<String> listDomains() throws Exception;

    Map<String, Mappings> listMappings() throws Exception;

    Mappings listUserDomainMappings(String user, String domain) throws Exception;

    void addAddressMapping(String user, String domain, String toAddress) throws Exception;

    void removeAddressMapping(String user, String domain, String fromAddress) throws Exception;

    void addRegexMapping(String user, String domain, String regex) throws Exception;

    void removeRegexMapping(String user, String domain, String regex) throws Exception;

}