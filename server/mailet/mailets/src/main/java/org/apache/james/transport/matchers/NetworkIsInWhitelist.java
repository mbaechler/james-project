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
package org.apache.james.transport.matchers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.mail.MessagingException;

import org.apache.james.core.MailAddress;
import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.dnsservice.library.netmatcher.NetMatcher;
import org.apache.mailet.Experimental;
import org.apache.mailet.Mail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Matcher which lookup whitelisted networks in a database. The networks can be
 * specified via netmask.
 * </p>
 * <p>
 * For example: <code>192.168.0.0/24</code>
 * </p>
 * <p>
 * Th whitelisting is done per recipient
 * </p>
 */
@Experimental
public class NetworkIsInWhitelist extends AbstractSQLWhitelistMatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkIsInWhitelist.class);

    private DNSService dns;
    private String selectNetworks;

    @Inject
    public void setDNSService(DNSService dns) {
        this.dns = dns;
    }

    @Override
    protected String getSQLSectionName() {
        return "NetworkWhiteList";
    }

    @Override
    public void init() throws MessagingException {
        super.init();
        selectNetworks = sqlQueries.getSqlString("selectNetwork", true);

    }

    @Override
    protected boolean matchedWhitelist(MailAddress recipientMailAddress, Mail mail) throws MessagingException {
        String recipientUser = recipientMailAddress.getLocalPart().toLowerCase(Locale.US);
        String recipientHost = recipientMailAddress.getDomain().asString();
        try (Connection conn = datasource.getConnection()) {
            return Flux
                .concat(
                    Mono.fromCallable(() -> loadNetworks(recipientUser, recipientHost, conn)),
                    Mono.fromCallable(() -> loadNetworksForHost(recipientHost, conn)))
                .map(nets -> new NetMatcher(nets, dns))
                .any(matcher -> matcher.matchInetNetwork(mail.getRemoteAddr()))
                .block();
        } catch (SQLException sqle) {
            LOGGER.error("Error accessing database", sqle);
            throw new MessagingException("Exception thrown", sqle);
        }
    }

    private List<String> loadNetworksForHost(String recipientHost, Connection conn) throws SQLException {
        return loadNetworks("*", recipientHost, conn);
    }

    private List<String> loadNetworks(String recipientUser, String recipientHost, Connection conn) throws SQLException {
        List<String> nets = new ArrayList<>();
        try (PreparedStatement statement = conn.prepareStatement(selectNetworks)) {
            statement.setString(1, recipientUser);
            statement.setString(2, recipientHost);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    nets.add(resultSet.getString(1));
                }
            }
        }
        return nets;
    }

    @Override
    protected String getTableCreateQueryName() {
        return "createNetworkWhiteListTable";
    }

    @Override
    protected String getTableName() {
        return "networkWhiteListTableName";
    }

}
