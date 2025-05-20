package com.example.attendanceManagementSystem.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.attendanceManagementSystem.entities.Roles;

public interface RolesDao extends JpaRepository<Roles, Integer> { 
}
