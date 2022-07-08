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

import org.hibernate.annotations.Subselect;
import org.springframework.data.annotation.Immutable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import java.util.HashSet;
import java.util.Set;

/**
 * Read-only entity representing SecurityServerClient from view flattened_security_server_client
 * type
 */
@Entity
@Immutable
// Subselect prevents table creation: https://stackoverflow.com/a/33689357
@Subselect("select * from " + FlattenedSecurityServerClient.TABLE_NAME)
@Table(name = FlattenedSecurityServerClient.TABLE_NAME)
public class FlattenedSecurityServerClient extends AuditableEntity {
    static final String TABLE_NAME = "flattened_security_server_client";

    private int id;

    private String xroadInstance;
    private MemberClass memberClass;
    private String memberCode;
    private String subsystemCode;
    private String memberName;
    private String type;

    private Set<FlattenedServerClient> flattenedServerClients = new HashSet<>();

    protected FlattenedSecurityServerClient() {
        //JPA
    }

    @Id
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "xroad_instance")
    public String getXroadInstance() {
        return this.xroadInstance;
    }

    public void setXroadInstance(String xroadInstance) {
        this.xroadInstance = xroadInstance;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_class_id")
    public MemberClass getMemberClass() {
        return this.memberClass;
    }

    public void setMemberClass(MemberClass memberClass) {
        this.memberClass = memberClass;
    }

    @Column(name = "member_code")
    public String getMemberCode() {
        return this.memberCode;
    }

    public void setMemberCode(String memberCode) {
        this.memberCode = memberCode;
    }

    @Column(name = "member_name")
    public String getMemberName() {
        return this.memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    @Column(name = "type")
    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Column(name = "subsystem_code")
    public String getSubsystemCode() {
        return this.subsystemCode;
    }

    public void setSubsystemCode(String subsystemCode) {
        this.subsystemCode = subsystemCode;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "flattenedSecurityServerClient")
    public Set<FlattenedServerClient> getFlattenedServerClients() {
        return flattenedServerClients;
    }

    public void setFlattenedServerClients(
            Set<FlattenedServerClient> flattenedServerClients) {
        this.flattenedServerClients = flattenedServerClients;
    }
}


