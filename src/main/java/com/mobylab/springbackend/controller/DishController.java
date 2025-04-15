package com.mobylab.springbackend.controller;

import com.mobylab.springbackend.service.dto.DishDto;
import com.mobylab.springbackend.service.DishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dishes")
public class DishController {

    private final DishService dishService;
    private static final Logger logger = LoggerFactory.getLogger(DishController.class);
    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    @GetMapping
    public ResponseEntity<List<DishDto>> getAllDishes() {
        List<DishDto> dishList = dishService.getAllDishes();
        logger.info("Request to return dishes");
        return ResponseEntity.ok(dishList);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<DishDto> addDish(@RequestBody DishDto dishDto) {
        DishDto createdDish = dishService.addDish(dishDto);
        logger.info("Request to add dish {}", createdDish.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDish);
    }
}
