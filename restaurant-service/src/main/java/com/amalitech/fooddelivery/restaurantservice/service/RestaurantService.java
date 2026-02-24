package com.amalitech.fooddelivery.restaurantservice.service;

import com.amalitech.fooddelivery.restaurantservice.dto.MenuItemRequest;
import com.amalitech.fooddelivery.restaurantservice.dto.MenuItemResponse;
import com.amalitech.fooddelivery.restaurantservice.dto.RestaurantRequest;
import com.amalitech.fooddelivery.restaurantservice.dto.RestaurantResponse;
import com.amalitech.fooddelivery.restaurantservice.entity.MenuItemEntity;
import com.amalitech.fooddelivery.restaurantservice.entity.RestaurantEntity;
import com.amalitech.fooddelivery.restaurantservice.exception.ResourceNotFoundException;
import com.amalitech.fooddelivery.restaurantservice.repository.MenuItemRepository;
import com.amalitech.fooddelivery.restaurantservice.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * MONOLITH COUPLING: This service directly accesses CustomerRepository
 * to validate restaurant ownership. In microservices, it should call
 * Customer Service via Feign to validate the owner.
 */
@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
//    private final CustomerRepository customerRepository; // CROSS-DOMAIN DEPENDENCY //TODO

    public RestaurantService(RestaurantRepository restaurantRepository,
                             MenuItemRepository menuItemRepository
//            ,
//                             CustomerRepository customerRepository
    ) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
//        this.customerRepository = customerRepository;
    }

    @Transactional
    public RestaurantResponse createRestaurant(String ownerUsername, RestaurantRequest request) {
        // TODO
//        // MONOLITH: directly accessing Customer entity from Restaurant domain
//        Customer owner = customerRepository.findByUsername(ownerUsername)
//                .orElseThrow(() -> new ResourceNotFoundException("Customer", "username", ownerUsername));
//
//        // Promote to RESTAURANT_OWNER if needed
//        if (owner.getRole() == Customer.Role.CUSTOMER) {
//            owner.setRole(Customer.Role.RESTAURANT_OWNER);
//            customerRepository.save(owner);
//        }

        RestaurantEntity restaurant = RestaurantEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .cuisineType(request.getCuisineType())
                .address(request.getAddress())
                .city(request.getCity())
                .phone(request.getPhone())
                .estimatedDeliveryMinutes(request.getEstimatedDeliveryMinutes())
//                .owner(owner) // MONOLITH: direct entity reference across domains // TODO
                .build();

        return RestaurantResponse.fromEntity(restaurantRepository.save(restaurant));
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getById(Long id) {
        RestaurantEntity restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", id));
        return RestaurantResponse.fromEntity(restaurant);
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> searchByCity(String city) {
        return restaurantRepository.findByCityIgnoreCaseAndActiveTrue(city)
                .stream().map(RestaurantResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> searchByCuisine(String cuisineType) {
        return restaurantRepository.findByCuisineTypeIgnoreCaseAndActiveTrue(cuisineType)
                .stream().map(RestaurantResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> getAllActive() {
        return restaurantRepository.findByActiveTrue()
                .stream().map(RestaurantResponse::fromEntity).toList();
    }

    // ---- Menu Item management ----

    @Transactional
    public MenuItemResponse addMenuItem(Long restaurantId, String ownerUsername, MenuItemRequest request) {
        RestaurantEntity restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", restaurantId));

//        // MONOLITH: cross-domain ownership check via entity traversal
//        if (!restaurant.getOwner().getUsername().equals(ownerUsername)) {
//            throw new UnauthorizedException("You don't own this restaurant");
//        }

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
    public List<MenuItemResponse> getMenu(Long restaurantId) {
        return menuItemRepository.findByRestaurantIdAndAvailableTrue(restaurantId)
                .stream().map(MenuItemResponse::fromEntity).toList();
    }

    @Transactional
    public MenuItemResponse updateMenuItem(Long itemId, String ownerUsername, MenuItemRequest request) {
        MenuItemEntity item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", itemId));

//        // MONOLITH: cross-domain ownership check
//        if (!item.getRestaurant().getOwner().getUsername().equals(ownerUsername)) {
//            throw new UnauthorizedException("You don't own this restaurant");
//        }

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

        // TODO
//        if (!item.getRestaurant().getOwner().getUsername().equals(ownerUsername)) {
//            throw new UnauthorizedException("You don't own this restaurant");
//        }

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
}
