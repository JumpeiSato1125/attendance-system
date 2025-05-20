package com.example.attendanceManagementSystem.service;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.attendanceManagementSystem.beans.AccountRegisterBean;
import com.example.attendanceManagementSystem.beans.MypageBean;
import com.example.attendanceManagementSystem.dao.DepartmentsDao;
import com.example.attendanceManagementSystem.dao.RolesDao;
import com.example.attendanceManagementSystem.dao.ShiftSettingsDao;
import com.example.attendanceManagementSystem.dao.UsersDao;
import com.example.attendanceManagementSystem.dto.DepartmentsDto;
import com.example.attendanceManagementSystem.dto.RolesDto;
import com.example.attendanceManagementSystem.dto.ShiftSettingDto;
import com.example.attendanceManagementSystem.dto.UserDto;
import com.example.attendanceManagementSystem.entities.Departments;
import com.example.attendanceManagementSystem.entities.Roles;
import com.example.attendanceManagementSystem.entities.ShiftSettings;
import com.example.attendanceManagementSystem.entities.Users;

@Service
public class RegisterService {

	private final RolesDao rolesDao;
	private final DepartmentsDao departmentsDao;
	private final UsersDao usersDao;
	private final ShiftSettingsDao shiftSettingsDao;
	private final PasswordEncoder passwordEncoder;
	private final JavaMailSender mailSender;

	public RegisterService(RolesDao rolesDao, DepartmentsDao departmentsDao,
			UsersDao usersDao, PasswordEncoder passwordEncoder, JavaMailSender mailSender,
			ShiftSettingsDao shiftSettingsDao) {
		this.rolesDao = rolesDao;
		this.departmentsDao = departmentsDao;
		this.usersDao = usersDao;
		this.shiftSettingsDao = shiftSettingsDao;
		this.passwordEncoder = passwordEncoder;
		this.mailSender = mailSender;
	}

	public UserDto selectUser(String username) {
		Optional<Users> users = usersDao.findById(username);
		Users user = users.get();
		UserDto ud = new UserDto();
		ud.setUsername(user.getUsername());
		ud.setLastName(user.getLastName());
		ud.setFirstName(user.getFirstName());
		ud.setEmail(user.getEmail());
		Departments dept = user.getDepartment();
		if (dept != null) {
		    ud.setDepartmentId(dept.getDepartmentId());
		} else {
			ud.setDepartmentId(null);
		}
		return ud;
	}

	public ShiftSettingDto selecShiftSetting(String username) {
		LocalDateTime now = LocalDateTime.now();
		LocalDate workDate = now.toLocalDate();
		Optional<ShiftSettings> opt = 
				shiftSettingsDao.findTopByUsernameAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(username, workDate);
		ShiftSettings shift = opt.get();
		ShiftSettingDto sd = new ShiftSettingDto();
		sd.setDefaultCheckIn(shift.getDefaultCheckIn());
		sd.setDefaultCheckOut(shift.getDefaultCheckOut());
		sd.setDepartmentId(shift.getDepartmentId());
		sd.setNightStart(shift.getNightStart());
		sd.setNightEnd(shift.getNightEnd());
		return sd;
	}

	public List<RolesDto> selectRoles() {
		List<RolesDto> rd = new ArrayList<RolesDto>();
		List<Roles> rolesList = rolesDao.findAll();
		for (Roles list : rolesList) {
			RolesDto dto = new RolesDto();
			dto.setRoleId(list.getRoleId());
			dto.setRoleName(list.getRoleName());
			dto.setDisplayName(list.getDisplayName());
			rd.add(dto);
		}
		// 管理者を除外
		List<RolesDto> filtered = new ArrayList<>();
		for (RolesDto role : rd) {
			if (!"ROLE_ADMIN".equals(role.getRoleName())) {
				filtered.add(role);
			}
		}
		return filtered;
	}

	public List<DepartmentsDto> selectDepartments() {
		List<DepartmentsDto> dd = new ArrayList<DepartmentsDto>();
		List<Departments> departmentsList = departmentsDao.findAll();
		for (Departments list : departmentsList) {
			DepartmentsDto dto = new DepartmentsDto();
			dto.setDepartmentId(list.getDepartmentId());
			dto.setName(list.getName());
			dto.setTeamName(list.getTeamName());
			dd.add(dto);
		}
		return dd;
	}

