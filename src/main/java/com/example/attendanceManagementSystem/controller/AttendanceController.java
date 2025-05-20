package com.example.attendanceManagementSystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.attendanceManagementSystem.service.AttendanceService;

@Controller
@RequestMapping("/attendance")
public class AttendanceController {

	@Autowired
	AttendanceService attendantService;

	@GetMapping("/index")
	public String index(Model model) {
		return "attendance";
	}

	@PostMapping("/start")
	public ModelAndView start(ModelAndView mav, RedirectAttributes redirectAttributes) {

		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		
		// 打刻の開始時刻があるかチェック
		if (!attendantService.checkStart(username)) {
			redirectAttributes.addFlashAttribute("msg", "すでに打刻が完了しています");
			mav.setViewName("redirect:/attendance/index");
			return mav;
		}
		// 打刻する(サービスの中で通常出勤か深夜か判定)
		if (!attendantService.punchInStart(username)) {
			redirectAttributes.addFlashAttribute("msg", "更新できませんでした");
			mav.setViewName("redirect:/attendance/index");
			return mav;
		}

		redirectAttributes.addFlashAttribute("msg", "開始時刻に打刻しました");
		mav.setViewName("redirect:/attendance/index");
		return mav;
	}

	@PostMapping("/end")
	public ModelAndView send(ModelAndView mav, RedirectAttributes redirectAttributes) {

		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		// 打刻の開始時刻があるかチェック
	
		if (attendantService.checkStart(username)) {
			if (!attendantService.checkEnd(username)) {
				redirectAttributes.addFlashAttribute("msg", "すでに打刻が完了しています");
				mav.setViewName("redirect:/attendance/index");
				return mav;
			}
		} else {
			redirectAttributes.addFlashAttribute("msg", "開始時間の打刻がありません");
			mav.setViewName("redirect:/attendance/index");
			return mav;
		}
		// 打刻する(サービスの中で通常出勤か深夜か判定)
		if (!attendantService.punchInEnd(username)) {
			redirectAttributes.addFlashAttribute("msg", "更新できませんでした");
			mav.setViewName("redirect:/attendance/index");
			return mav;
		}

		redirectAttributes.addFlashAttribute("msg", "終了時刻に打刻しました");
		mav.setViewName("redirect:/attendance/index");
		return mav;
	}
	
	@PostMapping("/absense")
	public ModelAndView absense(ModelAndView mav, RedirectAttributes redirectAttributes) {

		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		// 打刻の開始時刻があるかチェック
	
		if (!attendantService.checkStart(username)) {
			redirectAttributes.addFlashAttribute("msg", "すでに打刻がしています");
			mav.setViewName("redirect:/attendance/index");
			return mav;
		}
		// 打刻する(サービスの中で通常出勤か深夜か判定)
		if (!attendantService.punchInAbsense(username)) {
			redirectAttributes.addFlashAttribute("msg", "更新できませんでした");
			mav.setViewName("redirect:/attendance/index");
			return mav;
		}

		redirectAttributes.addFlashAttribute("msg", "欠席登録しました");
		mav.setViewName("redirect:/attendance/index");
		return mav;
	}
	
	@PostMapping("/holiday")
	public ModelAndView holiday(ModelAndView mav, RedirectAttributes redirectAttributes) {

		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		// 打刻の開始時刻があるかチェック
	
		if (!attendantService.checkStart(username)) {
			redirectAttributes.addFlashAttribute("msg", "すでに打刻がしています");
			mav.setViewName("redirect:/attendance/index");
			return mav;
		}
		// 打刻する(サービスの中で通常出勤か深夜か判定)
		if (!attendantService.punchInHoliday(username)) {
			redirectAttributes.addFlashAttribute("msg", "更新できませんでした");
			mav.setViewName("redirect:/attendance/index");
			return mav;
		}

		redirectAttributes.addFlashAttribute("msg", "休日登録しました");
		mav.setViewName("redirect:/attendance/index");
		return mav;
	}
}
