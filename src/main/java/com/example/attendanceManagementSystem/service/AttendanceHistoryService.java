package com.example.attendanceManagementSystem.service;

import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Time;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.attendanceManagementSystem.beans.AttendanceHistoryBean;
import com.example.attendanceManagementSystem.dao.AttendanceMonthlyRecordsDao;
import com.example.attendanceManagementSystem.dao.AttendanceRecordsDao;
import com.example.attendanceManagementSystem.dto.AttendanceSummaryDto;
import com.example.attendanceManagementSystem.entities.AttendanceMonthlyRecords;
import com.example.attendanceManagementSystem.entities.AttendanceRecords;
import com.example.attendanceManagementSystem.enums.AttendanceType;
import com.example.attendanceManagementSystem.enums.ClockType;
import com.example.attendanceManagementSystem.enums.Status;

@Service
public class AttendanceHistoryService {

	public final AttendanceRecordsDao attendanceRecordsDao;
	public final AttendanceMonthlyRecordsDao attendanceMonthlyRecordsDao;

	public AttendanceHistoryService(AttendanceRecordsDao attendanceRecordsDao,
			AttendanceMonthlyRecordsDao attendanceMonthlyRecordsDao) {
		this.attendanceRecordsDao = attendanceRecordsDao;
		this.attendanceMonthlyRecordsDao = attendanceMonthlyRecordsDao;
	}

	public boolean checkRecords(String username, LocalDate workDate) {
		return attendanceRecordsDao.existsByUsernameAndWorkDate(username, workDate);
	}

	public boolean updateRecords(String username, LocalDate workDate, AttendanceHistoryBean bean) {
		LocalDateTime now = LocalDateTime.now();
		int year = workDate.getYear();
		int month = workDate.getMonthValue();

		// 月の親レコード取得または作成
		AttendanceMonthlyRecords monthly = attendanceMonthlyRecordsDao
				.findByUsernameAndYearAndMonth(username, year, month)
				.orElseGet(() -> {
					AttendanceMonthlyRecords m = new AttendanceMonthlyRecords();
					m.setUsername(username);
					m.setYear(year);
					m.setMonth(month);
					m.setStatus(Status.PENDING);
					m.setSubmittedAt(now);
					m.setUpdatedAt(now);
					return attendanceMonthlyRecordsDao.save(m);
				});

		AttendanceType type = bean.getAttendanceType();
		if (type == null) {
			return false;
		}

		// 欠勤有給の場合に通常出勤・夜勤のレコードがある場合に消す。
		if (AttendanceType.有給.equals(bean.getAttendanceType()) || AttendanceType.欠勤.equals(bean.getAttendanceType())) {

			AttendanceRecords normal = attendanceRecordsDao
					.findByUsernameAndWorkDateAndClockType(username, workDate, ClockType.通常)
					.orElseGet(() -> {
						AttendanceRecords r = new AttendanceRecords();
						r.setUsername(username);
						r.setWorkDate(workDate);
						r.setClockType(ClockType.通常);
						r.setCreatedAt(now);
						return r;
					});

			normal.setAttendanceType(type);
			normal.setClockIn(null);
			normal.setClockOut(null);
			normal.setMonthly(monthly);
			normal.setUpdatedAt(now);

			attendanceRecordsDao.save(normal);

			// 深夜勤務時間が存在する場合
			Optional<AttendanceRecords> opt = attendanceRecordsDao
					.findByUsernameAndWorkDateAndClockType(username, workDate, ClockType.深夜);
			if (opt.isPresent()) {
				AttendanceRecords night = opt.get();
				// 欠勤・有給の場合は深夜データを削除
				attendanceRecordsDao.delete(night);
			}
		} else {
			// 通常出勤・有給・欠勤の登録
			AttendanceRecords normal = attendanceRecordsDao
					.findByUsernameAndWorkDateAndClockType(username, workDate, ClockType.通常)
					.orElseGet(() -> {
						AttendanceRecords r = new AttendanceRecords();
						r.setUsername(username);
						r.setWorkDate(workDate);
						r.setClockType(ClockType.通常);
						r.setCreatedAt(now);
						return r;
					});

			normal.setAttendanceType(type);
			normal.setClockIn(bean.getClockIn() != null ? Time.valueOf(bean.getClockIn()) : null);
			normal.setClockOut(bean.getClockOut() != null ? Time.valueOf(bean.getClockOut()) : null);
			normal.setMonthly(monthly);
			normal.setUpdatedAt(now);

			attendanceRecordsDao.save(normal);

			// 深夜勤務時間が入力されていた場合 
			if (type == AttendanceType.通常出勤 &&
					(bean.getNightClockIn() != null || bean.getNightClockOut() != null)) {

				AttendanceRecords night = attendanceRecordsDao
						.findByUsernameAndWorkDateAndClockType(username, workDate, ClockType.深夜)
						.orElseGet(() -> {
							AttendanceRecords r = new AttendanceRecords();
							r.setUsername(username);
							r.setWorkDate(workDate);
							r.setClockType(ClockType.深夜);
							r.setCreatedAt(now);
							return r;
						});

				night.setAttendanceType(AttendanceType.通常出勤);
				night.setClockIn(bean.getNightClockIn() != null ? Time.valueOf(bean.getNightClockIn()) : null);
				night.setClockOut(bean.getNightClockOut() != null ? Time.valueOf(bean.getNightClockOut()) : null);
				night.setMonthly(monthly);
				night.setUpdatedAt(now);

				attendanceRecordsDao.save(night);
			}
		}

		return true;
	}

