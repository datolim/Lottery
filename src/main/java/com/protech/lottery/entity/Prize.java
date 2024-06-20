package com.protech.lottery.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@RedisHash("Prize")
public class Prize implements Serializable {
    @Id
    private int id;
    private String name;
    private int quantity;
    private double winningProbability; // 0.01=1%, 1= 100%
}
