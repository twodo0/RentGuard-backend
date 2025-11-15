package com.twodo0.capstoneWeb;

import com.twodo0.capstoneWeb.common.config.AiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AiProperties.class)
public class CapstoneWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(CapstoneWebApplication.class, args);
	}

}
