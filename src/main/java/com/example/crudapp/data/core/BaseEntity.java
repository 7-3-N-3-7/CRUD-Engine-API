package com.example.crudapp.data.core;

import com.example.crudapp.infrastructure.annotations.Children;
import com.example.crudapp.infrastructure.annotations.CrudResource;
import com.example.crudapp.infrastructure.annotations.Parent;
import com.example.crudapp.infrastructure.mapping.MappingCache;
import com.example.crudapp.infrastructure.mapping.MappingCache.RecordMapping;
import jakarta.persistence.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * [DATA LAYER]
 * Base entity providing JPA identity, hierarchical relationships, and optimized record mapping.
 */
@Entity
@Table(name = "baseentity")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Parent
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private BaseEntity parent;

    @Children
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<BaseEntity> children = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BaseEntity getParent() { return parent; }
    public void setParent(BaseEntity parent) { this.parent = parent; }
    public List<BaseEntity> getChildren() { return children; }
    public void setChildren(List<BaseEntity> children) { this.children = children; }

    /**
     * Helper to get the grandparent of this entity.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public Optional<BaseEntity> getGrandparent() {
        return Optional.ofNullable(parent).map(BaseEntity::getParent);
    }

    /**
     * Helper to get all grandchildren of this entity.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public List<BaseEntity> getGrandchildren() {
        return children.stream()
                .flatMap(child -> child.getChildren().stream())
                .collect(Collectors.toList());
    }
}
