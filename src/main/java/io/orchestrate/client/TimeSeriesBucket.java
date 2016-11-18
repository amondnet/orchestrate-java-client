package io.orchestrate.client;

public class TimeSeriesBucket {
    
    private final String bucket;
    
    private final long count;
    
    public TimeSeriesBucket(String bucket, long count) {
        this.bucket = bucket;
        this.count = count;
    }
    
    public String getBucket() {
        return bucket;
    }
    
    public long getCount() {
        return count;
    }

}
