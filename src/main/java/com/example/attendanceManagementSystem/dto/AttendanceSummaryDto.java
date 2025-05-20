package com.example.attendanceManagementSystem.dto;

import lombok.Data;

@Data
public class AttendanceSummaryDto {
	private long totalWorkHours;
    private long nightWorkHours;
    private int paidLeaveDays;
    private int absentDays;
}
