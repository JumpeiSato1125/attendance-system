package com.example.attendanceManagementSystem.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.attendanceManagementSystem.entities.Departments;

public interface DepartmentsDao extends JpaRepository<Departments, Integer> { 
}
