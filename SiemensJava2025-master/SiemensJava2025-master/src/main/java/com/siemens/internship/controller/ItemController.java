package com.siemens.internship.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import com.siemens.internship.model.Item;
import com.siemens.internship.service.ItemService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * REST controller for Item operations.
 * Provides endpoints for CRUD operations on items.
 */
@RestController
@RequestMapping("/api/items")
public class ItemController {
    private static final Logger logger = Logger.getLogger(ItemController.class.getName());

    @Autowired
    private ItemService itemService;

    /**
     * Get all items.
     *
     * No changes needed for this endpoint - it was already using correct status codes.
     *
     * @return ResponseEntity with all items and HTTP status OK
     */
    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        logger.info("Getting all items");
        List<Item> items = itemService.findAll();
        return new ResponseEntity<>(items, HttpStatus.OK);
    }

    /**
     * Create a new item.
     *
     * FIXED ISSUES:
     * 1. Original returned CREATED status (201) for validation errors
     * 2. Original returned BAD_REQUEST status (400) for successful creation
     * 3. Original didn't provide validation error details
     *
     * @param item The item to create
     * @param result Validation result
     * @return ResponseEntity with created item and HTTP status CREATED, or BAD_REQUEST if validation fails
     */
    @PostMapping
    public ResponseEntity<?> createItem(@Valid @RequestBody Item item, BindingResult result) {
        logger.info("Creating a new item");

        // Handle validation errors
        if (result.hasErrors()) {
            List<String> errors = result.getFieldErrors()
                    .stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.toList());

            logger.warning("Validation errors: " + errors);
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST); // Correct status for validation errors
        }

        // Create the item
        Item savedItem = itemService.save(item);
        return new ResponseEntity<>(savedItem, HttpStatus.CREATED); // Correct status for creation
    }

    /**
     * Get an item by ID.
     *
     * FIXED ISSUES:
     * 1. Original returned NO_CONTENT (204) when item not found, which is incorrect
     *    - NOT_FOUND (404) is the appropriate response
     *
     * @param id The ID of the item to get
     * @return ResponseEntity with item and HTTP status OK, or NOT_FOUND if item doesn't exist
     */
    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) {
        logger.info("Getting item with ID: " + id);
        return itemService.findById(id)
                .map(item -> new ResponseEntity<>(item, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND)); // Corrected from NO_CONTENT to NOT_FOUND
    }

    /**
     * Update an existing item.
     *
     * FIXED ISSUES:
     * 1. Original returned CREATED (201) for updates, which is incorrect
     *    - OK (200) is more appropriate for updates
     * 2. Original returned ACCEPTED (202) when item not found, which is incorrect
     *    - NOT_FOUND (404) is more appropriate
     * 3. Original didn't validate the update data
     * 4. Original didn't handle validation errors
     *
     * @param id The ID of the item to update
     * @param item The updated item data
     * @param result Validation result
     * @return ResponseEntity with updated item and HTTP status OK,
     *         BAD_REQUEST if validation fails, or NOT_FOUND if item doesn't exist
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody Item item, // Added @Valid annotation
            BindingResult result) {
        logger.info("Updating item with ID: " + id);

        // Handle validation errors
        if (result.hasErrors()) {
            List<String> errors = result.getFieldErrors()
                    .stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.toList());

            logger.warning("Validation errors: " + errors);
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }

        // Check if item exists
        Optional<Item> existingItem = itemService.findById(id);
        if (existingItem.isPresent()) {
            item.setId(id);
            Item updatedItem = itemService.save(item);
            return new ResponseEntity<>(updatedItem, HttpStatus.OK); // Changed from CREATED to OK
        } else {
            logger.warning("Item with ID " + id + " not found");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); // Changed from ACCEPTED to NOT_FOUND
        }
    }

    /**
     * Delete an item by ID.
     *
     * FIXED ISSUES:
     * 1. Original always returned CONFLICT (409) status, which is incorrect
     *    - NO_CONTENT (204) is the standard for successful deletion
     * 2. Original didn't check if the item exists before deletion
     *
     * @param id The ID of the item to delete
     * @return ResponseEntity with HTTP status NO_CONTENT if successful, or NOT_FOUND if item doesn't exist
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        logger.info("Deleting item with ID: " + id);

        // Check if item exists before deleting
        if (itemService.findById(id).isPresent()) {
            itemService.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT); // Changed from CONFLICT to NO_CONTENT
        } else {
            logger.warning("Item with ID " + id + " not found");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Process all items asynchronously.
     *
     * FIXED ISSUES:
     * 1. Original didn't handle the CompletableFuture return type properly
     * 2. Original had no error handling
     * 3. Original had no timeout handling
     *
     * @return ResponseEntity with list of processed items and HTTP status OK,
     *         INTERNAL_SERVER_ERROR if processing fails, or REQUEST_TIMEOUT if processing times out
     */
    @GetMapping("/process")
    public ResponseEntity<?> processItems() {
        logger.info("Processing items asynchronously");

        try {
            // Start processing and wait for completion with timeout
            CompletableFuture<List<Item>> future = itemService.processItemsAsync();
            List<Item> processedItems = future.get(60, TimeUnit.SECONDS); // Wait with timeout

            logger.info("Successfully processed " + processedItems.size() + " items");
            return new ResponseEntity<>(processedItems, HttpStatus.OK);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.log(Level.SEVERE, "Processing interrupted", e);
            return new ResponseEntity<>("Processing interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            logger.log(Level.SEVERE, "Error during processing", e);
            return new ResponseEntity<>("Processing error: " + e.getCause().getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (TimeoutException e) {
            logger.log(Level.SEVERE, "Processing timed out", e);
            return new ResponseEntity<>("Processing timed out", HttpStatus.REQUEST_TIMEOUT);
        }
    }
}