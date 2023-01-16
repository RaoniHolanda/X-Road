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
package org.niis.xroad.cs.admin.core.entity.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.niis.xroad.cs.admin.api.converter.GenericBiDirectionalMapper;
import org.niis.xroad.cs.admin.api.domain.AuthenticationCertificateRegistrationRequestProcessing;
import org.niis.xroad.cs.admin.api.domain.ClientRegistrationRequestProcessing;
import org.niis.xroad.cs.admin.api.domain.OwnerChangeRequestProcessing;
import org.niis.xroad.cs.admin.api.domain.RequestProcessing;
import org.niis.xroad.cs.admin.api.domain.RequestWithProcessing;
import org.niis.xroad.cs.admin.core.entity.AuthenticationCertificateRegistrationRequestProcessingEntity;
import org.niis.xroad.cs.admin.core.entity.ClientRegistrationRequestProcessingEntity;
import org.niis.xroad.cs.admin.core.entity.OwnerChangeRequestProcessingEntity;
import org.niis.xroad.cs.admin.core.entity.RequestProcessingEntity;
import org.niis.xroad.cs.admin.core.entity.RequestWithProcessingEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {ClientIdMapper.class, SecurityServerIdMapper.class})
public interface RequestProcessingMapper extends GenericBiDirectionalMapper<RequestProcessingEntity, RequestProcessing> {

    @Override
    default RequestProcessing toTarget(RequestProcessingEntity source) {
        if (source == null) {
            return null;
        }
        if (source instanceof AuthenticationCertificateRegistrationRequestProcessingEntity) {
            return toDto((AuthenticationCertificateRegistrationRequestProcessingEntity) source);
        }
        if (source instanceof ClientRegistrationRequestProcessingEntity) {
            return toDto((ClientRegistrationRequestProcessingEntity) source);
        }
        if (source instanceof OwnerChangeRequestProcessingEntity) {
            return toDto((OwnerChangeRequestProcessingEntity) source);
        }

        throw new IllegalArgumentException("Cannot map " + source.getClass());
    }

    @Override
    default RequestProcessingEntity fromTarget(RequestProcessing source) {
        if (source == null) {
            return null;
        }
        if (source instanceof AuthenticationCertificateRegistrationRequestProcessing) {
            return fromDto((AuthenticationCertificateRegistrationRequestProcessing) source);
        }
        if (source instanceof ClientRegistrationRequestProcessing) {
            return fromDto((ClientRegistrationRequestProcessing) source);
        }
        if (source instanceof OwnerChangeRequestProcessing) {
            return fromDto((OwnerChangeRequestProcessing) source);
        }


        throw new IllegalArgumentException("Cannot map " + source.getClass());
    }

    default RequestWithProcessingEntity fromDto(RequestWithProcessing source) {

        return null; //TODO
    }

    AuthenticationCertificateRegistrationRequestProcessingEntity fromDto(AuthenticationCertificateRegistrationRequestProcessing source);

    ClientRegistrationRequestProcessingEntity fromDto(ClientRegistrationRequestProcessing source);

    OwnerChangeRequestProcessingEntity fromDto(OwnerChangeRequestProcessing source);

    AuthenticationCertificateRegistrationRequestProcessing toDto(AuthenticationCertificateRegistrationRequestProcessingEntity source);

    ClientRegistrationRequestProcessing toDto(ClientRegistrationRequestProcessingEntity source);

    OwnerChangeRequestProcessing toDto(OwnerChangeRequestProcessingEntity source);

    default RequestWithProcessing toDto(RequestWithProcessingEntity source) {
        return null;
    }


}
