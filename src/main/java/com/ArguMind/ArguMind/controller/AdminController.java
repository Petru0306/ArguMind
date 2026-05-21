package com.ArguMind.ArguMind.controller;

import com.ArguMind.ArguMind.dto.GlobalStatsDto;
import com.ArguMind.ArguMind.model.DebateTopic;
import com.ArguMind.ArguMind.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/topics")
    public ResponseEntity<DebateTopic> addTopic(@RequestParam String title, @RequestParam String category) {
        return ResponseEntity.ok(adminService.addTopic(title, category));
    }

    @GetMapping("/stats")
    public ResponseEntity<GlobalStatsDto> getStats() {
        return ResponseEntity.ok(adminService.getGlobalStats());
    }
}
