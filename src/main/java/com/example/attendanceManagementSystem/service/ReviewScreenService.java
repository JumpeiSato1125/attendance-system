package com.example.attendanceManagementSystem.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.attendanceManagementSystem.dao.AttendanceMonthlyRecordsDao;
import com.example.attendanceManagementSystem.entities.AttendanceMonthlyRecords;
import com.example.attendanceManagementSystem.enums.Status;

@Service
public class ReviewScreenService {

	public final AttendanceMonthlyRecordsDao attendanceMonthlyRecordsDao;

	public ReviewScreenService(AttendanceMonthlyRecordsDao attendanceMonthlyRecordsDao) {
		this.attendanceMonthlyRecordsDao = attendanceMonthlyRecordsDao;
	}

	public boolean addReturnComent(Integer monthlyId, String returnComment) {
		// 更新する月にレコードが存在するか確認する
		Optional<AttendanceMonthlyRecords> opt = attendanceMonthlyRecordsDao.findById(monthlyId);
		if(!opt.isPresent()) return false;
		// コメントを追加
		AttendanceMonthlyRecords record = opt.get();
		record.setReturnComment(returnComment);
		
		attendanceMonthlyRecordsDao.save(record);
		return true;
	}
	
	public boolean updateStatuToRejected(String username, Integer MonthlyId) {
		// usernameとMonthlyIdでレコードを取得する
		Optional<AttendanceMonthlyRecords> opt = attendanceMonthlyRecordsDao.findByUsernameAndMonthlyId(username,
				MonthlyId);
		if (!opt.isPresent())
			return false;
		try {
			AttendanceMonthlyRecords record = opt.get();
			record.setStatus(Status.REJECTED);
			attendanceMonthlyRecordsDao.save(record);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean updateStatuToApprove(String username, Integer MonthlyId) {
		// usernameとMonthlyIdでレコードを取得する
		Optional<AttendanceMonthlyRecords> opt = attendanceMonthlyRecordsDao.findByUsernameAndMonthlyId(username,
				MonthlyId);
		if (!opt.isPresent())
			return false;
		try {
			AttendanceMonthlyRecords record = opt.get();
			record.setStatus(Status.APPROVE);
			attendanceMonthlyRecordsDao.save(record);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
}
