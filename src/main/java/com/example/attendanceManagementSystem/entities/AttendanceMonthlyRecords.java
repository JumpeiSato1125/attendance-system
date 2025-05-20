package com.example.attendanceManagementSystem.entities;


import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.example.attendanceManagementSystem.enums.Status;

import lombok.Data;

@Entity
@Table(name = "attendance_monthly_records", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"username", "year", "month"})
})
@Data
public class AttendanceMonthlyRecords {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "monthly_id")
    private Integer monthlyId;

    @ManyToOne
    @JoinColumn(name = "username", referencedColumnName = "username", insertable = false, updatable = false)
    private Users user;
    
    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "month", nullable = false)
    private int month;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private Status status = Status.PENDING;
    
    @Column(name = "return_comment", length = 255)
    private String returnComment;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 日ごとの勤怠と1:Nで紐づけ（双方向）
    @OneToMany(mappedBy = "monthly", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AttendanceRecords> attendanceRecordsList;
}