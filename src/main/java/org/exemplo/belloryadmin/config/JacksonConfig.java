package org.exemplo.belloryadmin.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * ObjectMapper default do Spring MVC.
 *
 * Marcado como @Primary porque o contexto tambem tem outro bean ObjectMapper
 * (paymentApiObjectMapper em RedisConfig) - sem @Primary, o Spring Boot
 * desiste de criar o default e o MVC acaba usando o paymentApiObjectMapper.
 *
 * Hibernate6Module e registrado para que Jackson ignore automaticamente
 * propriedades proxy lazy (hibernateLazyInitializer/handler).
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        Hibernate6Module hibernateModule = new Hibernate6Module();
        hibernateModule.disable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);

        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(hibernateModule)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
    }
}
