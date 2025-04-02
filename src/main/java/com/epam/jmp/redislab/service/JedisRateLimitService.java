package com.epam.jmp.redislab.service;

import com.epam.jmp.redislab.configuration.ratelimit.RateLimitRule;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisCluster;

import java.util.Set;

@Service
public class JedisRateLimitService implements RateLimitService {

    private final Set<RateLimitRule> rateLimitRules;
    private final JedisCluster jedisCluster;

    public JedisRateLimitService(Set<RateLimitRule> rateLimitRules, JedisCluster jedisCluster) {
        this.rateLimitRules = rateLimitRules;
        this.jedisCluster = jedisCluster;
    }
}
