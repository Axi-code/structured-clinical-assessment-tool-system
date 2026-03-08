package com.medical.assessment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.medical.assessment.mapper")
public class AssessmentSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(AssessmentSystemApplication.class, args);
    }
}

