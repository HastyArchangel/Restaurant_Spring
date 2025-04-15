package com.mobylab.springbackend.service;

import com.mobylab.springbackend.entity.Dish;
import com.mobylab.springbackend.service.dto.DishDto;
import com.mobylab.springbackend.repository.DishRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DishService {

    private final DishRepository dishRepository;

    public DishService(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    private DishDto mapDishToDto(Dish dish) {
        if (dish == null) return null;
        DishDto dto = new DishDto();
        dto.setId(dish.getId());
        dto.setName(dish.getName());
        dto.setDescription(dish.getDescription());
        dto.setPrice(dish.getPrice());
        return dto;
    }

    private List<DishDto> mapDishListToDtoList(List<Dish> dishes) {
        return dishes.stream()
                .map(this::mapDishToDto)
                .collect(Collectors.toList());
    }

    public DishDto addDish(DishDto dishDto) {
        Dish dish = new Dish();
        dish.setName(dishDto.getName());
        dish.setDescription(dishDto.getDescription());
        dish.setPrice(dishDto.getPrice());
        Dish savedDish = dishRepository.save(dish);
        dishDto.setId(savedDish.getId());
        return dishDto;
    }

    public List<DishDto> getAllDishes() {
        List<Dish> dishes = dishRepository.findAll();
        return mapDishListToDtoList(dishes);
    }
}