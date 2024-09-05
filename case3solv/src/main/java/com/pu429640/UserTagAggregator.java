package com.pu429640;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pu429640.config.Config;
import com.pu429640.domain.UserTagEvent;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Properties;
public class UserTagAggregator {
    private static MySqlWriter mySqlWriter;

    public static void main(String[] args) throws SQLException {
        Properties props = new Properties();
        props.put("application.id", Config.get("KAFKA_APP_ID"));
        props.put("bootstrap.servers", Config.get("KAFKA_BOOTSTRAP"));
        props.put("default.key.serde", Serdes.String().getClass());
        props.put("default.value.serde", Serdes.String().getClass());
        props.put("default.timestamp.extractor", UserTagEventTimestampExtractor.class.getName());
        props.put("mysql.url", String.format("jdbc:mysql://%s:%s/%s", new Object[] { Config.get("MYSQL_HOST"),
                Config.get("MYSQL_PORT"),
                Config.get("MYSQL_DATABASE") }));
        props.put("mysql.user", Config.get("MYSQL_ROOT_USER"));
        props.put("mysql.password", Config.get("MYSQL_ROOT_PASSWORD"));
        props.put("mysql.table", Config.get("MYSQL_TABLE"));
        mySqlWriter = new MySqlWriter(props.getProperty("mysql.url"), props.getProperty("mysql.user"), props.getProperty("mysql.password"), props.getProperty("mysql.table"));
        StreamsBuilder builder = new StreamsBuilder();
        Serde<UserTagEvent> userTagEventSerde = createUserTagEventSerde();
        Serde<Aggregation> aggregationSerde = createAggregationSerde();
        KStream<String, UserTagEvent> userTags = builder.stream(Config.get("KAFKA_TOPIC"),
                Consumed.with(Serdes.String(), userTagEventSerde));
        TimeWindows timeWindows = TimeWindows.ofSizeAndGrace(Duration.ofMinutes(1L), Duration.ofSeconds(3L));
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
                .process(() -> new MySqlProcessor(props));
        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            streams.close();
            mySqlWriter.close();
        }));
    }

    private static Serde<UserTagEvent> createUserTagEventSerde() {
        ObjectMapper mapper = new ObjectMapper();
        Serializer<UserTagEvent> serializer = (topic, data) -> {
            try {
                return mapper.writeValueAsBytes(data);
            } catch (Exception e) {
                throw new RuntimeException("Error serializing UserTagEvent", e);
            }
        };
        Deserializer<UserTagEvent> deserializer = (topic, data) -> {
            try {
                return (UserTagEvent)mapper.readValue(data, UserTagEvent.class);
            } catch (Exception e) {
                throw new RuntimeException("Error deserializing UserTagEvent", e);
            }
        };
        return Serdes.serdeFrom(serializer, deserializer);
    }

    private static Serde<Aggregation> createAggregationSerde() {
        ObjectMapper mapper = new ObjectMapper();
        Serializer<Aggregation> serializer = (topic, data) -> {
            try {
                return mapper.writeValueAsBytes(data);
            } catch (Exception e) {
                throw new RuntimeException("Error serializing Aggregation", e);
            }
        };
        Deserializer<Aggregation> deserializer = (topic, data) -> {
            try {
                return (Aggregation)mapper.readValue(data, Aggregation.class);
            } catch (Exception e) {
                throw new RuntimeException("Error deserializing Aggregation", e);
            }
        };
        return Serdes.serdeFrom(serializer, deserializer);
    }

    private static String createGroupKey(UserTagEvent userTag) {
        return String.join(":", new CharSequence[] { userTag
                .getAction().toString(), userTag
                .getOrigin(), userTag
                .getProductInfo().getBrandId(), userTag
                .getProductInfo().getCategoryId() });
    }
}