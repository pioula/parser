package com.pu429640;

public class Aggregation {
    private long count;
    private long sumPrice;

    public Aggregation() {}

    Aggregation(long count, long sumPrice) {
        this.count = count;
        this.sumPrice = sumPrice;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getSumPrice() {
        return sumPrice;
    }

    public void setSumPrice(long sumPrice) {
        this.sumPrice = sumPrice;
    }
}