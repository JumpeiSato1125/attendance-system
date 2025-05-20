package com.example.attendanceManagementSystem.dao;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.attendanceManagementSystem.entities.ShiftSettings;

public interface ShiftSettingsDao extends JpaRepository<ShiftSettings, Integer> { 
	
	Optional<ShiftSettings> findTopByUsernameAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(String username, LocalDate date);

}
