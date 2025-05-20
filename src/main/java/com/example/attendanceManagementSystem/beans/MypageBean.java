package com.example.attendanceManagementSystem.beans;

import java.time.LocalTime;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class MypageBean {
	
	
	@NotNull(message ="部署を選択してください")
	private Integer departmentId;
	@NotEmpty(message ="emailを入力してください")
	private String email;
	@NotNull(message ="シフトを選択してください")
	private Integer shiftDepartmentId;
	@NotNull(message ="業務開始時刻を入力してください")
	private LocalTime defaultCheckIn;
	@NotNull(message ="業務終了時刻を入力してください")
	private LocalTime defaultCheckOut;
	@NotNull(message ="深夜開始時刻を入力してください")
	private LocalTime nightStart;
	@NotNull(message ="深夜終了時刻を入力してください")
	private LocalTime nightEnd;
	
}
