package com.example.libweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.example.libweb.repository") // 推荐添加，明确扫描路径
@EnableScheduling 
public class LibWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(LibWebApplication.class, args);
    }
}