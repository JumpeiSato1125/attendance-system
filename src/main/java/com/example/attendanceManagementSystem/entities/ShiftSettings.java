package com.example.attendanceManagementSystem.entities;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "shift_settings")
public class ShiftSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Integer settingId;

    @Column(name = "username", length=50)
    private String username;

    @Column(name = "department_id")
    private Integer departmentId;

    @Column(name = "default_check_in")
    private LocalTime defaultCheckIn;

    @Column(name = "default_check_out")
    private LocalTime defaultCheckOut;

    @Column(name = "night_start")
    private LocalTime nightStart;

    @Column(name = "night_end")
    private LocalTime nightEnd;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "created_at", insertable = false, updatable = false)
    private java.sql.Timestamp createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private java.sql.Timestamp updatedAt;

    // --- Getter & Setter ---

    public Integer getSettingId() {
        return settingId;
    }

    public void setSettingId(Integer settingId) {
        this.settingId = settingId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public LocalTime getDefaultCheckIn() {
        return defaultCheckIn;
    }

    public void setDefaultCheckIn(LocalTime defaultCheckIn) {
        this.defaultCheckIn = defaultCheckIn;
    }

    public LocalTime getDefaultCheckOut() {
        return defaultCheckOut;
    }

    public void setDefaultCheckOut(LocalTime defaultCheckOut) {
        this.defaultCheckOut = defaultCheckOut;
    }

    public LocalTime getNightStart() {
        return nightStart;
    }

    public void setNightStart(LocalTime nightStart) {
        this.nightStart = nightStart;
    }

    public LocalTime getNightEnd() {
        return nightEnd;
    }

    public void setNightEnd(LocalTime nightEnd) {
        this.nightEnd = nightEnd;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public void setCreatedAt(java.sql.Timestamp createdAt) {
		this.createdAt = createdAt;
	}

	public java.sql.Timestamp getCreatedAt() {
        return createdAt;
    }

	public void setUpdatedAt(java.sql.Timestamp updatedAt) {
		this.updatedAt = updatedAt;
	}
	
    public java.sql.Timestamp getUpdatedAt() {
        return updatedAt;
    }
}
