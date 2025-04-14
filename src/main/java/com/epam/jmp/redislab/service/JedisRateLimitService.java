package com.epam.jmp.redislab.service;

import com.epam.jmp.redislab.api.RequestDescriptor;
import com.epam.jmp.redislab.configuration.ratelimit.RateLimitRule;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.args.ExpiryOption;

import java.util.Optional;
import java.util.Set;

@Service
public class JedisRateLimitService implements RateLimitService {

    private final Set<RateLimitRule> rateLimitRules;
    private final JedisCluster jedisCluster;

    public JedisRateLimitService(Set<RateLimitRule> rateLimitRules, JedisCluster jedisCluster) {
        this.rateLimitRules = rateLimitRules;
        this.jedisCluster = jedisCluster;
    }

    @Override
    public boolean shouldLimit(Set<RequestDescriptor> requestDescriptors) {
        for (RequestDescriptor requestDescriptor : requestDescriptors) {
            Optional<RateLimitRule> rule = getRule(requestDescriptor);
            if (rule.isPresent()) {
                RateLimitRule rateLimitRule = rule.get();
                String key = generateKey(requestDescriptor);

                String counter = jedisCluster.get(key);
                int currentCounter = counter != null ? Integer.parseInt(counter) : 0;

                if (currentCounter <= rateLimitRule.getAllowedNumberOfRequests()) {
                    jedisCluster.incr(key);
                    jedisCluster.expire(key, rateLimitRule.getTimeInterval().getSeconds());
                    return true;
                }
            }
        }
        return false;
    }

    private String generateKey(RequestDescriptor descriptor) {
        StringBuilder keyBuilder = new StringBuilder("rate-limiter:");
        if (descriptor.getAccountId().isPresent()) {
            keyBuilder.append("account:").append(descriptor.getAccountId().get());
        }
        if (descriptor.getClientIp().isPresent()) {
            keyBuilder.append("ip:").append(descriptor.getClientIp().get());
        }
        if (descriptor.getRequestType().isPresent()) {
            keyBuilder.append("type:").append(descriptor.getRequestType().get());
        }
        return keyBuilder.toString();
    }


    private Optional<RateLimitRule> getRule(RequestDescriptor requestDescriptor) {
        Optional<RateLimitRule> specificRule = rateLimitRules.stream()
                .filter(rule -> getSpecificRule(requestDescriptor, rule))
                .findFirst();

        if (specificRule.isPresent()) {
            return specificRule;
        }

        return rateLimitRules.stream()
                .filter(rule -> getGeneralRule(requestDescriptor, rule))
                .findFirst();
    }

    private boolean getGeneralRule(RequestDescriptor descriptor, RateLimitRule rule) {
        return matchesField(rule.getAccountId(), descriptor.getAccountId()) ||
                matchesField(rule.getClientIp(), descriptor.getClientIp()) ||
                matchesField(rule.getRequestType(), descriptor.getRequestType());
    }

    private boolean getSpecificRule(RequestDescriptor descriptor, RateLimitRule rule) {
        return matchesSpecificField(rule.getAccountId(), descriptor.getAccountId()) ||
                matchesSpecificField(rule.getClientIp(), descriptor.getClientIp()) ||
                matchesSpecificField(rule.getRequestType(), descriptor.getRequestType());
    }

    private boolean matchesField(Optional<String> ruleField, Optional<String> descriptorField) {
        return ruleField.isPresent() && descriptorField.isPresent() && ruleField.get().isEmpty();
    }

    private boolean matchesSpecificField(Optional<String> ruleField, Optional<String> descriptorField) {
        return ruleField.isPresent() && descriptorField.isPresent() && ruleField.get().equals(descriptorField.get());
    }
}
