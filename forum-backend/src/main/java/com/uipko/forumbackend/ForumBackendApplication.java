package com.uipko.forumbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
    JpaRepositoriesAutoConfiguration.class,
    RedisRepositoriesAutoConfiguration.class
})
@EnableScheduling
public class ForumBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ForumBackendApplication.class, args);
	}

}
