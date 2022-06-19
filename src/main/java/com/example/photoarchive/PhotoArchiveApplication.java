package com.example.photoarchive;

import com.example.photoarchive.services.ConfigProperties;
import com.vaadin.flow.component.dependency.NpmPackage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
@EnableConfigurationProperties(ConfigProperties.class)
@NpmPackage(value = "lumo-css-framework", version = "^4.0.10")
//public class PhotoArchiveApplication extends SpringBootServletInitializer implements AppShellConfigurator {
public class PhotoArchiveApplication extends SpringBootServletInitializer {
	public static void main(String[] args) {
		SpringApplication.run(PhotoArchiveApplication.class, args);
	}
}
