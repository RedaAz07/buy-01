package com.Media;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MediaServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MediaServiceApplication.class, args);
	}

}
