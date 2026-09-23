package com.umlcase.domain.generation.java;

public record GeneratedJavaRelationship(
    String fieldName,
    String targetClassName,
    String relationshipType, // @ManyToOne, @OneToMany, @OneToOne, @ManyToMany
    String mappedBy, // null if this is the owning side
    String joinColumnName, // for @JoinColumn
    String joinTableName, // for @JoinTable
    String inverseJoinColumnName, // for @JoinTable
    boolean isComposition // cascade = CascadeType.ALL, orphanRemoval = true
) {}
