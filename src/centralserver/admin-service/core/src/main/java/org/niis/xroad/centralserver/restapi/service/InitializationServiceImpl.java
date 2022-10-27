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
package org.niis.xroad.centralserver.restapi.service;

import ee.ria.xroad.common.CodedException;
import ee.ria.xroad.common.ErrorCodes;
import ee.ria.xroad.commonui.SignerProxy;
import ee.ria.xroad.signer.protocol.dto.TokenInfo;
import ee.ria.xroad.signer.protocol.dto.TokenStatusInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.niis.xroad.centralserver.restapi.service.exception.InvalidCharactersException;
import org.niis.xroad.centralserver.restapi.service.exception.InvalidInitParamsException;
import org.niis.xroad.centralserver.restapi.service.exception.ServerAlreadyFullyInitializedException;
import org.niis.xroad.centralserver.restapi.service.exception.SoftwareTokenInitException;
import org.niis.xroad.centralserver.restapi.service.exception.WeakPinException;
import org.niis.xroad.cs.admin.api.dto.HAConfigStatus;
import org.niis.xroad.cs.admin.api.dto.InitialServerConfDto;
import org.niis.xroad.cs.admin.api.dto.InitializationStatusDto;
import org.niis.xroad.cs.admin.api.dto.TokenInitStatus;
import org.niis.xroad.cs.admin.api.facade.SignerProxyFacade;
import org.niis.xroad.cs.admin.api.service.InitializationService;
import org.niis.xroad.cs.admin.api.service.SystemParameterService;
import org.niis.xroad.cs.admin.api.service.TokenPinValidator;
import org.niis.xroad.cs.admin.core.entity.GlobalGroupEntity;
import org.niis.xroad.cs.admin.core.repository.GlobalGroupRepository;
import org.niis.xroad.restapi.config.audit.AuditDataHelper;
import org.niis.xroad.restapi.config.audit.RestApiAuditProperty;
import org.niis.xroad.restapi.exceptions.DeviationCodes;
import org.niis.xroad.restapi.service.SignerNotReachableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ee.ria.xroad.common.ErrorCodes.X_KEY_NOT_FOUND;

