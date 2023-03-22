/*
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

package org.niis.xroad.cs.admin.rest.api.openapi;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.niis.xroad.cs.admin.api.service.TrustedAnchorService;
import org.niis.xroad.cs.admin.rest.api.converter.TrustedAnchorConverter;
import org.niis.xroad.cs.openapi.TrustedAnchorsApi;
import org.niis.xroad.cs.openapi.model.TrustedAnchorDto;
import org.niis.xroad.restapi.config.audit.AuditEventMethod;
import org.niis.xroad.restapi.openapi.ControllerUtil;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static org.niis.xroad.restapi.config.audit.RestApiAuditEvent.ADD_TRUSTED_ANCHOR;
import static org.niis.xroad.restapi.util.ResourceUtils.springResourceToBytesOrThrowBadRequest;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

@Controller
@RequestMapping(ControllerUtil.API_V1_PREFIX)
@PreAuthorize("denyAll")
@RequiredArgsConstructor
public class TrustedAnchorsApiController implements TrustedAnchorsApi {

    private final TrustedAnchorService trustedAnchorService;
    private final TrustedAnchorConverter trustedAnchorConverter;

    @Override
    public ResponseEntity<Void> deleteTrustedAnchor(String hash) {
        throw new NotImplementedException("deleteTrustedAnchor not implemented yet.");
    }

    @Override
    public ResponseEntity<Resource> downloadTrustedAnchor(String hash) {
        throw new NotImplementedException("downloadTrustedAnchor not implemented yet.");
    }

    @Override
    public ResponseEntity<List<TrustedAnchorDto>> getTrustedAnchors() {
        throw new NotImplementedException("getTrustedAnchors not implemented yet.");
    }

    @Override
    @PreAuthorize("hasAuthority('UPLOAD_TRUSTED_ANCHOR')")
    public ResponseEntity<TrustedAnchorDto> previewTrustedAnchor(Resource body) {
        return ok(trustedAnchorConverter.toTarget(
                trustedAnchorService.preview(springResourceToBytesOrThrowBadRequest(body)))
        );
    }

    @Override
    @AuditEventMethod(event = ADD_TRUSTED_ANCHOR)
    @PreAuthorize("hasAuthority('UPLOAD_TRUSTED_ANCHOR')")
    public ResponseEntity<TrustedAnchorDto> uploadTrustedAnchor(Resource body) {
        return status(CREATED).body(
                trustedAnchorConverter.toTarget(
                        trustedAnchorService.upload(springResourceToBytesOrThrowBadRequest(body)))
        );
    }

}
