package com.pu429640;

import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.Record;

public class BucketLogger implements Processor<Windowed<String>, Aggregation, Void, Void> {
    private static int distinct = 0;

    @Override
    public void process(Record<Windowed<String>, Aggregation> record) {
        // Access the key and value from the record
        String windowedKey = record.key().key();
        Aggregation value = record.value();

        // Split the key into parts
        String[] keyParts = windowedKey.split(":");

        // Extract and format the window start and end times
        String bucketStart = formatTimestamp(record.key().window().start());
        String bucketEnd = formatTimestamp(record.key().window().end());

        // Log the bucket information
        System.out.println("Distinct1: " + distinct);
        System.out.printf("New bucket created: %s - %s\n", bucketStart, bucketEnd);
        System.out.printf("Action: %s, Origin: %s, Brand ID: %s, Category ID: %s\n",
                keyParts[0], keyParts[1], keyParts[2], keyParts[3]);
        System.out.printf("Count: %d, Sum Price: %d\n\n", value.getCount(), value.getSumPrice());
        distinct += 1;
        System.out.println("Distinct2: " + distinct);
    }

    private String formatTimestamp(long timestamp) {
        // Implement timestamp formatting logic here
        return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new java.util.Date(timestamp));
    }
}
