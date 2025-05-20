package com.example.attendanceManagementSystem.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.attendanceManagementSystem.entities.AttendanceMonthlyRecords;

public interface AttendanceMonthlyRecordsDao extends JpaRepository<AttendanceMonthlyRecords, Integer> {
	Optional<AttendanceMonthlyRecords> findByUsernameAndYearAndMonth(String username, int year, int month);

	Page<AttendanceMonthlyRecords> findByYearAndMonth(int year, int month, Pageable pageable);

	@Query("""
			    SELECT m FROM AttendanceMonthlyRecords m
			    WHERE m.year = :year
			      AND m.month = :month
			      AND m.user.department.departmentId = :departmentId
			""")
	Page<AttendanceMonthlyRecords> findByYearAndMonthAndDepartment(
			@Param("year") int year,
			@Param("month") int month,
			@Param("departmentId") int departmentId,
			Pageable pageable);

	Optional<AttendanceMonthlyRecords> findByUsernameAndMonthlyId(String username, Integer monthlyId);

	@Query("""
			    SELECT m FROM AttendanceMonthlyRecords m
			    WHERE m.year = :year
			      AND m.month = :month
			      AND m.user.department.departmentId = :departmentId
			""")
	List<AttendanceMonthlyRecords> findByYearAndMonthAndDepartment(
			@Param("year") int year,
			@Param("month") int month,
			@Param("departmentId") Integer departmentId);

	List<AttendanceMonthlyRecords> findByYearAndMonth(int year, int month);
}
