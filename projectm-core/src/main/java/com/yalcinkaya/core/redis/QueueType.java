package com.yalcinkaya.core.redis;

public enum QueueType {

    SOLO("solo"),
    PARTY("party");

    private final String redisKey;

    QueueType(String redisKey) {
        this.redisKey = redisKey;
    }

    public String getRedisKey() {
        return redisKey;
    }
}
