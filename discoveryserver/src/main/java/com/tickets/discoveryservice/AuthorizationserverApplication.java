package com.tickets.discoveryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class AuthorizationserverApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthorizationserverApplication.class, args);
	}

}
