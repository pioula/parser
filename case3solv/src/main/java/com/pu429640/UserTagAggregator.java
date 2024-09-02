package com.pu429640;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pu429640.domain.UserTagEvent;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.Record;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Properties;

public class UserTagAggregator {

    private static PostgresWriter postgresWriter;

    public static void main(String[] args) throws SQLException {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "user-tag-aggregator");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka-service:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, UserTagEventTimestampExtractor.class.getName());

        // PostgreSQL configuration
        props.put("postgres.url", "jdbc:postgresql://localhost:5432/your_database");
        props.put("postgres.user", "your_username");
        props.put("postgres.password", "your_password");
        props.put("postgres.table", "user_tag_aggregations");

        postgresWriter = new PostgresWriter(
            props.getProperty("postgres.url"),
            props.getProperty("postgres.user"),
            props.getProperty("postgres.password"),
            props.getProperty("postgres.table")
        );

        StreamsBuilder builder = new StreamsBuilder();

        // Create a custom Serde for UserTagEvent
        final Serde<UserTagEvent> userTagEventSerde = createUserTagEventSerde();
        final Serde<Aggregation> aggregationSerde = createAggregationSerde();

        // Consume the stream with the custom Serde
        KStream<String, UserTagEvent> userTags = builder.stream("usertagevents",
                Consumed.with(Serdes.String(), userTagEventSerde));

        // Create 1-minute tumbling windows
        TimeWindows timeWindows = TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1));

        // Aggregate count and sum_price
        userTags
                .groupBy((key, value) -> createGroupKey(value))
                .windowedBy(timeWindows)
                .aggregate(
                        () -> new Aggregation(0L, 0L),
                        (key, value, aggregate) -> {
                            aggregate.setCount(aggregate.getCount() + 1);
                            aggregate.setSumPrice(aggregate.getSumPrice() + value.getProductInfo().getPrice());
                            return aggregate;
                        },
                        Materialized.with(Serdes.String(), aggregationSerde)
                )
                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
                .toStream()
                .process(PostgresProcessor::new);

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            streams.close();
            postgresWriter.close();
        }));
    }

    private static class PostgresProcessor implements Processor<Windowed<String>, Aggregation, Void, Void> {
        @Override
        public void process(Record<Windowed<String>, Aggregation> record) {
            postgresWriter.writeAggregation(record.key(), record.value());
        }
    }

    private static Serde<UserTagEvent> createUserTagEventSerde() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        final Serializer<UserTagEvent> serializer = (topic, data) -> {
            try {
                return mapper.writeValueAsBytes(data);
            } catch (Exception e) {
                throw new RuntimeException("Error serializing UserTagEvent", e);
            }
        };

        final Deserializer<UserTagEvent> deserializer = (topic, data) -> {
            try {
                return mapper.readValue(data, UserTagEvent.class);
            } catch (Exception e) {
                throw new RuntimeException("Error deserializing UserTagEvent", e);
            }
        };

        return Serdes.serdeFrom(serializer, deserializer);
    }

    private static Serde<Aggregation> createAggregationSerde() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        final Serializer<Aggregation> serializer = (topic, data) -> {
            try {
                return mapper.writeValueAsBytes(data);
            } catch (Exception e) {
                throw new RuntimeException("Error serializing Aggregation", e);
            }
        };

        final Deserializer<Aggregation> deserializer = (topic, data) -> {
            try {
                return mapper.readValue(data, Aggregation.class);
            } catch (Exception e) {
                throw new RuntimeException("Error deserializing Aggregation", e);
            }
        };

        return Serdes.serdeFrom(serializer, deserializer);
    }

    private static String createGroupKey(UserTagEvent userTag) {
        return String.join(":",
                userTag.getAction().toString(),
                userTag.getOrigin(),
                userTag.getProductInfo().getBrandId(),
                userTag.getProductInfo().getCategoryId()
        );
    }
}