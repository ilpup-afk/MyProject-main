package com.example.demo.model;

import java.util.Set;

import org.springframework.security.core.GrantedAuthority;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(uniqueConstraints = {
    @jakarta.persistence.UniqueConstraint(columnNames = 
    {"resource", "operation"})})
public class Permission implements GrantedAuthority {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String resource;
    @Column(nullable = false)
    private String operation;

    @ManyToMany(mappedBy = "permissions")
    private Set<Role> roles;
    
    public Permission(String resource, String operation) {
        this.resource = resource;
        this.operation = operation;
    }

    @Override
    public String getAuthority() {
        return String.format(
            "%s_%s", 
            resource.toUpperCase(),
            operation.toUpperCase());
    }
    
}