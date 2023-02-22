/*
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
package org.niis.xroad.cs.test.glue;

import com.nortal.test.asserts.Assertion;
import feign.FeignException;
import io.cucumber.java.en.Step;
import org.niis.xroad.cs.openapi.model.ClientDto;
import org.niis.xroad.cs.openapi.model.PagedSecurityServersDto;
import org.niis.xroad.cs.openapi.model.PagingSortingParametersDto;
import org.niis.xroad.cs.openapi.model.SecurityServerDto;
import org.niis.xroad.cs.test.api.FeignSecurityServersApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static com.nortal.test.asserts.Assertions.equalsAssertion;
import static com.nortal.test.asserts.Assertions.notNullAssertion;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.StringUtils.split;
import static org.junit.Assert.fail;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class SecurityServerApiStepDefs extends BaseStepDefs {
    @Autowired
    private FeignSecurityServersApi securityServersApi;

    @Step("Security server auth certs for {string} is requested")
    public void systemStatusIsRequested(String id) {
        try {
            var response = securityServersApi.getSecurityServerAuthCerts(id);
            putStepData(StepDataKey.RESPONSE_STATUS, response.getStatusCodeValue());
        } catch (FeignException feignException) {
            putStepData(StepDataKey.RESPONSE_STATUS, feignException.status());
        }
    }

    @Step("Response is of status code {int}")
    public void systemStatusIsValidated(int statusCode) {
        int responseCode = getRequiredStepData(StepDataKey.RESPONSE_STATUS);

        validate(responseCode)
                .assertion(new Assertion.Builder()
                        .message("Verify status code")
                        .expression("=")
                        .actualValue(responseCode)
                        .expectedValue(statusCode)
                        .build())
                .execute();
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    @Step("user can get security server {string} details")
    public void userCanGetSecurityServerDetails(String serverId) {
        final String[] idParts = split(serverId, ':');

        final ResponseEntity<SecurityServerDto> response = securityServersApi.getSecurityServer(serverId);

        validate(response)
                .assertion(equalsStatusCodeAssertion(OK))
                .assertion(equalsAssertion(serverId, "body.id", "Server id should match"))
                .assertion(equalsAssertion(idParts[0], "body.xroadId.instanceId", "Instance id should match"))
                .assertion(equalsAssertion(idParts[1], "body.xroadId.memberClass", "Member class id should match"))
                .assertion(equalsAssertion(idParts[2], "body.xroadId.memberCode", "Member code class id should match"))
                .assertion(equalsAssertion(idParts[3], "body.xroadId.serverCode", "Server code class id should match"))
                .assertion(equalsAssertion("Member name for " + serverId.substring(0, serverId.lastIndexOf(':')),
                        "body.ownerName", "Owner name id should match"))
                .assertion(equalsAssertion("security-server-address-" + idParts[3], "body.serverAddress", "Server address id should match"))
                .assertion(notNullAssertion("body.createdAt"))
                .execute();
    }

    @Step("getting non existing security server details fails")
    public void gettingNonExistingSecurityServerDetailsFails() {
        try {
            securityServersApi.getSecurityServer(randomSecurityServerId());
            fail("Should throw exception");
        } catch (FeignException exception) {
            validate(exception.status())
                    .assertion(new Assertion.Builder()
                            .message("Verify status code")
                            .expression("=")
                            .actualValue(exception.status())
                            .expectedValue(NOT_FOUND.value())
                            .build())
                    .execute();
        }
    }

    @Step("security servers list contains {string}")
    public void securityServersListContains(String serverId) {
        final ResponseEntity<PagedSecurityServersDto> response = securityServersApi
                .findSecurityServers("", new PagingSortingParametersDto());

        validate(response)
                .assertion(equalsStatusCodeAssertion(OK))
                .assertion(equalsAssertion(1, "body.items.?[id=='" + serverId + "'].size",
                        "Servers list contains id " + serverId))
                .execute();
    }

    @Step("security server {string} clients contains {string}")
    public void securityServerClientsContains(String serverId, String clientId) {
        final ResponseEntity<Set<ClientDto>> response = securityServersApi.getSecurityServerClients(serverId);

        final String[] idParts = split(clientId, ':');

        validate(response)
                .assertion(equalsStatusCodeAssertion(OK))
                .assertion(equalsAssertion(1,
                        "body.?[xroadId.instanceId =='" + idParts[0] + "' "
                                + "and xroadId.memberClass == '" + idParts[1] + "' "
                                + "and xroadId.memberCode == '" + idParts[2] + "'].size",
                        "Clients list contains " + clientId))
                .execute();
    }

    @Step("security server {string} has no clients")
    public void securityServerHasNoClients(String serverId) {
        final ResponseEntity<Set<ClientDto>> response = securityServersApi.getSecurityServerClients(serverId);

        validate(response)
                .assertion(equalsStatusCodeAssertion(OK))
                .assertion(equalsAssertion(0, "body.size", "Clients list is empty"))
                .execute();
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private String randomSecurityServerId() {
        return String.format("%s:%s:%s:%s", randomAlphabetic(3), randomAlphabetic(3),
                randomAlphabetic(3), randomAlphabetic(3));
    }

}
