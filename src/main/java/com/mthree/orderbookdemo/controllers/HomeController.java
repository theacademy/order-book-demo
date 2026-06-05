package com.mthree.orderbookdemo.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HomeController {
	

@GetMapping("/")
public String hello() {

	try {
		System.out.println("Testing!!!");
	}
	catch(Exception e){
		// Exception handling - currently suppressed
	}
	return "Welcome to Spring Boot Application!!";
}
	

}
