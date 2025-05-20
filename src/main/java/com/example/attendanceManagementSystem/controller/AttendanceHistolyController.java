package com.example.attendanceManagementSystem.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.attendanceManagementSystem.beans.AttendanceHistoryBean;
import com.example.attendanceManagementSystem.dao.AttendanceMonthlyRecordsDao;
import com.example.attendanceManagementSystem.dao.AttendanceRecordsDao;
import com.example.attendanceManagementSystem.dao.DepartmentsDao;
import com.example.attendanceManagementSystem.dto.AttendanceSummaryDto;
import com.example.attendanceManagementSystem.entities.AttendanceMonthlyRecords;
import com.example.attendanceManagementSystem.entities.AttendanceRecords;
import com.example.attendanceManagementSystem.enums.AttendanceType;
import com.example.attendanceManagementSystem.enums.ClockType;
import com.example.attendanceManagementSystem.enums.Status;
import com.example.attendanceManagementSystem.service.AttendanceHistoryService;
import com.example.attendanceManagementSystem.service.AttendanceService;

@Controller
@RequestMapping("/attendance")
public class AttendanceHistolyController {

	@Autowired
	AttendanceService attendantService;

	@Autowired
	AttendanceHistoryService attendantHistoryService;

	@Autowired
	AttendanceRecordsDao attendanceRecordsDao;

	@Autowired
	AttendanceMonthlyRecordsDao attendanceMonthlyRecordsDao;

	@Autowired
	DepartmentsDao departmentsDao;

	@GetMapping("/history")
	public String viewAttendance(
			@RequestParam(required = false) Integer year,
			@RequestParam(required = false) Integer month,
			Model model,
			@AuthenticationPrincipal UserDetails userDetails) {

		String username = userDetails.getUsername();
		YearMonth targetMonth = (year != null && month != null)
				? YearMonth.of(year, month)
				: YearMonth.now();

		Optional<AttendanceMonthlyRecords> optionalMonthly = attendanceMonthlyRecordsDao
				.findByUsernameAndYearAndMonth(username, targetMonth.getYear(), targetMonth.getMonthValue());

		List<AttendanceRecords> allRecords = optionalMonthly
				.map(monthly -> attendanceRecordsDao.findByMonthly(monthly))
				.orElse(Collections.emptyList());

		List<LocalDate> daysInMonth = IntStream.rangeClosed(1, targetMonth.lengthOfMonth())
				.mapToObj(targetMonth::atDay)
				.collect(Collectors.toList());

		Map<LocalDate, AttendanceRecords> normalRecords = allRecords.stream()
				.filter(r -> ClockType.通常.equals(r.getClockType()))
				.collect(Collectors.toMap(AttendanceRecords::getWorkDate, r -> r, (r1, r2) -> r1));

		Map<LocalDate, AttendanceRecords> nightRecords = allRecords.stream()
				.filter(r -> ClockType.深夜.equals(r.getClockType()))
				.collect(Collectors.toMap(AttendanceRecords::getWorkDate, r -> r, (r1, r2) -> r1));

		// 勤怠集計処理の呼び出し
		AttendanceSummaryDto summary = attendantHistoryService.calculateMonthlySummary(allRecords);

		model.addAttribute("monthly", optionalMonthly.orElse(null));
		model.addAttribute("normalRecords", normalRecords);
		model.addAttribute("nightRecords", nightRecords);
		model.addAttribute("daysInMonth", daysInMonth);
		model.addAttribute("year", targetMonth.getYear());
		model.addAttribute("month", targetMonth.getMonthValue());
		model.addAttribute("targetUser", username);
		model.addAttribute("attendanceTypes", AttendanceType.values());
		model.addAttribute("mode", "historyMode");

		// ステータス定数
		model.addAttribute("PENDING", Status.PENDING);
		model.addAttribute("REJECTED", Status.REJECTED);

		// モデルに集計結果を詰める
		model.addAttribute("summary", summary);

		return "attendanceHistory";
	}

	@PostMapping("/edit/{day}")
	public String searchPaging(@PathVariable("day") LocalDate day,
			@ModelAttribute AttendanceHistoryBean attendanceHistoryBean,
			RedirectAttributes redirectAttributes, @AuthenticationPrincipal UserDetails userDetails) {

		// usernameと日付で入力内容を更新する
		boolean updated = attendantHistoryService.updateRecords(userDetails.getUsername(), day, attendanceHistoryBean);
		if (!updated) {
			redirectAttributes.addFlashAttribute("msg", "更新に失敗しました");
			return "redirect:/attendance/history";
		}

		redirectAttributes.addFlashAttribute("msg", "更新しました");
		return "redirect:/attendance/history";
	}

	@PostMapping("/application")
	public String application(@ModelAttribute AttendanceHistoryBean attendanceHistoryBean,
			RedirectAttributes redirectAttributes, @AuthenticationPrincipal UserDetails userDetails) {

		// ステータスを更新する
		boolean updated = attendantHistoryService.updateStatuToProgress(userDetails.getUsername(),
				attendanceHistoryBean.getMonthlyId());
		if (!updated) {
			redirectAttributes.addFlashAttribute("msg", "更新できませんでした");
			return "redirect:/attendance/history";
		}
		redirectAttributes.addFlashAttribute("msg", "申請しました");
		return "redirect:/attendance/history";
	}
}