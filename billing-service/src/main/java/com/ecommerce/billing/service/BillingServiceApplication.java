package com.ecommerce.billing.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class BillingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BillingServiceApplication.class, args);
	}

	@Bean
	CommandLineRunner runner(Environment env) {
	    return args -> {
	        System.out.println("BOOTSTRAP = " +
	                env.getProperty("spring.kafka.bootstrap-servers"));
	    };
	}
}
