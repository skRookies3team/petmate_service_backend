package com.example.petlog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing; // [중요]

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableJpaAuditing // [필수 추가] 이게 없으면 생성시간이 NULL이 되어 에러가 납니다.
public class PetlogApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetlogApplication.class, args);
    }

}