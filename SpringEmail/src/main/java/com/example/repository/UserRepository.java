package com.example.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.example.model.User;

@Repository("userRepository")
public interface UserRepository extends CrudRepository<User, Integer> {
	
	User findByEmail(String email);
	
    User findByConfirmationToken(String confirmationToken);

}
