package com.example.attendanceManagementSystem.dto;

import lombok.Data;

@Data
public class UserDto {
	private String username;
	private String lastName;
	private String firstName;
	private Integer departmentId;
	private String email;
}
