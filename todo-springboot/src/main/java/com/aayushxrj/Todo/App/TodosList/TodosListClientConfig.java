package com.aayushxrj.Todo.App.TodosList;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class TodosListClientConfig {

	private final HydraTokenService hydraTokenService;

	public TodosListClientConfig(HydraTokenService hydraTokenService) {
		this.hydraTokenService = hydraTokenService;
	}

	@Bean
	public RequestInterceptor todosListRequestInterceptor() {
		return template -> template.header(
				HttpHeaders.AUTHORIZATION,
				"Bearer " + hydraTokenService.getAccessToken()
		);
	}

}