/**
 * The MIT License
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.centralserver.restapi.openapi;

import org.junit.Test;
import org.niis.xroad.centralserver.openapi.model.Client;
import org.niis.xroad.centralserver.openapi.model.PagedClients;
import org.niis.xroad.centralserver.restapi.repository.FlattenedSecurityServerClientRepositoryTest;
import org.niis.xroad.centralserver.restapi.util.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClientsApiTest extends AbstractApiRestTemplateTestContext {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    public void searchUsingSecurityServerId() {
        TestUtils.addApiKeyAuthorizationHeader(restTemplate);
        var uriVariables = new HashMap<String, String>();
        uriVariables.put("securityServerId", "TEST:GOV:M1:server1");
        ResponseEntity<PagedClients> response = restTemplate.getForEntity(
                "/api/v1/clients?security_server={securityServerId}",
                PagedClients.class,
                uriVariables);
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().getClients().size());
    }

    @Test
    public void searchAll() {
        TestUtils.addApiKeyAuthorizationHeader(restTemplate);
        ResponseEntity<PagedClients> response = restTemplate.getForEntity(
                "/api/v1/clients", PagedClients.class);
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(FlattenedSecurityServerClientRepositoryTest.CLIENTS_TOTAL_COUNT,
                response.getBody().getClients().size());
    }

    @Test
    public void sortByInstanceId() {
        TestUtils.addApiKeyAuthorizationHeader(restTemplate);
        var uriVariables = new HashMap<String, String>();
        uriVariables.put("sort", "xroad_id.instance_id");
        uriVariables.put("desc", "false");
        ResponseEntity<PagedClients> response = restTemplate.getForEntity(
                "/api/v1/clients?sort={sort}&desc={desc}", PagedClients.class, uriVariables);
        assertEquals("Instance2", response.getBody().getClients().get(0).getXroadId().getInstanceId());

        uriVariables.put("desc", "true");
        response = restTemplate.getForEntity(
                "/api/v1/clients?sort={sort}&desc={desc}", PagedClients.class, uriVariables);
        assertEquals("TEST", response.getBody().getClients().get(0).getXroadId().getInstanceId());
    }

    @Test
    public void stableSort() {
        // check that we always have secondary sort by id, to guarantee e.g. stable paging
        TestUtils.addApiKeyAuthorizationHeader(restTemplate);
        String clientSearchUrl = "/api/v1/clients?sort={sort}&desc={desc}&instance={instance}";
        var uriVariables = new HashMap<String, String>();
        uriVariables.put("sort", "xroad_id.instance_id");
        uriVariables.put("desc", "false");
        uriVariables.put("instance", "test");

        // clients for instance test, sorted by instance id
        ResponseEntity<PagedClients> response = restTemplate.getForEntity(
                clientSearchUrl, PagedClients.class, uriVariables);
        assertEquals(200, response.getStatusCodeValue());
        assertAscendingClientIds(response.getBody().getClients());

        uriVariables.put("desc", "true");
        // clients for instance test, sorted by instance id desc
        response = restTemplate.getForEntity(clientSearchUrl, PagedClients.class, uriVariables);
        assertEquals(200, response.getStatusCodeValue());
        assertAscendingClientIds(response.getBody().getClients());

        // all clients, no specified sorting
        response = restTemplate.getForEntity("/api/v1/clients", PagedClients.class);
        assertEquals(200, response.getStatusCodeValue());
        assertAscendingClientIds(response.getBody().getClients());

        uriVariables.put("sort", "id");
        uriVariables.put("desc", "false");
        uriVariables.put("instance", "test");
        // when "primary-sorting" for id, secondary id sort should not change results
        response = restTemplate.getForEntity(clientSearchUrl, PagedClients.class, uriVariables);
        assertEquals(200, response.getStatusCodeValue());
        assertAscendingClientIds(response.getBody().getClients());

        uriVariables.put("desc", "true");
        response = restTemplate.getForEntity(clientSearchUrl, PagedClients.class, uriVariables);
        assertEquals(200, response.getStatusCodeValue());
        assertDescendingClientIds(response.getBody().getClients());
    }

    private void assertAscendingClientIds(List<Client> clients) {
        assertClientIdOrdering(clients, true);
    }
    private void assertDescendingClientIds(List<Client> clients) {
        assertClientIdOrdering(clients, false);
    }

    private void assertClientIdOrdering(List<Client> clients, boolean ascending) {
        Integer previousClientId = null;
        for (Client client: clients) {
            int clientId = Integer.parseInt(client.getId());
            if (previousClientId != null) {
                boolean orderIsCorrect = false;
                if (ascending) {
                    orderIsCorrect = clientId > previousClientId;
                } else {
                    orderIsCorrect = clientId < previousClientId;
                }
                assertTrue(orderIsCorrect, "clientIds should be in "
                        + (ascending ? "ascending" : "descending")
                        + " order");
            }
            previousClientId = clientId;
        }
    }

}
