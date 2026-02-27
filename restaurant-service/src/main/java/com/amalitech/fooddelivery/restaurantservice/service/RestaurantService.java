package com.amalitech.fooddelivery.restaurantservice.service;

import com.amalitech.fooddelivery.restaurantservice.client.CustomerInterface;
import com.amalitech.fooddelivery.restaurantservice.dto.*;
import com.amalitech.fooddelivery.restaurantservice.entity.MenuItemEntity;
import com.amalitech.fooddelivery.restaurantservice.entity.RestaurantEntity;
import com.amalitech.fooddelivery.restaurantservice.exception.ResourceNotFoundException;
import com.amalitech.fooddelivery.restaurantservice.exception.UnauthorizedException;
import com.amalitech.fooddelivery.restaurantservice.repository.MenuItemRepository;
import com.amalitech.fooddelivery.restaurantservice.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Restaurant Service business logic.
 *
 * Cross-domain communication:
 *  - Validates restaurant ownership via Feign call to Customer Service
 *  - Enriches RestaurantResponse with owner name via Feign call to Customer Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final CustomerInterface customerService;


    @Transactional
    public RestaurantResponse createRestaurant(String ownerUsername, RestaurantRequest request) {

        CustomerResponse owner = customerService.findEntityByUsername(ownerUsername);

        // Promote to RESTAURANT_OWNER if needed
        if (owner.getRole().equalsIgnoreCase("CUSTOMER")) {
            log.warn("Making user {} a restaurant owner", ownerUsername);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            customerService.makeRestaurantOwner(auth);
        }

        RestaurantEntity restaurant = RestaurantEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .cuisineType(request.getCuisineType())
                .address(request.getAddress())
                .city(request.getCity())
                .phone(request.getPhone())
                .estimatedDeliveryMinutes(request.getEstimatedDeliveryMinutes())
                .ownerId(owner.getId())
                .build();

        return enrichWithOwnerName(RestaurantResponse.fromEntity(restaurantRepository.save(restaurant)));
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getById(Long id) {
        RestaurantEntity restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", id));
        return enrichWithOwnerName(RestaurantResponse.fromEntity(restaurant));
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> searchByCity(String city) {
        return restaurantRepository.findByCityIgnoreCaseAndActiveTrue(city)
                .stream().map(RestaurantResponse::fromEntity).map(this::enrichWithOwnerName).toList();
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> searchByCuisine(String cuisineType) {
        return restaurantRepository.findByCuisineTypeIgnoreCaseAndActiveTrue(cuisineType)
                .stream().map(RestaurantResponse::fromEntity).map(this::enrichWithOwnerName).toList();
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> getAllActive() {
        return restaurantRepository.findByActiveTrue()
                .stream().map(RestaurantResponse::fromEntity).map(this::enrichWithOwnerName).toList();
    }

    // ---- Menu Item management ----

    @Transactional
    public MenuItemResponse addMenuItem(Long restaurantId, String ownerUsername, MenuItemRequest request) {
        RestaurantEntity restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", restaurantId));

        CustomerResponse owner = customerService.findEntityByUsername(ownerUsername);

        if (!restaurant.getOwnerId().equals(owner.getId())) {
            throw new UnauthorizedException("You don't own this restaurant");
        }

        MenuItemEntity item = MenuItemEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .restaurant(restaurant)
                .build();

        return MenuItemResponse.fromEntity(menuItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public MenuItemResponse getMenuItemById(Long menuId) {
        return MenuItemResponse.fromEntity(menuItemRepository.findById(menuId).orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", menuId)));
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> getMenu(Long restaurantId) {
        return menuItemRepository.findByRestaurantIdAndAvailableTrue(restaurantId)
                .stream().map(MenuItemResponse::fromEntity).toList();
    }

    @Transactional
    public MenuItemResponse updateMenuItem(Long itemId, String ownerUsername, MenuItemRequest request) {
        MenuItemEntity item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", itemId));
        CustomerResponse owner = customerService.findEntityByUsername(ownerUsername);

        if (!item.getRestaurant().getOwnerId().equals(owner.getId())) {
            throw new UnauthorizedException("You don't own this restaurant");
        }

        if (request.getName() != null) item.setName(request.getName());
        if (request.getDescription() != null) item.setDescription(request.getDescription());
        if (request.getPrice() != null) item.setPrice(request.getPrice());
        if (request.getCategory() != null) item.setCategory(request.getCategory());

        return MenuItemResponse.fromEntity(menuItemRepository.save(item));
    }

    @Transactional
    public void toggleMenuItemAvailability(Long itemId, String ownerUsername) {
        MenuItemEntity item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", itemId));

        CustomerResponse owner = customerService.findEntityByUsername(ownerUsername);

        if (!item.getRestaurant().getOwnerId().equals(owner.getId())) {
            throw new UnauthorizedException("You don't own this restaurant");
        }

        item.setAvailable(!item.isAvailable());
        menuItemRepository.save(item);
    }

    // Used by OrderService — MONOLITH COUPLING
    public RestaurantEntity findEntityById(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", id));
    }

    public MenuItemEntity findMenuItemById(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", id));
    }

    /**
     * Enriches a RestaurantResponse with the owner's name fetched from the Customer Service.
     * Uses a try-catch so that a Customer Service outage does not break restaurant retrieval.
     */
    private RestaurantResponse enrichWithOwnerName(RestaurantResponse response) {
        if (response.getOwnerId() == null) {
            return response;
        }
        try {
            CustomerResponse owner = customerService.getById(response.getOwnerId());
            if (owner != null) {
                response.setOwnerName(owner.getFirstName() + " " + owner.getLastName());
            }
        } catch (Exception e) {
            log.warn("Could not fetch owner info for restaurant {}: {}", response.getId(), e.getMessage());
        }
        return response;
    }
}
