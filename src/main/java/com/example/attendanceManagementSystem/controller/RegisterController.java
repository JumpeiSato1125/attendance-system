package com.example.attendanceManagementSystem.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.attendanceManagementSystem.beans.AccountRegisterBean;
import com.example.attendanceManagementSystem.beans.MypageBean;
import com.example.attendanceManagementSystem.dto.DepartmentsDto;
import com.example.attendanceManagementSystem.dto.RolesDto;
import com.example.attendanceManagementSystem.dto.ShiftSettingDto;
import com.example.attendanceManagementSystem.dto.UserDto;
import com.example.attendanceManagementSystem.service.RegisterService;

@Controller
@RequestMapping("/register")
public class RegisterController {

	@Autowired
	RegisterService registerService;

	@GetMapping("")
	public String register(Model model) {

		// 部署を検索
		List<DepartmentsDto> dd = registerService.selectDepartments();
		// 権限を検索
		List<RolesDto> rd = registerService.selectRoles();

		model.addAttribute("dd", dd);
		model.addAttribute("rd", rd);
		return "register";
	}

	@PostMapping("")
	public String processRegister(@Valid @ModelAttribute AccountRegisterBean accountRegisterBean,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		// 入力バリデーションとアカウント登録処理
		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("accountRegisterBean", accountRegisterBean);
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.accountRegisterBean",
					bindingResult);
			return "redirect:/register";
		}

		boolean registerd = registerService.registerAccount(accountRegisterBean);
		if (!registerd) {
			redirectAttributes.addFlashAttribute("accountRegisterBean", accountRegisterBean);
			redirectAttributes.addFlashAttribute("msg", "すでに同じユーザー情報が存在します");
			return "redirect:/register";
		}

		redirectAttributes.addFlashAttribute("msg", "登録しました。");
		return "registered";
	}

	@GetMapping("/mypage")
	public String mypage(Model model, @AuthenticationPrincipal UserDetails userDetails) {

		String username = userDetails.getUsername();
		// user情報を取得
		UserDto ud = registerService.selectUser(username);
		// シフト情報を取得
		ShiftSettingDto sd = registerService.selecShiftSetting(username);
		// 部署を検索
		List<DepartmentsDto> dd = registerService.selectDepartments();

		if (!model.containsAttribute("mypageBean")) {
		    MypageBean mypageBean = registerService.toMypageBean(username, ud, sd); // 必要なら ud/sd を渡す
		    model.addAttribute("mypageBean", mypageBean);
		}
		model.addAttribute("ud", ud);
		model.addAttribute("sd", sd);
		model.addAttribute("dd", dd);
		return "mypage";
	}

	@PostMapping("/mypage/update")
	public String mypageUpdate(@ModelAttribute MypageBean mypageBean, BindingResult bindingResult,
			RedirectAttributes redirectAttributes,
			@AuthenticationPrincipal UserDetails userDetails) {

		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("mypageBeann",mypageBean);
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.mypageBean",
					bindingResult);
			return "redirect:/register/mypage";
		}
		// user情報とshiftsetting情報を登録する
		boolean updated = registerService.updateAccount(userDetails.getUsername(), mypageBean);
		if(!updated) {
			redirectAttributes.addFlashAttribute("mypageBean", mypageBean);
			redirectAttributes.addFlashAttribute("msg", "更新できませんでした");
			return "redirect:/register/mypage";
		}
		
		redirectAttributes.addFlashAttribute("msg", "更新しました");
		return "redirect:/register/mypage";
	}

}
