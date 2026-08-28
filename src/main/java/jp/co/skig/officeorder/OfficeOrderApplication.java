package jp.co.skig.officeorder;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ConfigurationPropertiesScan

public class OfficeOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OfficeOrderApplication.class, args);
    }

    // ★ 追加: Springコンテナに ObjectMapper の Bean を登録します
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
}
}
