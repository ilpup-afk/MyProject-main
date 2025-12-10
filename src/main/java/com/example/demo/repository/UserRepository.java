package com.example.demo.repository;

import org.springframework.stereotype.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.User;

@Repository
public interface UserRepository 
        extends JpaRepository<User, Long> {
        Optional<User> findByUsername(String username);
}