@SuppressWarnings("checkstyle:TodoComment")
@Slf4j
@Service
@Transactional(rollbackOn = WeakPinException.class)
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class InitializationServiceImpl implements InitializationService {

    private final SignerProxyFacade signerProxyFacade;
    private final GlobalGroupRepository globalGroupRepository;
    private final SystemParameterService systemParameterService;
    private final TokenPinValidator tokenPinValidator;
    private final AuditDataHelper auditDataHelper;
    private final HAConfigStatus currentHaConfigStatus;

    @Override
    public InitializationStatusDto getInitializationStatus() {
        TokenInitStatus initStatusInfo = getTokenInitStatusInfo();
        InitializationStatusDto statusDto = new InitializationStatusDto();

        statusDto.setInstanceIdentifier(getStoredInstanceIdentifier());
        statusDto.setCentralServerAddress(getStoredCentralServerAddress());
        statusDto.setSoftwareTokenInitStatus(initStatusInfo);
        return statusDto;
    }

    private TokenInitStatus getTokenInitStatusInfo() {
        TokenInitStatus initStatusInfo;
        try {
            if (isSWTokenInitialized()) {
                initStatusInfo = TokenInitStatus.INITIALIZED;
            } else {
                initStatusInfo = TokenInitStatus.NOT_INITIALIZED;
            }
        } catch (SignerNotReachableException notReachableException) {
            log.info("getInitializationStatus - signer was not reachable", notReachableException);
            initStatusInfo = TokenInitStatus.UNKNOWN;
        }
        return initStatusInfo;
    }


    public void initialize(InitialServerConfDto configDto)
            throws ServerAlreadyFullyInitializedException, SoftwareTokenInitException, InvalidCharactersException,
            WeakPinException, InvalidInitParamsException {

        log.debug("initializing server with {}", configDto);

        auditDataHelper.put(RestApiAuditProperty.CENTRAL_SERVER_ADDRESS, configDto.getCentralServerAddress());
        auditDataHelper.put(RestApiAuditProperty.INSTANCE_IDENTIFIER, configDto.getInstanceIdentifier());
        auditDataHelper.put(RestApiAuditProperty.HA_NODE, currentHaConfigStatus.getCurrentHaNodeName());

        if (null == configDto.getSoftwareTokenPin()) {
            configDto.setSoftwareTokenPin("");
        }
        if (null == configDto.getCentralServerAddress()) {
            configDto.setCentralServerAddress("");
        }
        if (null == configDto.getInstanceIdentifier()) {
            configDto.setInstanceIdentifier("");
        }

        if (null != configDto.getSoftwareTokenPin()) {
            tokenPinValidator.validateSoftwareTokenPin(configDto.getSoftwareTokenPin().toCharArray());
        }

        final boolean isSWTokenInitialized = TokenInitStatus.INITIALIZED == getTokenInitStatusInfo();
        final boolean isServerAddressInitialized = !getStoredCentralServerAddress().isEmpty();
        final boolean isInstanceIdentifierInitialized = !getStoredInstanceIdentifier().isEmpty();
        validateConfigParameters(configDto,
                isSWTokenInitialized,
                isServerAddressInitialized,
                isInstanceIdentifierInitialized);

        if (!isServerAddressInitialized) {
            systemParameterService.updateOrCreateParameter(
                    SystemParameterServiceImpl.CENTRAL_SERVER_ADDRESS,
                    configDto.getCentralServerAddress()
            );
        }

        if (!isInstanceIdentifierInitialized) {
            systemParameterService.updateOrCreateParameter(
                    SystemParameterServiceImpl.INSTANCE_IDENTIFIER,
                    configDto.getInstanceIdentifier()
            );
        }

        initializeGlobalGroupForSecurityServerOwners();

        initializeCsSystemParameters();

        if (!isSWTokenInitialized) {
            try {
                signerProxyFacade.initSoftwareToken(configDto.getSoftwareTokenPin().toCharArray());
            } catch (Exception e) {
                if (e instanceof CodedException
                        && ((CodedException) e).getFaultCode().contains(ErrorCodes.X_TOKEN_PIN_POLICY_FAILURE)) {
                    log.warn(new StringBuilder().append("Signer saw Token pin policy failure, ")
                                    .append("remember to restart also the central server after ")
                                    .append("configuring policy enforcement")
                                    .toString(),
                            e);
                    throw new WeakPinException("Token pin policy failure at Signer");
                }
                log.warn("Software token initialization failed", e);
                throw new SoftwareTokenInitException("Software token initialization failed", e);
            }
        }
    }

    private void validateConfigParameters(InitialServerConfDto configDto,
                                          boolean isSWTokenInitialized,
                                          boolean isServerAddressInitialized,
                                          boolean isInstanceIdentifierInitialized)
            throws ServerAlreadyFullyInitializedException, InvalidInitParamsException {


        if (isSWTokenInitialized && isServerAddressInitialized && isInstanceIdentifierInitialized) {
            throw new ServerAlreadyFullyInitializedException(
                    "Central server Initialization failed, already fully initialized"
            );
        }
        List<String> errorMetadata = new ArrayList<>();
        if (isSWTokenInitialized && !configDto.getSoftwareTokenPin().isEmpty()) {
            errorMetadata.add(DeviationCodes.ERROR_METADATA_PIN_EXISTS);
        }
        if (!isSWTokenInitialized && configDto.getSoftwareTokenPin().isEmpty()) {
            errorMetadata.add(DeviationCodes.ERROR_METADATA_PIN_NOT_PROVIDED);
        }
        if (isServerAddressInitialized && !configDto.getCentralServerAddress().isEmpty()) {
            errorMetadata.add(DeviationCodes.ERROR_METADATA_SERVER_ADDRESS_EXISTS);
        }
        if (!isServerAddressInitialized && configDto.getCentralServerAddress().isEmpty()) {
            errorMetadata.add(DeviationCodes.ERROR_METADATA_SERVER_ADDRESS_NOT_PROVIDED);
        }
        if (isInstanceIdentifierInitialized && !configDto.getInstanceIdentifier().isEmpty()) {
            errorMetadata.add(DeviationCodes.ERROR_METADATA_INSTANCE_IDENTIFIER_EXISTS);
        }
        if (!isInstanceIdentifierInitialized && configDto.getInstanceIdentifier().isEmpty()) {
            errorMetadata.add(DeviationCodes.ERROR_METADATA_INSTANCE_IDENTIFIER_NOT_PROVIDED);
        }
        if (!errorMetadata.isEmpty()) {
            log.debug("collected errors {}", String.join(", ", errorMetadata));
            throw new InvalidInitParamsException("Empty, missing or redundant parameters provided for initialization",
                    errorMetadata);
        }
    }

    private void initializeCsSystemParameters() {
        systemParameterService.updateOrCreateParameter(
                SystemParameterServiceImpl.CONF_SIGN_DIGEST_ALGO_ID,
                SystemParameterServiceImpl.DEFAULT_CONF_SIGN_DIGEST_ALGO_ID
        );
        systemParameterService.updateOrCreateParameter(
                SystemParameterServiceImpl.CONF_HASH_ALGO_URI,
                SystemParameterServiceImpl.DEFAULT_CONF_HASH_ALGO_URI
        );
        systemParameterService.updateOrCreateParameter(
                SystemParameterServiceImpl.CONF_SIGN_CERT_HASH_ALGO_URI,
                SystemParameterServiceImpl.DEFAULT_CONF_HASH_ALGO_URI
        );
        systemParameterService.updateOrCreateParameter(
                SystemParameterServiceImpl.SECURITY_SERVER_OWNERS_GROUP,
                SystemParameterServiceImpl.DEFAULT_SECURITY_SERVER_OWNERS_GROUP
        );
    }

    private void initializeGlobalGroupForSecurityServerOwners() {
        Optional<GlobalGroupEntity> securityServerOwnersGlobalGroup = globalGroupRepository
                .getByGroupCode(SystemParameterServiceImpl.DEFAULT_SECURITY_SERVER_OWNERS_GROUP);
        if (securityServerOwnersGlobalGroup.isEmpty()) {
            var defaultSsOwnersGlobalGroup =
                    new GlobalGroupEntity(SystemParameterServiceImpl.DEFAULT_SECURITY_SERVER_OWNERS_GROUP);
            securityServerOwnersGlobalGroup = Optional.of(defaultSsOwnersGlobalGroup);
        }
        securityServerOwnersGlobalGroup.get()
                .setDescription(SystemParameterServiceImpl.DEFAULT_SECURITY_SERVER_OWNERS_GROUP_DESC);
        globalGroupRepository.save(securityServerOwnersGlobalGroup.get());
    }

    private boolean isSWTokenInitialized() {
        boolean isSWTokenInitialized = false;
        TokenInfo tokenInfo;
        try {
            tokenInfo = signerProxyFacade.getToken(SignerProxy.SSL_TOKEN_ID);
            if (null != tokenInfo) {
                isSWTokenInitialized = tokenInfo.getStatus() != TokenStatusInfo.NOT_INITIALIZED;
            }
        } catch (Exception e) {
            if (!((e instanceof CodedException) && X_KEY_NOT_FOUND.equals(((CodedException) e).getFaultCode()))) {
                throw new SignerNotReachableException("could not list all tokens", e);
            }
        }
        return isSWTokenInitialized;
    }


    private String getStoredInstanceIdentifier() {
        return systemParameterService.getParameterValue(
                SystemParameterServiceImpl.INSTANCE_IDENTIFIER,
                ""
        );
    }

    private String getStoredCentralServerAddress() {
        return systemParameterService.getParameterValue(
                SystemParameterServiceImpl.CENTRAL_SERVER_ADDRESS,
                "");
    }

}

