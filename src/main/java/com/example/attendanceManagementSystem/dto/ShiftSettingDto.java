package com.example.attendanceManagementSystem.dto;

import java.time.LocalTime;

import lombok.Data;

@Data
public class ShiftSettingDto {
	private Integer departmentId;
	private LocalTime defaultCheckIn;
	private LocalTime defaultCheckOut;
	private LocalTime nightStart;
	private LocalTime nightEnd;
	
}
