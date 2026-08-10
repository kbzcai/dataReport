package com.sjtb.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DataReportingApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataReportingApplication.class, args);
    }
}
