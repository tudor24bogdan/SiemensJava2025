package com.siemens.internship.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.siemens.internship.model.Item;

/**
 * fallows tbe interface segretation pcinciple
 * enables easier unit testing through mocking
 * clear contract for any class implementing service
 * changed processItemsAsync() to CompletableFuture<List<Item>> to properly support asynchronous processing
 */
public interface ItemService {

    List<Item> findAll();
    Optional<Item> findById(Long id);

    Item save(Item item);

    void deleteById(Long id);

    CompletableFuture<List<Item>> processItemsAsync();

}