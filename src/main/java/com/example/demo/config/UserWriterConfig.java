package com.example.demo.config;

import com.example.demo.model.User;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserWriterConfig {

//    @Bean
//    public JpaItemWriter<User> writer(EntityManagerFactory emf) {
//        return new JpaItemWriterBuilder<User>()
//                .entityManagerFactory(emf)
//                .build();
//    }

}
