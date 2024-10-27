package com.bytedance.douyinclouddemo.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

 @Configuration
 @EnableTransactionManagement
 @EntityScan(basePackages = "com.bytedance.douyinclouddemo.entity")
 @EnableJpaRepositories(basePackages = "com.bytedance.douyinclouddemo.repository")
public class DatabaseConfig {
    
}
