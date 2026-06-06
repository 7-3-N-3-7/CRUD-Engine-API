package com.org73n37.crudapp.data.core;

import com.org73n37.crudapp.infrastructure.annotations.Children;
import com.org73n37.crudapp.infrastructure.annotations.Parent;
import com.org73n37.crudapp.infrastructure.security.TenantContext;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * [DATA LAYER]
 * Base entity providing JPA identity, hierarchical relationships, JPA auditing,
 * optimistic locking, multi-tenancy, and dynamic runtime attributes.
 */
@Entity
@Table(name = "baseentity")
@Inheritance(strategy = InheritanceType.JOINED)
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    // Auditing fields
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedBy
    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;


    // Single-Database Multi-Tenancy field
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    // Dynamic Runtime Attributes
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "entity_attributes", joinColumns = @JoinColumn(name = "entity_id"))
    @MapKeyColumn(name = "attribute_key")
    @Column(name = "attribute_value")
    private Map<String, String> attributes = new HashMap<>();

    @Parent
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private BaseEntity parent;

    @Children
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<BaseEntity> children = new ArrayList<>();

    @PrePersist
    @PreUpdate
    public void setTenantId() {
        if (this.tenantId == null) {
            String current = TenantContext.getTenantId();
            this.tenantId = (current != null) ? current : "default";
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public String getLastModifiedBy() { return lastModifiedBy; }
    public void setLastModifiedBy(String lastModifiedBy) { this.lastModifiedBy = lastModifiedBy; }

    public LocalDateTime getLastModifiedDate() { return lastModifiedDate; }
    public void setLastModifiedDate(LocalDateTime lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public Map<String, String> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        return attributes;
    }
    public void setAttributes(Map<String, String> attributes) {
        this.attributes = (attributes != null) ? attributes : new HashMap<>();
    }

    public BaseEntity getParent() { return parent; }
    public void setParent(BaseEntity parent) { this.parent = parent; }

    public List<BaseEntity> getChildren() { return children; }
    public void setChildren(List<BaseEntity> children) { this.children = children; }

    @JsonIgnore
    public Optional<BaseEntity> getGrandparent() {
        return Optional.ofNullable(parent).map(BaseEntity::getParent);
    }

    @JsonIgnore
    public List<BaseEntity> getGrandchildren() {
        return children.stream()
                .flatMap(child -> child.getChildren().stream())
                .collect(Collectors.toList());
    }
}
