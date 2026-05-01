package br.com.api.auroraorg;

import org.springframework.boot.SpringApplication;

public class TestAuroraorgApplication {

    public static void main(String[] args) {
        SpringApplication.from(AuroraorgApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
