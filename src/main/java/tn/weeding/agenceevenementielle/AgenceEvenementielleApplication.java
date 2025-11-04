package tn.weeding.agenceevenementielle;


import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;


@SpringBootApplication
@EnableAsync
@EnableScheduling
public class AgenceEvenementielleApplication {


    public static void main(String[] args) {
        SpringApplication.run(AgenceEvenementielleApplication.class, args);

    }

    @Bean
    public CommandLineRunner dumpEnv(org.springframework.core.env.Environment env, javax.sql.DataSource ds) {
        return args -> {
            System.out.println(">>> Active profiles: " + Arrays.toString(env.getActiveProfiles()));
            System.out.println(">>> spring.datasource.url = " + env.getProperty("spring.datasource.url"));
            System.out.println(">>> spring.datasource.username = " + env.getProperty("spring.datasource.username"));
            System.out.println(">>> DataSource impl: " + (ds != null ? ds.getClass().getName() : "null"));
        };
    }


}
