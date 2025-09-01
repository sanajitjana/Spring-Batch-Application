package com.example.demo.config;

import com.example.demo.dto.MyDto;
import com.example.demo.entity.MyEntity;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

@Configuration
public class BatchConfig {

    private final EntityManagerFactory emf;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    public BatchConfig(JobRepository jobRepository,
                       PlatformTransactionManager transactionManager,
                       EntityManagerFactory emf) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.emf = emf;
    }

    // === Reader ===
    @Bean
    @StepScope
    public FlatFileItemReader<MyDto> reader(@Value("#{jobParameters['filePath']}") String filePath) throws IOException {
        // Read from local file path
        Path path = Paths.get(filePath);
        
        FlatFileItemReader<MyDto> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(path.toFile()));
        reader.setLinesToSkip(1);
        reader.setLineMapper(new DefaultLineMapper<>() {{
            setLineTokenizer(new DelimitedLineTokenizer(",") {{
                setNames("name","email");
            }});
            setFieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                setTargetType(MyDto.class);
            }});
        }});
        return reader;
    }

    // === Processor ===
    @Bean
    public ItemProcessor<MyDto, MyEntity> processor() {
        return dto -> {
            if (dto.getEmail() == null || !dto.getEmail().contains("@")) {
                return null; // skip invalid record
            }
            MyEntity entity = new MyEntity();
            // Don't set ID as it's auto-generated
            entity.setFullName(dto.getName().trim());
            entity.setEmail(dto.getEmail().toLowerCase());
            entity.setCreatedAt(Instant.now());
            entity.setUpdatedAt(Instant.now());
            return entity;
        };
    }

    // === Writer ===
    @Bean
    public JpaItemWriter<MyEntity> writer() {
        JpaItemWriter<MyEntity> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(emf);
        return writer;
    }

    // === Step ===
    @Bean
    public Step step1() throws Exception {
        return new StepBuilder("step1", jobRepository)
                .<MyDto, MyEntity>chunk(5, transactionManager)
                .reader(reader(null)) // StepScope proxy resolves parameter dynamically at runtime
                .processor(processor())
                .writer(writer())
                .build();
    }

    // === Job ===
    @Bean
    public Job importJob(Step step1, JobExecutionListener listener) throws Exception {
        return new JobBuilder("importJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(step1)
                .build();
    }
}