	public boolean registerAccount(AccountRegisterBean accountRegisterBean) {

		// 社員番号でレコードを検索、存在したらfalseを返す
		Optional<Users> user = usersDao.findById(accountRegisterBean.getUsername());
		if (user.isPresent()) {
			return false;
		}
		//　emaliでレコードを検索、存在したらfalseを返す
		Optional<Users> userByEmail = usersDao.findByEmail(accountRegisterBean.getEmail());
		if (userByEmail.isPresent()) {
			return false;
		}

		// usersテーブルに登録する。
		Departments department = departmentsDao.findById(accountRegisterBean.getDepartmentId())
				.orElseThrow(
						() -> new IllegalArgumentException("部署が見つかりません: ID=" + accountRegisterBean.getDepartmentId()));

		Roles role = rolesDao.findById(accountRegisterBean.getRoleId())
				.orElseThrow(() -> new IllegalArgumentException("ロールが見つかりません: ID=" + accountRegisterBean.getRoleId()));

		// パスワード生成
		String rawPassword = generatePassword(8);
		String hashedPassword = passwordEncoder.encode(rawPassword);

		Users record = new Users();
		record.setUsername(accountRegisterBean.getUsername());
		record.setPassword(hashedPassword);
		record.setFirstName(accountRegisterBean.getFirstName());
		record.setLastName(accountRegisterBean.getLastName());
		record.setEmail(accountRegisterBean.getEmail());
		record.setDepartment(department);
		record.setRoles(role);
		record.setEnabled(true);
		record.setDeleteFlg(false);
		record.setCreatedAt(LocalDateTime.now());
		record.setUpdatedAt(LocalDateTime.now());

		usersDao.save(record);

		// メール送信
		try {
			sendPasswordMail(accountRegisterBean.getEmail(), accountRegisterBean.getUsername(), rawPassword);
		} catch (Exception e) {
			// ログだけ残して処理を続ける（アカウント登録は成功とみなす）
			System.err.println("メール送信に失敗しました: " + e.getMessage());
			e.printStackTrace();
		}

		return true;
	}
	
	@Transactional
	public boolean updateAccount(String username, MypageBean mypageBean) {
		Optional<Users> optUser = usersDao.findById(username);
		if (optUser.isEmpty()) {
			return false;
		}

		Users user = optUser.get();
		user.setEmail(mypageBean.getEmail());
		// 部署の更新
		Departments department = departmentsDao.findById(mypageBean.getDepartmentId())
				.orElse(null);
		user.setDepartment(department);
		user.setUpdatedAt(LocalDateTime.now());
		usersDao.save(user);

		// シフト設定の更新（最新の有効なシフトを取得）
		Optional<ShiftSettings> shiftOpt = shiftSettingsDao
			.findTopByUsernameAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
				username, LocalDate.now());

		if (shiftOpt.isPresent()) {
			ShiftSettings shift = shiftOpt.get();
			shift.setDepartmentId(mypageBean.getShiftDepartmentId()); // ※別部署IDの場合
			shift.setDefaultCheckIn(mypageBean.getDefaultCheckIn());
			shift.setDefaultCheckOut(mypageBean.getDefaultCheckOut());
			shift.setNightStart(mypageBean.getNightStart());
			shift.setNightEnd(mypageBean.getNightEnd());
			shift.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

			shiftSettingsDao.save(shift);
		} else {
			// 新規シフト作成
			ShiftSettings shift = new ShiftSettings();
			shift.setUsername(username);
			shift.setDepartmentId(mypageBean.getShiftDepartmentId());
			shift.setDefaultCheckIn(mypageBean.getDefaultCheckIn());
			shift.setDefaultCheckOut(mypageBean.getDefaultCheckOut());
			shift.setNightStart(mypageBean.getNightStart());
			shift.setNightEnd(mypageBean.getNightEnd());
			shift.setEffectiveFrom(LocalDate.now());
			shift.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));

			shiftSettingsDao.save(shift);
		}

		return true;
	}
	
	public MypageBean toMypageBean(String username, UserDto ud, ShiftSettingDto sd) {
		
	    MypageBean bean = new MypageBean();
	    bean.setEmail(ud.getEmail());
	    bean.setDepartmentId(ud.getDepartmentId());
	    bean.setShiftDepartmentId(sd.getDepartmentId());
	    bean.setDefaultCheckIn(sd.getDefaultCheckIn());
	    bean.setDefaultCheckOut(sd.getDefaultCheckOut());
	    bean.setNightStart(sd.getNightStart());
	    bean.setNightEnd(sd.getNightEnd());

	    return bean;
	}
	
	// パスワード生成
	public String generatePassword(int length) {
		String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		SecureRandom random = new SecureRandom();
		StringBuilder password = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			password.append(chars.charAt(random.nextInt(chars.length())));
		}
		return password.toString();
	}

	public void sendPasswordMail(String toEmail, String username, String rawPassword) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(toEmail);
		message.setSubject("アカウント登録完了のお知らせ");
		message.setText("以下の内容でアカウントが登録されました。\n\n"
				+ "ユーザー名: " + username + "\n"
				+ "パスワード: " + rawPassword + "\n\n"
				+ "ログイン後、パスワードの変更をおすすめします。");
		mailSender.send(message);
	}
}
