package jp.co.skig.officeorder;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@ConfigurationPropertiesScan
// @MapperScan("jp.co.skig.officeorder.mapper")
public class OfficeOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OfficeOrderApplication.class, args);
    }
}
