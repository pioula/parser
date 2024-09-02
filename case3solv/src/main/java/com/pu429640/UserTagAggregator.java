package com.pu429640;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.StateStore;
import org.apache.kafka.streams.processor.To;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import java.time.Duration;
import java.util.Properties;

public class UserTagAggregator {

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "user-tag-aggregator");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka-service:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> userTags = builder.stream("usertagevents");


        // Print each incoming event
        userTags.foreach((key, value) -> {
            System.out.println("Received event:");
            System.out.println("Key: " + key);
            System.out.println("Value: " + value);
            System.out.println("--------------------");
        });

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();

        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

//        // Create 1-minute tumbling windows
//        TimeWindows timeWindows = TimeWindows.of(Duration.ofMinutes(1));
//
//        // Aggregate count and sum_price
//        userTags
//                .groupBy((key, value) -> createGroupKey(value))
//                .windowedBy(timeWindows)
//                .aggregate(
//                        () -> new Aggregation(0L, 0L),
//                        (key, value, aggregate) -> {
//                            aggregate.count++;
//                            aggregate.sumPrice += value.getProductInfo().getPrice();
//                            return aggregate;
//                        },
//                        Materialized.with(Serdes.String(), new AggregationSerde())
//                )
//                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
//                .toStream()
//                .process(() -> new BucketLogger());
//
//        KafkaStreams streams = new KafkaStreams(builder.build(), props);
//        streams.start();
    }
//
//    private static String createGroupKey(UserTag userTag) {
//        return String.join(":",
//                userTag.getAction(),
//                userTag.getOrigin(),
//                userTag.getProductInfo().getBrandId(),
//                userTag.getProductInfo().getCategoryId()
//        );
//    }
//
//    static class Aggregation {
//        long count;
//        long sumPrice;
//
//        Aggregation(long count, long sumPrice) {
//            this.count = count;
//            this.sumPrice = sumPrice;
//        }
//    }
//    private static String createGroupKey(UserTag userTag) {
//        return String.join(":",
//                userTag.getAction(),
//                userTag.getOrigin(),
//                userTag.getProductInfo().getBrandId(),
//                userTag.getProductInfo().getCategoryId()
//        );
//    }
//
//    static class Aggregation {
//        long count;
//        long sumPrice;
//
//        Aggregation(long count, long sumPrice) {
//            this.count = count;
//            this.sumPrice = sumPrice;
//        }
//    }

//    static class BucketLogger implements Processor<Windowed<String>, Aggregation, Void, Void> {
//        private ProcessorContext context;
//
//        @Override
//        public void init(ProcessorContext context) {
//            this.context = context;
//        }
//
//        @Override
//        public void process(Windowed<String> key, Aggregation value) {
//            String[] keyParts = key.key().split(":");
//            String bucketStart = formatTimestamp(key.window().start());
//            String bucketEnd = formatTimestamp(key.window().end());
//
//            System.out.printf("New bucket created: %s - %s\n", bucketStart, bucketEnd);
//            System.out.printf("Action: %s, Origin: %s, Brand ID: %s, Category ID: %s\n",
//                    keyParts[0], keyParts[1], keyParts[2], keyParts[3]);
//            System.out.printf("Count: %d, Sum Price: %d\n\n", value.count, value.sumPrice);
//        }
//
//        @Override
//        public void process(Record<Windowed<String>, Aggregation> record) {
//
//        }
//
//        @Override
//        public void close() {}
//
//        private String formatTimestamp(long timestamp) {
//            // Implement timestamp formatting logic here
//            return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new java.util.Date(timestamp));
//        }
//    }
}