/**
 * The MIT License
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

import ee.ria.xroad.commonui.CertificateProfileInfoValidator;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.NotImplementedException;
import org.niis.xroad.centralserver.openapi.CertificationServicesApi;
import org.niis.xroad.centralserver.openapi.model.ApprovedCertificationServiceDto;
import org.niis.xroad.centralserver.openapi.model.ApprovedCertificationServiceListItemDto;
import org.niis.xroad.centralserver.openapi.model.CertificateAuthorityDto;
import org.niis.xroad.centralserver.openapi.model.CertificationServiceSettingsDto;
import org.niis.xroad.centralserver.openapi.model.OcspResponderDto;
import org.niis.xroad.centralserver.restapi.converter.ApprovedCertificationServiceDtoConverter;
import org.niis.xroad.centralserver.restapi.dto.AddApprovedCertificationServiceDto;
import org.niis.xroad.centralserver.restapi.dto.CertificationService;
import org.niis.xroad.centralserver.restapi.service.CertificationServicesService;
import org.niis.xroad.restapi.config.audit.AuditEventMethod;
import org.niis.xroad.restapi.config.audit.RestApiAuditEvent;
import org.niis.xroad.restapi.openapi.ControllerUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

import static java.lang.Boolean.parseBoolean;
import static org.springframework.http.ResponseEntity.ok;

@Controller
@PreAuthorize("denyAll")
@RequiredArgsConstructor
@RequestMapping(ControllerUtil.API_V1_PREFIX)
public class CertificationServicesController implements CertificationServicesApi {

    private final CertificationServicesService certificationServicesService;
    private final ApprovedCertificationServiceDtoConverter dtoConverter;

    @Override
    @SneakyThrows
    @PreAuthorize("hasAuthority('ADD_APPROVED_CA')")
    @AuditEventMethod(event = RestApiAuditEvent.ADD_CERTIFICATION_SERVICE)
    public ResponseEntity<ApprovedCertificationServiceDto> addCertificationService(MultipartFile certificate,
                                                                                   String certificateProfileInfo,
                                                                                   String tlsAuth) {
        CertificateProfileInfoValidator.validate(certificateProfileInfo);

        var isForTlsAuth = parseBoolean(tlsAuth);
        var approvedCaDto = new AddApprovedCertificationServiceDto(certificate.getBytes(), certificateProfileInfo, isForTlsAuth);

        CertificationService persistedApprovedCa = certificationServicesService.add(approvedCaDto);

        return ok(dtoConverter.convert(persistedApprovedCa));
    }

    @Override
    public ResponseEntity<CertificateAuthorityDto> addCertificationServiceIntermediateCa(String id, MultipartFile certificate) {
        throw new NotImplementedException("addCertificationServiceIntermediateCa not implemented yet");
    }

    @Override
    public ResponseEntity<OcspResponderDto> addCertificationServiceOcspResponder(String id, String url, MultipartFile certificate) {
        throw new NotImplementedException("addCertificationServiceOcspResponder not implemented yet");
    }

    @Override
    public ResponseEntity<Void> deleteCertificationService(String id) {
        throw new NotImplementedException("deleteCertificationService not implemented yet");
    }

    @Override
    @PreAuthorize("hasAuthority('VIEW_APPROVED_CA_DETAILS')")
    public ResponseEntity<ApprovedCertificationServiceDto> getCertificationService(Integer id) {
        return ok(dtoConverter.convert(certificationServicesService.get(id)));
    }

    @Override
    public ResponseEntity<Set<CertificateAuthorityDto>> getCertificationServiceIntermediateCas(String id) {
        throw new NotImplementedException("getCertificationServiceIntermediateCas not implemented yet");
    }

    @Override
    public ResponseEntity<Set<OcspResponderDto>> getCertificationServiceOcspResponders(String id) {
        throw new NotImplementedException("getCertificationServiceOcspResponders not implemented yet");
    }

    @Override
    @PreAuthorize("hasAuthority('VIEW_APPROVED_CAS')")
    public ResponseEntity<Set<ApprovedCertificationServiceListItemDto>> getCertificationServices() {
        return ok(dtoConverter.convertListItems(certificationServicesService.getCertificationServices()));
    }

    @Override
    @PreAuthorize("hasAuthority('EDIT_APPROVED_CA')")
    @AuditEventMethod(event = RestApiAuditEvent.EDIT_CERTIFICATION_SERVICE_SETTINGS)
    public ResponseEntity<ApprovedCertificationServiceDto> updateCertificationService(String id, CertificationServiceSettingsDto settings) {
        CertificationService approvedCa = new CertificationService()
                .setId(Integer.valueOf(id))
                .setCertificateProfileInfo(settings.getCertificateProfileInfo())
                .setTlsAuth(parseBoolean(settings.getTlsAuth()));

        return ok(dtoConverter.convert(certificationServicesService.update(approvedCa)));
    }
}
