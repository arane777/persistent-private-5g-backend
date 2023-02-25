package com.hackathon.demo;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync
public class CommandexecutionApplication {

	@Value("${async.corePoolSize}")
	private String corePoolSize;
	@Value("${async.maxPoolSize}")
	private String maxPoolSize;
	@Value("${async.queueCapacity}")
	private String queueCapacity;
	@Value("${async.threadNamePrefix}")
	private String threadNamePrefix;
	
	public static void main(String[] args) {
		SpringApplication.run(CommandexecutionApplication.class, args);
	}

	@Bean
	public Executor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(Integer.parseInt(corePoolSize));
		executor.setMaxPoolSize(Integer.parseInt(maxPoolSize));
		executor.setQueueCapacity(Integer.parseInt(queueCapacity));
		executor.setThreadNamePrefix(threadNamePrefix);
		executor.initialize();
		return executor;
	}
}

