package com.example.attendanceManagementSystem.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.attendanceManagementSystem.entities.AttendanceMonthlyRecords;
import com.example.attendanceManagementSystem.entities.AttendanceRecords;
import com.example.attendanceManagementSystem.enums.ClockType;

public interface AttendanceRecordsDao extends JpaRepository<AttendanceRecords, Integer>{
	
	List<AttendanceRecords> findByMonthly(AttendanceMonthlyRecords monthly);
	
	Optional<AttendanceRecords> findByUsernameAndWorkDateAndClockType(String username, 
				LocalDate workDate, ClockType clockType);
	
	boolean existsByUsernameAndWorkDate(String username, LocalDate workDate);
	
	boolean existsByUsernameAndWorkDateAndClockType(String username, 
				LocalDate workDate, ClockType clockType);
	
	List<AttendanceRecords> findByUsernameAndWorkDateBetween(String username, LocalDate startDate, LocalDate endDate);
	
}

