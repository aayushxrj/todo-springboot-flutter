package com.aayushxrj.Todo.App.TodosList;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
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

}