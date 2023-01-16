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
package org.niis.xroad.cs.admin.jpa.repository;

import ee.ria.xroad.common.identifier.XRoadObjectType;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.niis.xroad.cs.admin.api.service.ClientService;
import org.niis.xroad.cs.admin.core.entity.FlattenedSecurityServerClientViewEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Disabled("Deprecated. Should be replaced by servicetests")
public class FlattenedSecurityServerClientViewRepositoryTest extends AbstractRepositoryTest {

    public static final int CLIENTS_TOTAL_COUNT = 18;
    public static final int SUBSYSTEMS_TOTAL_COUNT = 3;
    public static final int MEMBERS_TOTAL_COUNT = CLIENTS_TOTAL_COUNT - SUBSYSTEMS_TOTAL_COUNT;
    @Autowired
    private JpaFlattenedSecurityServerClientRepository repository;

    @Test
    public void findUsingSpecialCharacters() {
        // free text search using % and _ which have special handling in LIKE queries
        // Member6\a
        // Member7_a
        // Member8%a
        // Member9__%%em%
        List<FlattenedSecurityServerClientViewEntity> clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.multifieldSearch("%"));
        assertEquals(2, clients.size());

        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.multifieldSearch("_"));
        assertEquals(2, clients.size());

        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.multifieldSearch("\\"));
        assertEquals(1, clients.size());

        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.multifieldSearch("%%"));
        assertEquals(1, clients.size());

        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.multifieldSearch("%em%"));
        assertEquals(1, clients.size());
    }

    @Test
    public void multifieldTextSearch() {
        // member name, member_class, member_code, subsystem_code

        // member name
        List<FlattenedSecurityServerClientViewEntity> clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.multifieldSearch("Member1"));
        assertEquals(4, clients.size());

        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.multifieldSearch("Member2"));
        assertEquals(1, clients.size());

        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.multifieldSearch("member1"));
        assertEquals(4, clients.size());

        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.multifieldSearch("member"));
        assertEquals(12, clients.size());

        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.multifieldSearch("ÅÖÄ"));
        assertEquals(1, clients.size());

        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.multifieldSearch("åöä"));
        assertEquals(1, clients.size());

        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.multifieldSearch("ÅöÄ"));
        assertEquals(1, clients.size());

        // member class
        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.multifieldSearch("MemberclassFoo"));
        assertEquals(1, clients.size());

        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.multifieldSearch("MemberCLASS"));
        assertEquals(1, clients.size());

        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.multifieldSearch("gOv"));
        assertEquals(CLIENTS_TOTAL_COUNT - 5, clients.size());

        // member code
        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.multifieldSearch("m1"));
        assertEquals(4, clients.size());

        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.multifieldSearch("m4"));
        assertEquals(1, clients.size());

        // subsystem code
        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.multifieldSearch("Ss1"));
        assertEquals(1, clients.size());
    }

    @Test
    public void findClientsByInstance() {
        List<FlattenedSecurityServerClientViewEntity> clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.instance("teS"));
        assertEquals(CLIENTS_TOTAL_COUNT - 1, clients.size());

        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.instance("teStFOO"));
        assertEquals(0, clients.size());
    }

    @Test
    public void findClientsByMemberClass() {
        List<FlattenedSecurityServerClientViewEntity> clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.memberClass("CLASSfoo"));
        assertEquals(1, clients.size());

        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.memberClass("gOV"));
        // other clients: 4 ORG, 1 MemberclassFoo
        assertEquals(CLIENTS_TOTAL_COUNT - 5, clients.size());

        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.memberClass("gOVi"));
        assertEquals(0, clients.size());
    }

    @Test
    public void findClientsByMemberCode() {
        List<FlattenedSecurityServerClientViewEntity> clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.memberCode("m1"));
        assertEquals(4, clients.size());

        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.memberCode("m4"));
        assertEquals(1, clients.size());

        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.memberCode("m"));
        // M1 - M11 + subsystem
        assertEquals(12, clients.size());
    }

    @Test
    public void findClientsBySubsystemCode() {
        List<FlattenedSecurityServerClientViewEntity> clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.subsystemCode("ss"));
        assertEquals(1, clients.size());
    }

    @Test
    public void pagedSortedFindClientsBySecurityServerId() {
        PageRequest page = PageRequest.of(0, 2, Sort.by("id").descending());
        var clientsPage = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.securityServerId(1000001),
                page);
        assertEquals(2, clientsPage.getTotalPages());
        assertEquals(3, clientsPage.getTotalElements());
        assertEquals(2, clientsPage.getNumberOfElements());
        assertEquals(0, clientsPage.getNumber());
        assertEquals(Arrays.asList(1000010, 1000002),
                clientsPage.get().map(FlattenedSecurityServerClientViewEntity::getId).collect(Collectors.toList()));

        page = page.next();
        clientsPage = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.securityServerId(1000001),
                page);
        assertEquals(1, clientsPage.getNumberOfElements());
        assertEquals(1, clientsPage.getNumber());
        assertEquals(Arrays.asList(1000001),
                clientsPage.get().map(FlattenedSecurityServerClientViewEntity::getId).collect(Collectors.toList()));
    }

    @Test
    public void paging() {
        PageRequest page = PageRequest.of(0, 4);
        var memberPage = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.member(),
                page);
        assertEquals(4, memberPage.getTotalPages());
        assertEquals(MEMBERS_TOTAL_COUNT, memberPage.getTotalElements());
        assertEquals(4, memberPage.getNumberOfElements());
        assertEquals(0, memberPage.getNumber());

        page = page.next();
        memberPage = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.member(),
                page);
        assertEquals(4, memberPage.getNumberOfElements());
        assertEquals(1, memberPage.getNumber());

        page = page.next().next();
        memberPage = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.member(),
                page);
        assertEquals(3, memberPage.getNumberOfElements());
        assertEquals(3, memberPage.getNumber());
    }

    @Test
    public void sorting() {
        PageRequest page = PageRequest.of(0, 5, Sort.by("id"));
        Page<FlattenedSecurityServerClientViewEntity> memberPage = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.member(),
                page);
        assertEquals(3, memberPage.getTotalPages());
        assertEquals(MEMBERS_TOTAL_COUNT, memberPage.getTotalElements());
        assertEquals(5, memberPage.getNumberOfElements());
        assertEquals(0, memberPage.getNumber());
        var pageClients = memberPage.stream().collect(Collectors.toList());
        assertEquals(5, pageClients.size());
    }

    @Test
    public void findClientsBySecurityServerId() {
        List<FlattenedSecurityServerClientViewEntity> clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.securityServerId(1000001));
        assertEquals(3, clients.size());

        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.securityServerId(1000002));
        assertEquals(2, clients.size());

        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.securityServerId(1));
        assertEquals(0, clients.size());

    }

    @Test
    public void findClientsBySecurityServerIdAndFreetext() {
        List<FlattenedSecurityServerClientViewEntity> clients = repository.findAll(
                (root, query, builder) -> builder.and(
                        JpaFlattenedSecurityServerClientRepository.clientOfSecurityServerPredicate(root, builder, 1000001),
                        JpaFlattenedSecurityServerClientRepository.multifieldTextSearchPredicate(root, builder, "ss1")
                ));
        assertEquals(1, clients.size());

        clients = repository.findAll(
                (root, query, builder) -> builder.and(
                        JpaFlattenedSecurityServerClientRepository.clientOfSecurityServerPredicate(root, builder, 1000001),
                        JpaFlattenedSecurityServerClientRepository.multifieldTextSearchPredicate(root, builder, "gov")
                ));
        assertEquals(3, clients.size());

        PageRequest page = PageRequest.of(1, 1, Sort.by("id"));
        var clientsPage = repository.findAll(
                (root, query, builder) -> builder.and(
                        JpaFlattenedSecurityServerClientRepository.clientOfSecurityServerPredicate(root, builder, 1000001),
                        JpaFlattenedSecurityServerClientRepository.multifieldTextSearchPredicate(root, builder, "gov")
                ), page);
        assertEquals(3, clientsPage.getTotalPages());
        assertEquals(3, clientsPage.getTotalElements());
        assertEquals(1, clientsPage.getNumberOfElements());
        assertEquals(1, clientsPage.getNumber());
    }

    @SuppressWarnings("checkstyle:MethodLength") // I think it makes sense to test all of these in same test
    @Test
    public void findClientsByMultiParameterSearch() {
        List<FlattenedSecurityServerClientViewEntity> clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                ));
        assertEquals(CLIENTS_TOTAL_COUNT, clients.size());

        clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                                .setSecurityServerId(1000001)
                ));
        assertEquals(3, clients.size());

        clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                                .setSecurityServerId(1000001)
                                .setMultifieldSearch("ss1")
                ));
        assertEquals(1, clients.size());

        clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                                .setSecurityServerId(1000001)
                                .setMemberCodeSearch("m1")
                ));
        assertEquals(2, clients.size());

        clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                                .setSecurityServerId(1000001)
                                .setMultifieldSearch("ss1")
                                .setMemberCodeSearch("m1")
                ));
        assertEquals(1, clients.size());

        clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                                .setSecurityServerId(1000001)
                                .setMultifieldSearch("ss1")
                                .setMemberCodeSearch("m1-does-not-exist")
                ));
        assertEquals(0, clients.size());

        // memberClass
        clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                                .setSecurityServerId(1000001)
                                .setMemberClassSearch("gov")
                ));
        assertEquals(3, clients.size());

        clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                                .setSecurityServerId(1000001)
                                .setMemberClassSearch("gov")
                                .setMultifieldSearch("ss1")
                ));
        assertEquals(1, clients.size());

        clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                                .setMemberCodeSearch("m1")
                                .setMemberClassSearch("gov")
                ));
        assertEquals(3, clients.size());

        clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                                .setMemberCodeSearch("m2")
                                .setMemberClassSearch("foo")
                ));
        assertEquals(0, clients.size());

        clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                                .setMemberCodeSearch("m1")
                                .setMemberClassSearch("foo")
                ));
        assertEquals(1, clients.size());

        // instance
        clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                                .setInstanceSearch("e")
                ));
        assertEquals(CLIENTS_TOTAL_COUNT, clients.size());

        clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                                .setInstanceSearch("instance2")
                ));
        assertEquals(1, clients.size());

        clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                                .setMemberCodeSearch("m1")
                                .setMemberClassSearch("gov")
                                .setInstanceSearch("test")
                ));
        assertEquals(2, clients.size());

        // subsystemCode
        clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                                .setSubsystemCodeSearch("s1")
                ));
        assertEquals(1, clients.size());

        clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                                .setSubsystemCodeSearch("s1")
                                .setMemberCodeSearch("m1")
                ));
        assertEquals(1, clients.size());

        clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                                .setSubsystemCodeSearch("s1")
                                .setMemberCodeSearch("m2")
                ));
        assertEquals(0, clients.size());

        // clientType
        clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                                .setClientType(XRoadObjectType.MEMBER)
                ));
        assertEquals(MEMBERS_TOTAL_COUNT, clients.size());

        clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                                .setClientType(XRoadObjectType.SUBSYSTEM)
                ));
        assertEquals(SUBSYSTEMS_TOTAL_COUNT, clients.size());

        clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                                .setClientType(XRoadObjectType.MEMBER)
                                .setMemberCodeSearch("m1")
                ));
        assertEquals(3, clients.size());

        try {
            repository.findAll(
                    repository.multiParameterSearch(
                            new ClientService.SearchParameters()
                                    .setClientType(XRoadObjectType.SERVER)
                    ));
            fail("bad client type should throw exception");
        } catch (Exception expected) {
        }

        // memberName
        clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                                .setMemberNameSearch("gov")
                                .setClientType(XRoadObjectType.MEMBER)
                ));
        assertEquals(0, clients.size());

        clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                                .setMemberNameSearch("2")
                ));
        // Member2, TEST2
        assertEquals(2, clients.size());

        // combo all parameters
        clients = repository.findAll(
                repository.multiParameterSearch(
                        new ClientService.SearchParameters()
                                .setClientType(XRoadObjectType.MEMBER)
                                .setMemberCodeSearch("m1")
                                .setSecurityServerId(1000001)
                                .setMultifieldSearch("ber1")
                                .setInstanceSearch("e")
                                .setMemberNameSearch("e")
                                .setMemberClassSearch("o")
                ));
        assertEquals(1, clients.size());
    }

    @Test
    public void caseInsensitiveSort() {
        Sort.Order order = Sort.Order.by("memberName").ignoreCase();
        List<FlattenedSecurityServerClientViewEntity> clients = repository.findAll(Sort.by(order));
        int index = 0;
        assertEquals("ADMORG", clients.get(index++).getMemberName());
        assertEquals("ADMORG", clients.get(index++).getMemberName()); // subsystem
        assertEquals("ADMORG", clients.get(index++).getMemberName()); // subsystem
        assertEquals("Member1", clients.get(index++).getMemberName());
        assertEquals("Member1", clients.get(index++).getMemberName()); // subsystem
        assertEquals("Member10", clients.get(index++).getMemberName());
        assertEquals("Member11", clients.get(index++).getMemberName());
        assertEquals("Member2", clients.get(index++).getMemberName());
        assertEquals("member3", clients.get(index++).getMemberName());
        assertEquals("mEmber4", clients.get(index++).getMemberName());
    }

    @Test
    public void findClientsByMemberName() {
        String memberName = "memBer1";
        List<FlattenedSecurityServerClientViewEntity> clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.memberName(memberName));
        // 3 members and one subsystem
        assertEquals(4, clients.size());
    }

    @Test
    public void findAll() {
        List<FlattenedSecurityServerClientViewEntity> clients = repository.findAll();
        assertEquals(CLIENTS_TOTAL_COUNT, clients.size());
    }

    @Test
    public void findByType() {
        List<FlattenedSecurityServerClientViewEntity> clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.member());
        assertEquals(MEMBERS_TOTAL_COUNT, clients.size());

        clients = repository.findAll(
                JpaFlattenedSecurityServerClientRepository.subsystem());
        assertEquals(SUBSYSTEMS_TOTAL_COUNT, clients.size());
    }

}
