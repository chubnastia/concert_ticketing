package com.yourticketing.concert_backend.controller;

import com.yourticketing.concert_backend.dto.AdminConcertDtos.CreateRequest;
import com.yourticketing.concert_backend.dto.AdminConcertDtos.UpdateRequest;
import com.yourticketing.concert_backend.model.Concert;
import com.yourticketing.concert_backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/admin/concerts")
public class AdminController {

    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @Operation(summary = "Create a new concert")
    @PostMapping
    public ResponseEntity<Concert> create(@Valid @RequestBody CreateRequest req) {
        Concert created = service.create(req);
        return ResponseEntity
                .created(URI.create("/concerts/" + created.getId()))
                .body(created);
    }

    @Operation(summary = "Update an existing concert (full update)")
    @PutMapping("/{id}")
    public ResponseEntity<Concert> update(@PathVariable long id,
                                          @Valid @RequestBody UpdateRequest req) {
        Concert updated = service.update(id, req);
        return ResponseEntity.ok(updated);
    }
}