	public boolean updateStatuToProgress(String username, Integer MonthlyId) {
		// usernameとMonthlyIdでレコードを取得する
		Optional<AttendanceMonthlyRecords> opt = attendanceMonthlyRecordsDao.findByUsernameAndMonthlyId(username,
				MonthlyId);
		if (!opt.isPresent())
			return false;
		try {
			AttendanceMonthlyRecords record = opt.get();
			record.setStatus(Status.PROGRESS);
			attendanceMonthlyRecordsDao.save(record);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public AttendanceSummaryDto calculateMonthlySummary(List<AttendanceRecords> records) {
		long totalWorkMinutes = 0;
		long nightWorkMinutes = 0;
		int paidLeaveDays = 0;
		int absentDays = 0;

		for (AttendanceRecords record : records) {
			AttendanceType type = record.getAttendanceType();
			ClockType clockType = record.getClockType();

			Time in = record.getClockIn();
			Time out = record.getClockOut();

			if (type == AttendanceType.有給) {
				totalWorkMinutes += 480; // 有給は8時間換算
				paidLeaveDays++;
			} else if (type == AttendanceType.欠勤) {
				absentDays++;
			} else if (in != null && out != null) {
				long minutes = Duration.between(in.toLocalTime(), out.toLocalTime()).toMinutes();
				totalWorkMinutes += minutes;

				if (clockType == ClockType.深夜) {
					nightWorkMinutes += minutes;
				}
			}
		}

		AttendanceSummaryDto dto = new AttendanceSummaryDto();
		dto.setTotalWorkHours(totalWorkMinutes / 60);
		dto.setNightWorkHours(nightWorkMinutes / 60);
		dto.setPaidLeaveDays(paidLeaveDays);
		dto.setAbsentDays(absentDays);

		return dto;
	}

	public void writeCsv(Writer writer, int year, int month, Integer departmentId) {
		List<AttendanceMonthlyRecords> monthlyList;

		if (departmentId != null) {
			monthlyList = attendanceMonthlyRecordsDao.findByYearAndMonthAndDepartment(year, month, departmentId);
		} else {
			monthlyList = attendanceMonthlyRecordsDao.findByYearAndMonth(year, month);
		}

		try (PrintWriter pw = new PrintWriter(writer)) {
			pw.println("username,氏名,年月,実労働時間,深夜労働時間");

			for (AttendanceMonthlyRecords monthly : monthlyList) {
				String username = monthly.getUsername();
				String name = monthly.getUser().getLastName() + " " + monthly.getUser().getFirstName();
				String ym = year + "/" + String.format("%02d", month);

				List<AttendanceRecords> records = attendanceRecordsDao.findByMonthly(monthly);

				long totalMin = 0;
				long nightMin = 0;

				for (AttendanceRecords r : records) {
					if (r.getAttendanceType() == AttendanceType.有給) {
						totalMin += 480;
					} else if (r.getAttendanceType() == AttendanceType.欠勤) {
						continue;
					} else if (r.getClockIn() != null && r.getClockOut() != null) {
						long min = Duration.between(
								r.getClockIn().toLocalTime(), r.getClockOut().toLocalTime()).toMinutes();
						totalMin += min;
						if (r.getClockType() == ClockType.深夜) {
							nightMin += min;
						}
					}
				}

				pw.printf("%s,%s,%s,%s,%s%n",
						username,
						name,
						ym,
						formatMinutes(totalMin),
						formatMinutes(nightMin));
			}

		} catch (Exception e) {
			throw new RuntimeException("CSV出力に失敗しました", e);
		}
	}

	private String formatMinutes(long minutes) {
		long hours = minutes / 60;
		long mins = minutes % 60;
		return String.format("%d:%02d", hours, mins);
	}
}
