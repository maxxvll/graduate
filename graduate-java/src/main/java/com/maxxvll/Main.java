package com.maxxvll;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.annotation.MapperScans;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
@MapperScan("com.maxxvll.mapper")

public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class);
    }
}

