package com.example.texshorts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan("com.example.texshorts.entity")
public class TexshortsApplication {

	public static void main(String[] args) {
		SpringApplication.run(TexshortsApplication.class, args);
	}

}
