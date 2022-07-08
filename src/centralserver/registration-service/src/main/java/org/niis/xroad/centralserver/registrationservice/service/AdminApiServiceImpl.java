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
package org.niis.xroad.centralserver.registrationservice.service;

import ee.ria.xroad.common.CodedException;
import ee.ria.xroad.common.ErrorCodes;
import ee.ria.xroad.common.identifier.SecurityServerId;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.niis.xroad.centralserver.registrationservice.config.RegistrationServiceProperties;
import org.niis.xroad.centralserver.registrationservice.openapi.model.AuthenticationCertificateRegistrationRequest;
import org.niis.xroad.centralserver.registrationservice.openapi.model.ErrorInfo;
import org.niis.xroad.centralserver.registrationservice.openapi.model.ManagementRequest;
import org.niis.xroad.centralserver.registrationservice.openapi.model.ManagementRequestOrigin;
import org.niis.xroad.centralserver.registrationservice.openapi.model.ManagementRequestType;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.RequestEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

@Service
@Slf4j
class AdminApiServiceImpl implements AdminApiService {

    public static final String REQUEST_FAILED = "Registration request failed";
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    @SuppressWarnings("checkstyle:MagicNumber")
    AdminApiServiceImpl(RegistrationServiceProperties properties, RestTemplateBuilder builder, ObjectMapper mapper) {

        CloseableHttpClient client;
        try {
            client = HttpClients.custom()
                    .setSSLHostnameVerifier(new NoopHostnameVerifier())
                    .setSSLContext(SSLContexts.custom()
                            .setProtocol("TLSv1.3")
                            .loadTrustMaterial(
                                    properties.getApiTrustStore().toFile(),
                                    properties.getApiTrustStorePassword().toCharArray())
                            .build())
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setConnectTimeout(1000)
                            .setSocketTimeout(5000)
                            .setConnectionRequestTimeout(10000)
                            .build())
                    .disableAutomaticRetries()
                    .disableCookieManagement()
                    .disableRedirectHandling()
                    .setUserAgent("X-Road Registration Service/7")
                    .build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | CertificateException e) {
            throw new IllegalStateException("Unable to create HTTP clients", e);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load trust material", e);
        }

        if (Strings.isNullOrEmpty(properties.getApiToken())) {
            log.warn("API token not provided");
        }

        this.mapper = mapper;
        this.restTemplate = builder
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client))
                .rootUri(properties.getApiBaseUrl().toString())
                .defaultHeader("Authorization", "X-ROAD-APIKEY TOKEN=" + properties.getApiToken())
                .build();
    }

    @Override
    public int addRegistrationRequest(SecurityServerId serverId, String address, byte[] certificate) {
        var request = new AuthenticationCertificateRegistrationRequest();

        request.setType(ManagementRequestType.AUTH_CERT_REGISTRATION_REQUEST);
        request.setOrigin(ManagementRequestOrigin.SECURITY_SERVER);
        request.setServerAddress(address);
        request.setAuthenticationCertificate(certificate);
        request.setSecurityServerId(serverId.asEncodedId());

        try {
            var result = restTemplate.exchange(
                    RequestEntity.post("/management-requests").body(request),
                    ManagementRequest.class);

            if (!result.hasBody()) {
                throw new CodedException(ErrorCodes.X_INTERNAL_ERROR, "Empty response");
            } else {
                return result.getBody().getId();
            }
        } catch (RestClientResponseException e) {
            var response = e.getResponseBodyAsByteArray();
            try {
                var errorInfo = mapper.readValue(response, ErrorInfo.class);
                var detail = errorInfo.getError() != null ? errorInfo.getError().getCode() : REQUEST_FAILED;
                throw new CodedException(ErrorCodes.X_INTERNAL_ERROR, e, "%s", detail);
            } catch (IOException ex) {
                throw new CodedException(ErrorCodes.X_INTERNAL_ERROR, ex, REQUEST_FAILED);
            }
        } catch (RestClientException e) {
            throw new CodedException(ErrorCodes.X_INTERNAL_ERROR, e, REQUEST_FAILED);
        }
    }
}
