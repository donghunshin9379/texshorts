package com.example.texshorts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackages = "com.example")  // 전체 스캔
@EnableScheduling
public class TexshortsApplication {

	public static void main(String[] args) {
		SpringApplication.run(TexshortsApplication.class, args);
	}

}
