package com.zhihao.food.orderservicemanager;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.zhihao.food")
@ComponentScan("com.zhihao.food")
@EnableAsync
public class OrderServiceManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceManagerApplication.class, args);
    }
}
