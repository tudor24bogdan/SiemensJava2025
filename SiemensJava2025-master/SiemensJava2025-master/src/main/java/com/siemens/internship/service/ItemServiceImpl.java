package com.siemens.internship.service;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {
    private static final Logger logger = Logger.getLogger(ItemServiceImpl.class.getName());
    @Autowired
    private ItemRepository itemRepository;
    public static final ExecutorService executor = Executors.newFixedThreadPool(10);

    @Override
    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    @Override
    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    @Override
    public Item save(Item item) {
        return itemRepository.save(item);
    }

    @Override
    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }

    @Override
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {
        logger.info("Starting async processing items");

        List<Long> itemIds = itemRepository.findAllIds();
        logger.info("Found " + itemIds.size() + " items to process"); //pt debug am adaugat aceste info

        List<CompletableFuture<Item>> futures = new ArrayList<>();

        for (Long id : itemIds) {
            CompletableFuture<Item> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(100);

                    // gasire items
                    Optional<Item> optionalItem = itemRepository.findById(id);
                    if (optionalItem.isEmpty()) {
                        logger.warning("Item with ID " + id + " not found");
                        return null;
                    }

                    Item item = optionalItem.get();

                    // process
                    item.setStatus("PROCESSED");

                    // save process
                    Item savedItem = itemRepository.save(item);
                    logger.info("Processed item with ID: " + id);

                    return savedItem;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.log(Level.SEVERE, "Processing interrupted for item ID: " + id, e);
                    throw new CompletionException("Processing interrupted", e);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error processing item ID: " + id, e);
                    throw new CompletionException("Error processing item: " + e.getMessage(), e);
                }
            }, executor);

            futures.add(future);
        }
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        // transform the combined future to collect all processed items
        return allFutures
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)  //  allOf ensures all futures are complete
                        .filter(Objects::nonNull)  // eliminare nulls
                        .collect(Collectors.toList())
                )
                .orTimeout(30, TimeUnit.SECONDS)  // sa orevenim pblocare definitiva
                .exceptionally(ex -> {
                    logger.log(Level.SEVERE, "Error in batch processing items", ex);
                    return new ArrayList<>();  // return lista null la eroare
                });
    }
    /**
     * cleanup method to shut down the executor service when the application stops
     * prevents resource leaks by properly terminating the thread pool

     * the original code had no cleanup mechanism for the executor service, which would lead to thread leaks.
     */
    @PreDestroy
    public void cleanup() {
        logger.info("Shutting down executor service");
        executor.shutdown();
        try {
            // Wait for existing tasks to terminate
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                // Force shutdown if normal shutdown fails
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    logger.severe("Executor did not terminate");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            // for interrupted thread while waiting
        }
    }
    /*
     * completely rewrote the processItemsAsync() method:
     * - changed return type to CompletableFuture for proper async behavior
     * & it allows proper composition of async op and error handling messages
     *
     *  added @PreDestoy cleanup because it resolves the problems of ExecutorService, it properly shuts
     * down the app when it stops, any running tasks are going to complete, resources are properly released
     * the JVM can shut down cleanly
     *
     * added logging for better debug
     *
     * added detailed comments

     * the code improved because:
     * - the original method returned immediately without waiting for processing to complete
     * - original method used non-thread-safe collections and counters
     *  - new implementation properly waits for all items to be processed
     * - error handling is robust and propagates exceptions properly
     * - logging helps with debugging and monitoring
     */
}

