package com.pu429640;

import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;

import java.sql.SQLException;
import java.util.Properties;

public class MySqlProcessor implements Processor<Windowed<String>, Aggregation, Void, Void> {
    private final Properties props;
    private MySqlWriter mySqlWriter;

    public MySqlProcessor(Properties props) {
        this.props = props;
    }

    @Override
    public void init(ProcessorContext<Void, Void> context) {
        try {
            this.mySqlWriter = new MySqlWriter(
                    props.getProperty("mysql.url"),
                    props.getProperty("mysql.user"),
                    props.getProperty("mysql.password"),
                    props.getProperty("mysql.table")
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void process(Record<Windowed<String>, Aggregation> record) {
        mySqlWriter.writeAggregation(record.key(), record.value());
    }

    @Override
    public void close() {
        if (mySqlWriter != null) {
            mySqlWriter.close();
        }
    }
}