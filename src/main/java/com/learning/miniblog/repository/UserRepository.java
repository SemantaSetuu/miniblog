package com.learning.miniblog.repository;

import com.learning.miniblog.entity.User;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    //here we created method basis on Email. these are not predefined method form JPA. we have created it and Spring will do rest of the work using using  naming convention mehtod.
    //Spring will generate  sql query.
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}