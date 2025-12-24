package com.org.api_web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication // 包含数据源配置以支持MyBatis
@ComponentScan(basePackages = {"com.org.api_common", "com.org.api_service", "com.org.api_admin_service", "com.org.api_web"})
@MapperScan(basePackages = {"com.org.api_service.mapper", "com.org.api_admin_service.mapper"})
@EnableScheduling  // 添加这个注解，启用定时任务
public class ApiWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiWebApplication.class, args);
    }

}
