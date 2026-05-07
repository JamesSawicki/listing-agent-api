package com.jimrealty.listingagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication is three annotations in one:
//   @Configuration      — this class can define beans
//   @EnableAutoConfiguration — let Spring Boot wire things up automatically
//   @ComponentScan      — scan this package for components (@Service, @Repository, etc.)
@SpringBootApplication
public class ListingAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(ListingAgentApplication.class, args);
    }
}
