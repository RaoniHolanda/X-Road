/**
 * The MIT License
 * <p>
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.cs.admin.application.openapi;

import org.junit.jupiter.api.Test;
import org.niis.xroad.centralserver.openapi.model.SystemStatusDto;
import org.niis.xroad.centralserver.openapi.model.VersionDto;
import org.niis.xroad.cs.admin.application.util.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SystemApiTest extends AbstractApiControllerTestContext {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    public void testGetVersionSucceeds() {
        TestUtils.addApiKeyAuthorizationHeader(restTemplate);
        ResponseEntity<VersionDto> response = restTemplate.getForEntity("/api/v1/system/version", VersionDto.class);
        assertNotNull(response, "System Version response  must not be null.");
        assertEquals(200, response.getStatusCodeValue(), "Version response status code must be 200 ");
        assertNotNull(response.getBody());
        assertEquals(ee.ria.xroad.common.Version.XROAD_VERSION, response.getBody().getInfo());
    }

    @Test
    public void testGetVersionFailsIfNotAuthorized() {
        restTemplate.getRestTemplate().setInterceptors(Collections.emptyList());
        var response = restTemplate.getForEntity("/api/v1/system/version", VersionDto.class);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCodeValue());
    }

    @Test
    public void testGetSystemStatusSucceeds() {
        TestUtils.addApiKeyAuthorizationHeader(restTemplate);
        ResponseEntity<SystemStatusDto> response =
                restTemplate.getForEntity("/api/v1/system/status", SystemStatusDto.class);
        assertNotNull(response, "System status response must not be null.");
        assertEquals(200, response.getStatusCodeValue(), "System status response status code must be 200 ");
        assertNotNull(response.getBody());
    }

    @Test
    public void testGetSystemStatusFailsIfNotAuthorized() {
        restTemplate.getRestTemplate().setInterceptors(Collections.emptyList());
        var response = restTemplate.getForEntity("/api/v1/system/status", SystemStatusDto.class);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCodeValue());
    }
}
