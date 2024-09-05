package com.pu429640.config;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaConfig {
    @Value("${kafka.broker.count}")
    private int kafkaBrokerCount;

    @Value("${kafka.broker.domain}")
    private String kafkaBrokerDomain;

    @Value("${kafka.broker.port}")
    private String kafkaBrokerPort;

    private String getBootstrapServers() {
        return IntStream.range(0, this.kafkaBrokerCount)
                .<CharSequence>mapToObj(i -> String.format("kafka-controller-%d.%s:%s", Integer.valueOf(i), this.kafkaBrokerDomain, this.kafkaBrokerPort)).collect(Collectors.joining(","));
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put("bootstrap.servers", getBootstrapServers());
        configProps.put("key.serializer", StringSerializer.class);
        configProps.put("value.serializer", JsonSerializer.class);
        return (ProducerFactory<String, Object>)new DefaultKafkaProducerFactory(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate(producerFactory());
    }
}
