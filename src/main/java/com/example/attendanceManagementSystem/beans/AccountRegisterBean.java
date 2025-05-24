package com.example.attendanceManagementSystem.beans;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class AccountRegisterBean {
	
	@NotEmpty
	private String username;
	@NotEmpty
	private String lastName;
	@NotEmpty
	private String firstName;
	@NotNull
	private Integer departmentId;
	@NotNull
	private Integer roleId;
	@NotEmpty
	private String email;
	
}
