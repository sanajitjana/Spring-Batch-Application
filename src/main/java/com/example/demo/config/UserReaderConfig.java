package com.example.demo.config;

import com.example.demo.model.User;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class UserReaderConfig {

//    @Bean
//    public FlatFileItemReader<User> reader() {
//        return new FlatFileItemReaderBuilder<User>()
//                .name("userItemReader")
//                .resource(new ClassPathResource("users.csv"))
//                .delimited()
//                .names("name", "email")
//                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
//                    setTargetType(User.class);
//                }})
//                .linesToSkip(1)
//                .build();
//    }

}
