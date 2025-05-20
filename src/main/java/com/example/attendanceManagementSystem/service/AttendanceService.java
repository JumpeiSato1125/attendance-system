package com.example.attendanceManagementSystem.service;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.attendanceManagementSystem.dao.AttendanceMonthlyRecordsDao;
import com.example.attendanceManagementSystem.dao.AttendanceRecordsDao;
import com.example.attendanceManagementSystem.dao.ShiftSettingsDao;
import com.example.attendanceManagementSystem.entities.AttendanceMonthlyRecords;
import com.example.attendanceManagementSystem.entities.AttendanceRecords;
import com.example.attendanceManagementSystem.entities.ShiftSettings;
import com.example.attendanceManagementSystem.enums.AttendanceType;
import com.example.attendanceManagementSystem.enums.ClockType;
import com.example.attendanceManagementSystem.enums.Status;

@Service
public class AttendanceService {

	private final AttendanceRecordsDao attendanceRecordsDao;
	private final AttendanceMonthlyRecordsDao attendanceMonthlyRecordsDao;
	private final ShiftSettingsDao shiftSettingsDao;

	public AttendanceService(AttendanceRecordsDao attendanceRecordsDao,
			AttendanceMonthlyRecordsDao attendanceMonthlyRecordsDao,
			ShiftSettingsDao shiftSettingsDao) {
		this.attendanceRecordsDao = attendanceRecordsDao;
		this.attendanceMonthlyRecordsDao = attendanceMonthlyRecordsDao;
		this.shiftSettingsDao = shiftSettingsDao;
	}

	public boolean checkStart(String username) {
		LocalDate today = LocalDate.now();
		LocalDate yesterday = today.minusDays(1);

		// 今日の通常勤務を確認
		boolean hasNormal = attendanceRecordsDao
				.findByUsernameAndWorkDateAndClockType(username, today, ClockType.通常)
				.map(record -> record.getClockIn() != null)
				.orElse(false);

		// 昨日の深夜勤務を確認
		boolean hasNightYesterday = attendanceRecordsDao
				.findByUsernameAndWorkDateAndClockType(username, yesterday, ClockType.深夜)
				.map(record -> record.getClockIn() != null)
				.orElse(false);

		// 今日の深夜勤務を確認（例: 25時〜などの開始）
		boolean hasNightToday = attendanceRecordsDao
				.findByUsernameAndWorkDateAndClockType(username, today, ClockType.深夜)
				.map(record -> record.getClockIn() != null)
				.orElse(false);

		return hasNormal || hasNightYesterday || hasNightToday;
	}

	public boolean checkEnd(String username) {
		LocalDate today = LocalDate.now();
		LocalDate yesterday = today.minusDays(1);

		// 今日の通常勤務を確認
		boolean hasNormal = attendanceRecordsDao
				.findByUsernameAndWorkDateAndClockType(username, today, ClockType.通常)
				.map(record -> record.getClockOut() != null)
				.orElse(false);

		// 昨日の深夜勤務を確認
		boolean hasNightYesterday = attendanceRecordsDao
				.findByUsernameAndWorkDateAndClockType(username, yesterday, ClockType.深夜)
				.map(record -> record.getClockOut() != null)
				.orElse(false);

		// 今日の深夜勤務を確認
		boolean hasNightToday = attendanceRecordsDao
				.findByUsernameAndWorkDateAndClockType(username, today, ClockType.深夜)
				.map(record -> record.getClockOut() != null)
				.orElse(false);

		return hasNormal || hasNightYesterday || hasNightToday;
	}

