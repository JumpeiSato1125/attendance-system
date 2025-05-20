package com.example.attendanceManagementSystem.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.example.attendanceManagementSystem.service.ReviewScreenService;

@Controller
@RequestMapping("/reviewScreen")
public class ReviewScreenController {

	@Autowired
	AttendanceRecordsDao attendanceRecordsDao;
	@Autowired
	AttendanceMonthlyRecordsDao attendanceMonthlyRecordsDao;
	@Autowired
	DepartmentsDao departmentsDao;
	@Autowired
	ReviewScreenService reviewScreenService;
	@Autowired
	AttendanceHistoryService attendantHistoryService;

	@GetMapping("")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public String reviewAttendance(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(required = false) Integer year,
			@RequestParam(required = false) Integer month,
			@RequestParam(required = false) Integer departmentId,
			Model model) {

		// 年月のデフォルト値
		YearMonth targetMonth = (year != null && month != null)
				? YearMonth.of(year, month)
				: YearMonth.now();

		Pageable pageable = PageRequest.of(page, 10);
		Page<AttendanceMonthlyRecords> recordsPage;

		// 部署あり・なしで取得処理分岐
		if (departmentId != null) {
			recordsPage = attendanceMonthlyRecordsDao.findByYearAndMonthAndDepartment(
					targetMonth.getYear(), targetMonth.getMonthValue(), departmentId, pageable);
		} else {
			recordsPage = attendanceMonthlyRecordsDao.findByYearAndMonth(
					targetMonth.getYear(), targetMonth.getMonthValue(), pageable);
		}

		model.addAttribute("recordsPage", recordsPage);
		model.addAttribute("year", targetMonth.getYear());
		model.addAttribute("month", targetMonth.getMonthValue());
		model.addAttribute("selectedDepartmentId", departmentId);

		// ステータス定数
		model.addAttribute("PENDING", Status.PENDING);
		model.addAttribute("REJECTED", Status.REJECTED);
		model.addAttribute("PROGRESS", Status.PROGRESS);

		// 部署一覧プルダウン用
		model.addAttribute("departments", departmentsDao.findAll());

		return "reviewScreen";
	}

	@GetMapping("/view/{username}")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public String reviewAttendance(@PathVariable String username, Model model) {
		YearMonth currentMonth = YearMonth.now();

		Optional<AttendanceMonthlyRecords> optionalMonthly = attendanceMonthlyRecordsDao
				.findByUsernameAndYearAndMonth(username, currentMonth.getYear(), currentMonth.getMonthValue());

		List<AttendanceRecords> allRecords = new ArrayList<>();

		if (optionalMonthly.isPresent()) {
			AttendanceMonthlyRecords monthly = optionalMonthly.get();
			allRecords = attendanceRecordsDao.findByMonthly(monthly);
			model.addAttribute("monthly", monthly);
		} else {
			model.addAttribute("monthly", null);
		}

		List<LocalDate> daysInMonth = IntStream.rangeClosed(1, currentMonth.lengthOfMonth())
				.mapToObj(currentMonth::atDay)
				.collect(Collectors.toList());

		Map<LocalDate, AttendanceRecords> normalRecords = allRecords.stream()
				.filter(r -> ClockType.通常.equals(r.getClockType()))
				.collect(Collectors.toMap(
						AttendanceRecords::getWorkDate,
						r -> r,
						(r1, r2) -> r1));

		Map<LocalDate, AttendanceRecords> nightRecords = allRecords.stream()
				.filter(r -> ClockType.深夜.equals(r.getClockType()))
				.collect(Collectors.toMap(
						AttendanceRecords::getWorkDate,
						r -> r,
						(r1, r2) -> r1));

		// 勤怠集計処理の呼び出し
		AttendanceSummaryDto summary = attendantHistoryService.calculateMonthlySummary(allRecords);

		model.addAttribute("monthly", optionalMonthly.get());
		model.addAttribute("normalRecords", normalRecords);
		model.addAttribute("nightRecords", nightRecords);
		model.addAttribute("daysInMonth", daysInMonth);
		model.addAttribute("year", currentMonth.getYear());
		model.addAttribute("month", currentMonth.getMonthValue());
		model.addAttribute("targetUser", username);
		model.addAttribute("attendanceTypes", AttendanceType.values());
		model.addAttribute("mode", "reviewMode");
		if (!model.containsAttribute("attendanceHistoryBean")) {
			AttendanceHistoryBean attendanceHistoryBean = new AttendanceHistoryBean();
			model.addAttribute("createThreadBean", attendanceHistoryBean);
		}

		// モデルに集計結果を詰める
		model.addAttribute("summary", summary);

		return "attendanceHistory"; // 共通テンプレートを使用
	}

	@PostMapping("/returnComment")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public String returnComment(@RequestParam String username,
			@ModelAttribute AttendanceHistoryBean attendanceHistoryBean,
			RedirectAttributes redirectAttributes) {

		boolean updated = reviewScreenService.addReturnComent(attendanceHistoryBean.getMonthlyId(),
				attendanceHistoryBean.getReturnComment());
		if (!updated) {
			redirectAttributes.addFlashAttribute("msg", "更新するレコードが存在しません");
			return "redirect:/reviewScreen";
		}
		redirectAttributes.addFlashAttribute("msg", "コメントを更新しました");
		return "redirect:/reviewScreen/view/" + username;
	}

	@PostMapping("/application/rejected")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public String applicationRjected(@ModelAttribute AttendanceHistoryBean attendanceHistoryBean,
			RedirectAttributes redirectAttributes) {

		// ステータスを更新する
		boolean updated = reviewScreenService.updateStatuToRejected(attendanceHistoryBean.getUsername(),
				attendanceHistoryBean.getMonthlyId());
		if (!updated) {
			redirectAttributes.addFlashAttribute("msg", "更新できませんでした");
			return "redirect:/reviewScreen";
		}
		redirectAttributes.addFlashAttribute("msg", "申請しました");
		return "redirect:/reviewScreen";
	}

	@PostMapping("/application/approve")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public String applicationApprove(@ModelAttribute AttendanceHistoryBean attendanceHistoryBean,
			RedirectAttributes redirectAttributes) {

		// ステータスを更新する
		boolean updated = reviewScreenService.updateStatuToApprove(attendanceHistoryBean.getUsername(),
				attendanceHistoryBean.getMonthlyId());
		if (!updated) {
			redirectAttributes.addFlashAttribute("msg", "更新できませんでした");
			return "redirect:/reviewScreen";
		}
		redirectAttributes.addFlashAttribute("msg", "申請しました");
		return "redirect:/reviewScreen";
	}

	@GetMapping("/export")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public void exportCsv(@RequestParam int year,
			@RequestParam int month,
			@RequestParam(required = false) Integer departmentId,
			HttpServletResponse response) throws IOException {

		// ヘッダー設定
		String fileName = "attendance_summary_" + year + "_" + month + ".csv";
		response.setContentType("text/csv; charset=UTF-8");
		response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
		response.setCharacterEncoding("UTF-8");

		// 書き込み処理（Service経由）
		attendantHistoryService.writeCsv(response.getWriter(), year, month, departmentId);
	}

}