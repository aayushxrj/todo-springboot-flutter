package com.aayushxrj.Todo.App.TodosList;

import feign.Feign;
import feign.RequestInterceptor;
import feign.Target;
import feign.jackson.JacksonDecoder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class TodosListClientConfig {

	@Bean
	public RequestInterceptor todosListRequestInterceptor() {
		return template -> {
			ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
			if (requestAttributes == null) {
				return;
			}

			HttpServletRequest request = requestAttributes.getRequest();
			String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
			if (StringUtils.hasText(authorization)) {
				template.header(HttpHeaders.AUTHORIZATION, authorization);
			}
		};
	}

	@Bean
	public TodosListClient todosListClient(
			@Value("${todoslist.base-url}") String baseUrl,
			RequestInterceptor todosListRequestInterceptor
	) {
		return Feign.builder()
				.requestInterceptor(todosListRequestInterceptor)
				.decoder(new JacksonDecoder())
				.target(new Target.HardCodedTarget<>(TodosListClient.class, "todosListClient", baseUrl));
	}
}