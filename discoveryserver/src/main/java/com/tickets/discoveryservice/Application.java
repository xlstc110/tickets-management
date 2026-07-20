package com.tickets.discoveryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

//	@Bean
//	public CommandLineRunner startup(BCryptPasswordEncoder encoder) {
//		return args -> {
//			var password = encoder.encode("letmein");
//			System.out.println(password);
//		};
//	}
}
