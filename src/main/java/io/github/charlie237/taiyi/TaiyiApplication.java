package io.github.charlie237.taiyi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TaiyiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaiyiApplication.class, args);
    }

}
