package com.leocorn.sandbox.azure;

import org.springframework.context.annotation.Bean;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.leocorn.sandbox.azure.service.HelloService;
import com.leocorn.sandbox.azure.service.DefaultHelloService;

@SpringBootApplication
public class AzureApplication implements CommandLineRunner {

    public static void main(String[] args) {

        SpringApplication.run(AzureApplication.class, args);
    }

    @Bean
    public HelloService getHelloService(){
        return  new DefaultHelloService();
    }

    @Override
    public void run(String... args) throws Exception {
        getHelloService().hello();
    }
}