	public boolean punchInStart(String username) {
		LocalDateTime now = LocalDateTime.now();
		LocalDate workDate = now.toLocalDate();
		LocalTime currentTime = now.toLocalTime();

		Optional<ShiftSettings> shiftOpt = shiftSettingsDao
				.findTopByUsernameAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(username, workDate);

		if (shiftOpt.isEmpty())
			return false;
		ShiftSettings shift = shiftOpt.get();

		boolean isNight = isNightTime(currentTime, shift.getNightStart(), shift.getNightEnd());
		ClockType clockType = isNight ? ClockType.深夜 : ClockType.通常;

		if (isNight && currentTime.isBefore(shift.getNightEnd())) {
			workDate = workDate.minusDays(1);
		}

		boolean alreadyExists = attendanceRecordsDao
				.existsByUsernameAndWorkDateAndClockType(username, workDate, clockType);
		if (alreadyExists)
			return false;

		LocalTime checkInTime;
		AttendanceType attendaceType;
		if (currentTime.isAfter(shift.getNightStart().minusHours(1))) {
			clockType = ClockType.深夜;
			attendaceType = AttendanceType.通常出勤;
			checkInTime = currentTime.isBefore(shift.getNightStart()) ? shift.getNightStart() : currentTime;

			if (currentTime.isBefore(shift.getNightEnd())) {
				workDate = workDate.minusDays(1);
			}
		} else {
			clockType = ClockType.通常;
			attendaceType = AttendanceType.通常出勤;
			checkInTime = currentTime.isBefore(shift.getDefaultCheckIn())
					? shift.getDefaultCheckIn()
					: currentTime;
		}

		// 年月を取得
		int year = workDate.getYear();
		int month = workDate.getMonthValue();

		// 親レコード（月）を取得 or 作成
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

		// 勤怠（子）レコード作成
		AttendanceRecords record = new AttendanceRecords();
		record.setUsername(username);
		record.setWorkDate(workDate);
		record.setClockType(clockType);
		record.setClockIn(Time.valueOf(checkInTime));
		record.setAttendanceType(attendaceType);
		record.setDepartmentId(shift.getDepartmentId());
		record.setCreatedAt(now);
		record.setUpdatedAt(now);
		record.setMonthly(monthly); // ← 親と紐づけ

		attendanceRecordsDao.save(record);
		return true;
	}

	public boolean punchInEnd(String username) {
		LocalDateTime now = LocalDateTime.now();
		LocalDate workDate = now.toLocalDate();
		LocalTime currentTime = now.toLocalTime();

		// シフト設定取得（最新の有効な設定）
		Optional<ShiftSettings> shiftOpt = shiftSettingsDao
				.findTopByUsernameAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(username, workDate);

		if (shiftOpt.isEmpty())
			return false;

		ShiftSettings shift = shiftOpt.get();
		boolean isNight = isNightTime(currentTime, shift.getNightStart(), shift.getNightEnd());

		// 深夜時間帯に日付を補正する
		if (isNight && currentTime.isBefore(shift.getNightEnd())) {
			workDate = workDate.minusDays(1);
		}

		boolean updated = false;

		// ① 通常勤務の clock_out を更新
		AttendanceType attendanceType;
		Optional<AttendanceRecords> normalRecordOpt = attendanceRecordsDao
				.findByUsernameAndWorkDateAndClockType(username, workDate, ClockType.通常);
		if (normalRecordOpt.isPresent() && normalRecordOpt.get().getClockOut() == null) {
			attendanceType = AttendanceType.通常出勤;
			AttendanceRecords normal = normalRecordOpt.get();
			normal.setClockOut(Time.valueOf(
					// 深夜開始時刻より前の場合現在時刻を、後の場合深夜開始時刻を終了時間に打刻する
					currentTime.isBefore(shift.getNightStart()) ? currentTime : shift.getNightStart()));
			normal.setAttendanceType(attendanceType);
			normal.setUpdatedAt(now);
			attendanceRecordsDao.save(normal);
			updated = true;
		}

		// ② 深夜勤務の clock_out を更新 or 登録
		if (isNight) {
			attendanceType = AttendanceType.通常出勤;
			Optional<AttendanceRecords> nightRecordOpt = attendanceRecordsDao
					.findByUsernameAndWorkDateAndClockType(username, workDate, ClockType.深夜);
			if (nightRecordOpt.isPresent()) {
				AttendanceRecords night = nightRecordOpt.get();
				if (night.getClockOut() == null) {
					night.setClockOut(Time.valueOf(currentTime));
					night.setAttendanceType(attendanceType);
					night.setUpdatedAt(now);
					attendanceRecordsDao.save(night);
					updated = true;
				}
			} else {
				// 深夜レコードがない場合 → 新規作成（遅れて登録した場合）
				AttendanceRecords night = new AttendanceRecords();
				night.setUsername(username);
				night.setWorkDate(workDate);
				night.setClockType(ClockType.深夜);
				night.setClockIn(Time.valueOf(shift.getNightStart())); // 始業時間未記録のため
				night.setClockOut(Time.valueOf(currentTime));
				night.setAttendanceType(attendanceType);
				night.setDepartmentId(shift.getDepartmentId());
				night.setCreatedAt(now);
				night.setUpdatedAt(now);
				attendanceRecordsDao.save(night);
				updated = true;
			}
		}

		return updated;
	}

