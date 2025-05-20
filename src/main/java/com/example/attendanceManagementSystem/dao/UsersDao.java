package com.example.attendanceManagementSystem.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.attendanceManagementSystem.entities.Users;

public interface UsersDao extends JpaRepository<Users, String> { 
	Optional<Users> findByEmail(String email);
}
