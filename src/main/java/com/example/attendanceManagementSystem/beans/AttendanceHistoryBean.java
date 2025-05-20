package com.example.attendanceManagementSystem.beans;

import java.time.LocalTime;

import com.example.attendanceManagementSystem.enums.AttendanceType;

import lombok.Data;

@Data
public class AttendanceHistoryBean {
	private LocalTime clockIn;
	private LocalTime clockOut;
	private AttendanceType AttendanceType;
	private LocalTime nightClockIn;
	private LocalTime nightClockOut;
	private String username;
	private String returnComment;
	private Integer monthlyId;
}