	public boolean punchInAbsense(String username) {
		LocalDateTime now = LocalDateTime.now();
		LocalDate today = LocalDate.now();
		LocalDate workDate = now.toLocalDate();

		// 通常出勤の勤怠レコードが既に存在するかチェック
		boolean exists = attendanceRecordsDao
				.existsByUsernameAndWorkDateAndClockType(username, today, ClockType.通常);

		if (exists) {
			// 既に打刻がある場合は欠勤登録せず false
			return false;
		}

		Optional<ShiftSettings> shiftOpt = shiftSettingsDao
				.findTopByUsernameAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(username, workDate);

		if (shiftOpt.isEmpty())
			return false;

		// 年月を取得
		int year = workDate.getYear();
		int month = workDate.getMonthValue();

		// 親レコード（月）を取得 or 作成
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

		// 欠勤レコードの登録
		AttendanceRecords record = new AttendanceRecords();
		record.setUsername(username);
		record.setWorkDate(today);
		record.setClockType(ClockType.通常);
		record.setAttendanceType(AttendanceType.欠勤);
		record.setDepartmentId(shiftOpt.get().getDepartmentId());
		record.setCreatedAt(LocalDateTime.now());
		record.setUpdatedAt(LocalDateTime.now());
		record.setMonthly(monthly); // ← 親と紐づけ

		attendanceRecordsDao.save(record);
		return true;
	}

	public boolean punchInHoliday(String username) {
		LocalDateTime now = LocalDateTime.now();
		LocalDate today = LocalDate.now();
		LocalDate workDate = now.toLocalDate();

		// 通常出勤の勤怠レコードが既に存在するかチェック
		boolean exists = attendanceRecordsDao
				.existsByUsernameAndWorkDateAndClockType(username, today, ClockType.通常);

		if (exists) {
			// 既に打刻がある場合は欠勤登録せず false
			return false;
		}

		Optional<ShiftSettings> shiftOpt = shiftSettingsDao
				.findTopByUsernameAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(username, workDate);

		if (shiftOpt.isEmpty())
			return false;

		// 年月を取得
		int year = workDate.getYear();
		int month = workDate.getMonthValue();

		// 親レコード（月）を取得 or 作成
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

		// 欠勤レコードの登録
		AttendanceRecords record = new AttendanceRecords();
		record.setUsername(username);
		record.setWorkDate(today);
		record.setClockType(ClockType.通常);
		record.setAttendanceType(AttendanceType.有給);
		record.setDepartmentId(shiftOpt.get().getDepartmentId());
		record.setCreatedAt(LocalDateTime.now());
		record.setUpdatedAt(LocalDateTime.now());
		record.setMonthly(monthly); // ← 親と紐づけ

		attendanceRecordsDao.save(record);
		return true;
	}

	private boolean isNightTime(LocalTime current, LocalTime nightStart, LocalTime nightEnd) {
		// 夜時間開始が夜時間終了より前か判定　
		if (nightStart.isBefore(nightEnd)) {
			//　ex 22:00 - 23:30
			// 夜時間開始より後かつ夜時間終了より前
			return !current.isBefore(nightStart) && current.isBefore(nightEnd);
		} else {
			// ex 22:00 - 7:00
			// 夜時間開始より後または夜時間終了より前
			return !current.isBefore(nightStart) || current.isBefore(nightEnd); // 夜またぎ
		}
	}
}
