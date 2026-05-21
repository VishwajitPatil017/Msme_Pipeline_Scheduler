package com.msme_pipeline_scheduler;

import org.springframework.boot.SpringApplication;

/**
 * Launcher stub kept for backwards compatibility. The main Spring Boot application class
 * has been renamed to MsmePipelineScheduler. This launcher delegates to it so any
 * scripts referencing DemoApplication continue to work.
 */
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsmePipelineScheduler.class, args);
	}

}
