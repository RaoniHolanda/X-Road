/*
 * The MIT License
 *
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
package org.niis.xroad.cs.admin.core.service;

import ee.ria.xroad.common.SystemProperties;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.niis.xroad.cs.admin.api.domain.DistributedFile;
import org.niis.xroad.cs.admin.api.dto.ConfigurationAnchor;
import org.niis.xroad.cs.admin.api.dto.ConfigurationParts;
import org.niis.xroad.cs.admin.api.dto.GlobalConfDownloadUrl;
import org.niis.xroad.cs.admin.api.dto.HAConfigStatus;
import org.niis.xroad.cs.admin.api.exception.NotFoundException;
import org.niis.xroad.cs.admin.api.service.ConfigurationService;
import org.niis.xroad.cs.admin.api.service.SystemParameterService;
import org.niis.xroad.cs.admin.core.entity.ConfigurationSourceEntity;
import org.niis.xroad.cs.admin.core.entity.DistributedFileEntity;
import org.niis.xroad.cs.admin.core.entity.mapper.DistributedFileMapper;
import org.niis.xroad.cs.admin.core.repository.ConfigurationSourceRepository;
import org.niis.xroad.cs.admin.core.repository.DistributedFileRepository;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.time.Instant;
import java.util.Set;

import static ee.ria.xroad.common.conf.globalconf.ConfigurationConstants.CONTENT_ID_PRIVATE_PARAMETERS;
import static ee.ria.xroad.common.conf.globalconf.ConfigurationConstants.CONTENT_ID_SHARED_PARAMETERS;
import static java.util.stream.Collectors.toSet;
import static org.niis.xroad.cs.admin.api.exception.ErrorMessage.CONFIGURATION_NOT_FOUND;

@Service
@Transactional
@RequiredArgsConstructor
public class ConfigurationServiceImpl implements ConfigurationService {
    private static final String INTERNAL_CONFIGURATION = "INTERNAL";
    private static final Set<String> NODE_LOCAL_CONTENT_IDS = Set.of(
            CONTENT_ID_PRIVATE_PARAMETERS,
            CONTENT_ID_SHARED_PARAMETERS);

    private final SystemParameterService systemParameterService;
    private final HAConfigStatus haConfigStatus;
    private final ConfigurationSourceRepository configurationSourceRepository;
    private final DistributedFileRepository distributedFileRepository;
    private final DistributedFileMapper distributedFileMapper;

    @Override
    public Set<ConfigurationParts> getConfigurationParts(String sourceType) {
        final ConfigurationSourceEntity configurationSource = findConfigurationSourceBySourceType(
                sourceType.toLowerCase());

        Set<DistributedFile> distributedFiles = distributedFileRepository
                .findAllByHaNodeName(configurationSource.getHaNodeName())
                .stream()
                .map(distributedFileMapper::toTarget)
                .collect(toSet());

        return distributedFiles.stream()
                .map(this::createConfParts)
                .collect(toSet());
    }

    @Override
    public ConfigurationAnchor getConfigurationAnchor(String sourceType) {
        final ConfigurationSourceEntity configurationSource = findConfigurationSourceBySourceType(
                sourceType.toLowerCase());

        return new ConfigurationAnchor(configurationSource.getAnchorFileHash(), configurationSource.getAnchorGeneratedAt());
    }

    @Override
    public GlobalConfDownloadUrl getGlobalDownloadUrl(String sourceType) {
        final String csAddress = systemParameterService.getCentralServerAddress();
        final String sourceDirectory = sourceType.equals(INTERNAL_CONFIGURATION)
                ? SystemProperties.getCenterInternalDirectory()
                : SystemProperties.getCenterExternalDirectory();

        final String downloadUrl = "http://" + csAddress + "/" + sourceDirectory;

        return new GlobalConfDownloadUrl(downloadUrl);
    }

    @Override
    public void saveConfigurationPart(String contentIdentifier, String fileName, byte[] data, int version) {
        var distributedFileEntity = findOrCreate(contentIdentifier, version);
        distributedFileEntity.setFileName(fileName);
        distributedFileEntity.setFileData(data);
        distributedFileEntity.setFileUpdatedAt(Instant.now());
        distributedFileRepository.save(distributedFileEntity);
    }

    @Override
    public Set<DistributedFile> getAllConfigurationFiles(int version) {
        return distributedFileRepository.findAllByVersion(version)
                .stream()
                .filter(this::isForCurrentNode)
                .map(distributedFileMapper::toTarget)
                .collect(toSet());
    }

    private boolean isForCurrentNode(DistributedFileEntity distributedFile) {
        if (haConfigStatus.isHaConfigured()
                && NODE_LOCAL_CONTENT_IDS.contains(distributedFile.getContentIdentifier())) {
            return haConfigStatus.getCurrentHaNodeName().equals(distributedFile.getHaNodeName());
        }
        return true;
    }

    private DistributedFileEntity findOrCreate(String contentIdentifier, int version) {
        String dfHaNodeName = haConfigStatus.isHaConfigured() && isNodeLocalContentId(contentIdentifier)
                ? haConfigStatus.getCurrentHaNodeName()
                : null;
        return distributedFileRepository.findByContentIdAndVersion(contentIdentifier, version, dfHaNodeName)
                .orElseGet(() -> new DistributedFileEntity(contentIdentifier, version, dfHaNodeName));
    }

    private boolean isNodeLocalContentId(@NonNull String contentId) {
        return NODE_LOCAL_CONTENT_IDS.contains(contentId);
    }

    private ConfigurationSourceEntity findConfigurationSourceBySourceType(String sourceType) {
        return configurationSourceRepository.findBySourceType(sourceType)
                .orElseThrow(() -> new NotFoundException(CONFIGURATION_NOT_FOUND));
    }

    private ConfigurationParts createConfParts(DistributedFile distributedFile) {
        return new ConfigurationParts(distributedFile.getContentIdentifier(), distributedFile.getFileName(),
                distributedFile.getVersion(), distributedFile.getFileUpdatedAt());
    }
}
