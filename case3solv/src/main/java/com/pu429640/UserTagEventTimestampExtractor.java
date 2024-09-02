package com.pu429640;

import com.pu429640.domain.UserTagEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

public class UserTagEventTimestampExtractor implements TimestampExtractor {
    @Override
    public long extract(ConsumerRecord<Object, Object> consumerRecord, long l) {
        UserTagEvent event = (UserTagEvent) consumerRecord.value();
        return event.getTime().toEpochMilli(); // Ass;
    }
}
