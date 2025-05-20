package com.example.attendanceManagementSystem;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class AttendanceManagementSystem extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        // WAR展開時に呼び出される
        return application.sources(AttendanceManagementSystem.class);
    }

    public static void main(String[] args) {
        // ローカル起動時はこちらが使われる
        SpringApplication.run(AttendanceManagementSystem.class, args);
    }
}