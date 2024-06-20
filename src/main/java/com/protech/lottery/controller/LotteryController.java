package com.protech.lottery.controller;

import com.protech.lottery.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.protech.lottery.service.*;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/lottery")
public class LotteryController {

    @Autowired
    private LotteryService lotteryService;

    @PostMapping("/draw/{userId}")
    public ResponseEntity<String> drawPrize(@PathVariable String userId) {
        return ResponseEntity.ok(lotteryService.drawPrize(userId));
    }

    @GetMapping("/userPrize/{userId}")
    public ResponseEntity<Prize> getUserPrize(@PathVariable String userId) {
        Prize prize = lotteryService.getUserPrize(userId);
        return ResponseEntity.ok(prize);
    }

    @GetMapping("/remainingAttempts/{userId}")
    public ResponseEntity<Integer> getRemainingAttempts(@PathVariable String userId) {
        int attempts = lotteryService.getRemainingAttempts(userId);
        return ResponseEntity.ok(attempts);
    }

    @PostMapping("/addUser")
    public ResponseEntity<String> addUser(@RequestBody User user) {
        lotteryService.addUser(user);
        return ResponseEntity.ok("User added successfully");
    }

    @PostMapping("/setAttempts/{userId}/{drawTimes}")
    public ResponseEntity<String> setAttempts(@PathVariable String userId, @PathVariable int drawTimes) {
        lotteryService.setAttempts(userId, drawTimes);
        return ResponseEntity.ok("User draw attempt set successfully");
    }

    @PostMapping("/addPrize")
    public ResponseEntity<String> addPrize(@RequestBody Prize prize) {
        lotteryService.addPrize(prize);
        return ResponseEntity.ok("Prize added successfully");
    }

    @GetMapping("/prizes")
    public List<Prize> getAllPrizes() {
        return lotteryService.getAllPrizes();
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return lotteryService.getAllUsers();
    }

}
