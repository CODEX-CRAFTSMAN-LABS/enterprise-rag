package com.enterprise.rag.ingestion.config;

import com.enterprise.rag.ingestion.application.TransientIngestionException;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(IngestionProperties.class)
public class IngestionConfig {

  @Configuration
  @EnableKafka
  @ConditionalOnExpression(
      "${ingestion.outbox.enabled:false} == true || ${ingestion.saga.enabled:false} == true")
  static class KafkaConfiguration {

    @Bean
    NewTopic documentUploadedTopic(IngestionProperties properties) {
      return new NewTopic(properties.kafka().documentUploadedTopic(), 3, (short) 1);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
        ConsumerFactory<String, String> consumerFactory) {
      ConcurrentKafkaListenerContainerFactory<String, String> factory =
          new ConcurrentKafkaListenerContainerFactory<>();
      factory.setConsumerFactory(consumerFactory);
      DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(2_000L, 5L));
      errorHandler.addRetryableExceptions(TransientIngestionException.class);
      factory.setCommonErrorHandler(errorHandler);
      return factory;
    }
  }

  @Configuration
  @ConditionalOnProperty(name = "ingestion.saga.enabled", havingValue = "true")
  static class SagaKafkaTopics {

    @Bean
    NewTopic documentParsedTopic(IngestionProperties properties) {
      return new NewTopic(properties.kafka().documentParsedTopic(), 3, (short) 1);
    }

    @Bean
    NewTopic documentChunkedTopic(IngestionProperties properties) {
      return new NewTopic(properties.kafka().documentChunkedTopic(), 3, (short) 1);
    }

    @Bean
    NewTopic documentIngestionFailedTopic(IngestionProperties properties) {
      return new NewTopic(properties.kafka().documentIngestionFailedTopic(), 3, (short) 1);
    }
  }
}
