package com.epam.jmp.redislab.configuration.ratelimit;

public enum RateLimitTimeInterval {

    MINUTE(60),
    HOUR(3600);

    private final long seconds;

    RateLimitTimeInterval(long seconds){
        this.seconds = seconds;
    }

    public long getSeconds() {
        return seconds;
    }
}
