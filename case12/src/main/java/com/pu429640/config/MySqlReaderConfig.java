package com.pu429640.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pu429640.services.MySqlReader;

@Configuration
public class MySqlReaderConfig {

    @Value("${mysql.url}")
    private String mysqlUrl;

    @Value("${mysql.user}")
    private String mysqlUser;

    @Value("${mysql.password}")
    private String mysqlPassword;

    @Value("${mysql.table.name}")
    private String mysqlTableName;

    @Bean
    public MySqlReader mySqlReader() {
        return new MySqlReader(mysqlUrl, mysqlUser, mysqlPassword, mysqlTableName);
    }
}