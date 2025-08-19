package com.yourticketing.concert_backend.controller;

import com.yourticketing.concert_backend.dto.AdminConcertDtos.CreateRequest;
import com.yourticketing.concert_backend.dto.AdminConcertDtos.UpdateRequest;
import com.yourticketing.concert_backend.model.Concert;
import com.yourticketing.concert_backend.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/concerts")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    public ResponseEntity<Concert> create(@Valid @RequestBody CreateRequest req) {
        Concert c = adminService.create(req);
        return ResponseEntity.ok(c);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Concert> update(@PathVariable long id,
                                          @Valid @RequestBody UpdateRequest req) {
        Concert c = adminService.update(id, req);
        return ResponseEntity.ok(c);
    }
}
