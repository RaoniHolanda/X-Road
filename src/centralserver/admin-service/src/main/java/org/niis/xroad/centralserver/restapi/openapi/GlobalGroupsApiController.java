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

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.niis.xroad.centralserver.openapi.GlobalGroupsApi;
import org.niis.xroad.centralserver.openapi.model.GlobalGroupCodeAndDescriptionDto;
import org.niis.xroad.centralserver.openapi.model.GlobalGroupDescriptionDto;
import org.niis.xroad.centralserver.openapi.model.GlobalGroupResourceDto;
import org.niis.xroad.centralserver.openapi.model.GroupMembersFilterDto;
import org.niis.xroad.centralserver.openapi.model.GroupMembersFilterModelDto;
import org.niis.xroad.centralserver.openapi.model.MembersDto;
import org.niis.xroad.centralserver.openapi.model.PagedGroupMemberDto;
import org.niis.xroad.centralserver.restapi.converter.GroupMemberConverter;
import org.niis.xroad.centralserver.restapi.converter.PageRequestConverter;
import org.niis.xroad.centralserver.restapi.converter.PagedGroupMemberConverter;
import org.niis.xroad.centralserver.restapi.dto.GlobalGroupUpdateDto;
import org.niis.xroad.centralserver.restapi.service.GlobalGroupService;
import org.niis.xroad.restapi.config.audit.AuditEventMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static java.util.Map.entry;
import static org.niis.xroad.restapi.config.audit.RestApiAuditEvent.ADD_GLOBAL_GROUP;
import static org.niis.xroad.restapi.config.audit.RestApiAuditEvent.DELETE_GLOBAL_GROUP;
import static org.niis.xroad.restapi.config.audit.RestApiAuditEvent.EDIT_GLOBAL_GROUP_DESCRIPTION;
import static org.niis.xroad.restapi.openapi.ControllerUtil.API_V1_PREFIX;

@RestController
@RequestMapping(API_V1_PREFIX)
@PreAuthorize("denyAll")
@RequiredArgsConstructor
public class GlobalGroupsApiController implements GlobalGroupsApi {
    private final GlobalGroupService globalGroupService;
    private final GroupMemberConverter groupMemberConverter;
    private final PageRequestConverter pageRequestConverter;
    private final PagedGroupMemberConverter pagedGroupMemberConverter;
    private final PageRequestConverter.MappableSortParameterConverter findSortParameterConverter =
            new PageRequestConverter.MappableSortParameterConverter(
                    entry("owner_name", "owner.name"),
                    entry("xroad_id.member_class", "owner.memberClass.code"),
                    entry("xroad_id.member_code", "owner.memberCode"),
                    entry("xroad_id.server_code", "serverCode")
            );

    @AuditEventMethod(event = ADD_GLOBAL_GROUP)
    @PreAuthorize("hasAuthority('ADD_GLOBAL_GROUP')")
    public ResponseEntity<GlobalGroupResourceDto> addGlobalGroup(GlobalGroupCodeAndDescriptionDto codeAndDescription) {
        return new ResponseEntity<>(globalGroupService.addGlobalGroup(codeAndDescription), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<MembersDto> addGlobalGroupMembers(Integer groupId, MembersDto members) {
        throw new NotImplementedException("addGlobalGroupMembers not implemented yet");
    }

    @Override
    @AuditEventMethod(event = DELETE_GLOBAL_GROUP)
    @PreAuthorize("hasAuthority('DELETE_GROUP')")
    public ResponseEntity<Void> deleteGlobalGroup(Integer groupId) {
        globalGroupService.deleteGlobalGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteGlobalGroupMembers(Integer groupId, MembersDto members) {
        throw new NotImplementedException("deleteGlobalGroupMembers not implemented yet");
    }

    @Override
    @PreAuthorize("hasAuthority('VIEW_GLOBAL_GROUPS')")
    public ResponseEntity<Set<GlobalGroupResourceDto>> findGlobalGroups() {
        return ResponseEntity.ok(globalGroupService.findGlobalGroups());
    }

    @Override
    @PreAuthorize("hasAuthority('VIEW_GROUP_DETAILS')")
    public ResponseEntity<PagedGroupMemberDto> findGlobalGroupMembers(Integer groupId, GroupMembersFilterDto filter) {
        var pageRequest = pageRequestConverter.convert(filter.getPagingSorting(), findSortParameterConverter);
        var resultPage =
                globalGroupService.findGroupMembers(groupMemberConverter.convert(groupId, filter), pageRequest);

        PagedGroupMemberDto pagedResults = pagedGroupMemberConverter.convert(resultPage, filter.getPagingSorting());
        return ResponseEntity.ok(pagedResults);
    }

    @Override
    @PreAuthorize("hasAuthority('VIEW_GROUP_DETAILS')")
    public ResponseEntity<GlobalGroupResourceDto> getGlobalGroup(Integer groupId) {
        return ResponseEntity.ok(globalGroupService.getGlobalGroup(groupId));
    }

    @Override
    @PreAuthorize("hasAuthority('VIEW_GROUP_DETAILS')")
    public ResponseEntity<GroupMembersFilterModelDto> getGroupMembersFilterModel(Integer groupId) {
        return ResponseEntity.ok(globalGroupService.getGroupMembersFilterModel(groupId));
    }

    @Override
    @AuditEventMethod(event = EDIT_GLOBAL_GROUP_DESCRIPTION)
    @PreAuthorize("hasAuthority('EDIT_GROUP_DESCRIPTION')")
    public ResponseEntity<GlobalGroupResourceDto> updateGlobalGroupDescription(
            Integer groupId, GlobalGroupDescriptionDto globalGroupDescription) {
        GlobalGroupUpdateDto updateDto = new GlobalGroupUpdateDto(groupId, globalGroupDescription.getDescription());
        return ResponseEntity.ok(globalGroupService.updateGlobalGroupDescription(updateDto));
    }
}
