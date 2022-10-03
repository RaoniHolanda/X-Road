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
package org.niis.xroad.centralserver.restapi.openapi;

import ee.ria.xroad.signer.protocol.dto.TokenInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.niis.xroad.centralserver.openapi.model.InitialServerConfDto;
import org.niis.xroad.centralserver.openapi.model.InitializationStatusDto;
import org.niis.xroad.centralserver.openapi.model.TokenInitStatusDto;
import org.niis.xroad.centralserver.restapi.util.TokenTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.ConstraintViolationException;

import static ee.ria.xroad.commonui.SignerProxy.SSL_TOKEN_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.niis.xroad.centralserver.restapi.service.SystemParameterService.CENTRAL_SERVER_ADDRESS;
import static org.niis.xroad.centralserver.restapi.service.SystemParameterService.INSTANCE_IDENTIFIER;

@WithMockUser(authorities = {"INIT_CONFIG"})
@Transactional
public class InitializationApiControllerTest extends AbstractApiControllerTestContext {

    @Autowired
    InitializationApiController initializationApiController;

    private InitialServerConfDto okConf;
    private TokenInfo testSWToken;

    @BeforeEach
    public void setup() {
        okConf = new InitialServerConfDto()
                .centralServerAddress("xroad.example.org")
                .instanceIdentifier("TEST")
                .softwareTokenPin("1234ABCD%&/(");

        testSWToken = new TokenTestUtils.TokenInfoBuilder()
                .id(SSL_TOKEN_ID)
                .build();

    }

    @Test
    public void getInitializationStatus() throws Exception {
        when(signerProxyFacade.getToken(SSL_TOKEN_ID)).thenReturn(
                null);
        when(systemParameterService.getParameterValue(
                eq(INSTANCE_IDENTIFIER),
                any()
        )).thenReturn("");
        when(systemParameterService.getParameterValue(eq(CENTRAL_SERVER_ADDRESS), any())).thenReturn("");

        ResponseEntity<InitializationStatusDto> response = initializationApiController.getInitializationStatus();
        assertNotNull(response, "status should be always available");
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.hasBody());
        assertNotNull(response.getBody());
        assertEquals(
                TokenInitStatusDto.NOT_INITIALIZED,
                response.getBody().getSoftwareTokenInitStatus(),
                "TokenInit status should be NOT_INITIALIZED before initialization"
        );
        assertTrue(
                response.getBody().getCentralServerAddress().isEmpty(),
                "No Server Address should be initialized yet."
        );
    }

    @Test
    public void getInitializationStatusFailingSignerConnection() throws Exception {

        when(signerProxyFacade.getToken(SSL_TOKEN_ID)).thenThrow(RuntimeException.class);
        assertDoesNotThrow(() -> {
            final ResponseEntity<InitializationStatusDto> response;
            response = initializationApiController.getInitializationStatus();
            assertTrue(response.hasBody());
            InitializationStatusDto status = response.getBody();
            assertNotNull(status);
            assertEquals(TokenInitStatusDto.UNKNOWN, status.getSoftwareTokenInitStatus());
        });
    }

    @Test
    public void getInitializationStatusFromSignerProxy() throws Exception {
        when(signerProxyFacade.getToken(SSL_TOKEN_ID)).thenReturn(testSWToken);
        ResponseEntity<InitializationStatusDto> statusResponseEntity =
                initializationApiController.getInitializationStatus();
        assertTrue(statusResponseEntity.hasBody());
        InitializationStatusDto status = statusResponseEntity.getBody();
        assertNotNull(status);
        assertEquals(TokenInitStatusDto.INITIALIZED, status.getSoftwareTokenInitStatus());

    }

    @Test
    public void initCentralServer() throws Exception {
        InitialServerConfDto initialServerConf1 = okConf.centralServerAddress("initCentralServer.example.org");
        when(signerProxyFacade.getToken(SSL_TOKEN_ID)).thenReturn(
                        null)  // For 1st status query during initCentralServer() call
                .thenReturn(testSWToken); // for the getInitializationStatus

        when(systemParameterService.getParameterValue(
                eq(INSTANCE_IDENTIFIER),
                any()
        )).thenReturn("")
                .thenReturn(initialServerConf1.getInstanceIdentifier());
        when(systemParameterService.getParameterValue(eq(CENTRAL_SERVER_ADDRESS), any())).thenReturn("").thenReturn(
                initialServerConf1.getCentralServerAddress());
        ResponseEntity<Void> response = initializationApiController.initCentralServer(initialServerConf1);
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());

        ResponseEntity<InitializationStatusDto> statusResponseEntity =
                initializationApiController.getInitializationStatus();
        assertNotNull(statusResponseEntity.getBody());
        assertEquals(
                TokenInitStatusDto.INITIALIZED,
                statusResponseEntity.getBody().getSoftwareTokenInitStatus()
        );
        assertEquals(okConf.getInstanceIdentifier(), statusResponseEntity.getBody().getInstanceIdentifier());
        assertEquals("initCentralServer.example.org", statusResponseEntity.getBody().getCentralServerAddress());
    }

    @Test
    public void initCentralServerMissingParams() {
        InitialServerConfDto testConf = new InitialServerConfDto()
                .instanceIdentifier("TEST")
                .centralServerAddress("initCentralServerMissingParams.example.org")
                .softwareTokenPin(null);
        when(systemParameterService.getParameterValue(
                eq(INSTANCE_IDENTIFIER),
                any()
        )).thenReturn("");
        when(systemParameterService.getParameterValue(eq(CENTRAL_SERVER_ADDRESS), any()))
                .thenReturn("");
        ConstraintViolationException exception = assertThrows(ConstraintViolationException.class,
                () -> initializationApiController.initCentralServer(testConf));
    }

    @Test
    public void initCentralServerAlreadyInitialized() throws Exception {
        String testAddress = "initCentralServerAlreadyInitialized.example.org";
        InitialServerConfDto testInitConf = new InitialServerConfDto()
                .centralServerAddress(testAddress)
                .instanceIdentifier("initCentralServerAlreadyInitialized-instance")
                .softwareTokenPin("12341234ABCabc___");
        when(systemParameterService.getParameterValue(
                eq(INSTANCE_IDENTIFIER),
                any()
        )).thenReturn(testInitConf.getInstanceIdentifier());
        when(systemParameterService.getParameterValue(eq(CENTRAL_SERVER_ADDRESS), any()))
                .thenReturn(testInitConf.getCentralServerAddress());

        when(signerProxyFacade.getToken(SSL_TOKEN_ID)).thenReturn(testSWToken);

        assertThrows(ConflictException.class, () -> initializationApiController.initCentralServer(testInitConf));

    }
}
