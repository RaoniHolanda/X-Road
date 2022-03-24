/**
 * The MIT License
 *
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.centralserver.restapi.entity;
// Generated Feb 16, 2021 11:14:33 AM by Hibernate Tools 5.4.20.Final

import ee.ria.xroad.common.identifier.ClientId;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entity representing X-Road Member
 */
@Entity
@DiscriminatorValue("XRoadMember")
public class XRoadMember extends SecurityServerClient {

    private MemberClass memberClass;
    private String memberCode;
    private String name;
    private String administrativeContact;
    private Set<SecurityServer> ownedServers = new HashSet<>(0);
    private Set<Subsystem> subsystems = new HashSet<>(0);

    protected XRoadMember() {
        //JPA
    }

    public XRoadMember(ClientId identifier, MemberClass memberClass) {
        super(identifier);
        if (!Objects.equals(identifier.getMemberClass(), memberClass.getCode())) {
            throw new IllegalArgumentException("ClientId and memberClass are not consistent");
        }
        this.memberCode = identifier.getMemberCode();
        this.memberClass = memberClass;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_class_id")
    @Access(AccessType.FIELD)
    public MemberClass getMemberClass() {
        return this.memberClass;
    }

    @Column(name = "member_code")
    @Access(AccessType.FIELD)
    public String getMemberCode() {
        return this.memberCode;
    }

    @Column(name = "name")
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "administrative_contact")
    public String getAdministrativeContact() {
        return this.administrativeContact;
    }

    public void setAdministrativeContact(String administrativeContact) {
        this.administrativeContact = administrativeContact;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "owner", cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public Set<SecurityServer> getOwnedServers() {
        return this.ownedServers;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "xroadMember", cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public Set<Subsystem> getSubsystems() {
        return this.subsystems;
    }
}
