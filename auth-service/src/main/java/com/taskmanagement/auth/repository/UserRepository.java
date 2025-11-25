package com.taskmanagement.auth.repository;

import com.taskmanagement.auth.model.AuthProvider;
import com.taskmanagement.auth.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUserId(String userId);
    
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
    
    boolean existsByEmail(String email);
    
    boolean existsByUserId(String userId);
}