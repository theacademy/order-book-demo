package com.mthree.orderbookdemo.configs;

import com.mthree.orderbookdemo.OrderBookDemoApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

public class MyServletInitializer extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(OrderBookDemoApplication.class);
	}

} 