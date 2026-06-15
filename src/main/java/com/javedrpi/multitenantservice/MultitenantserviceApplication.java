package com.javedrpi.multitenantservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class MultitenantserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MultitenantserviceApplication.class, args);
	}

}
