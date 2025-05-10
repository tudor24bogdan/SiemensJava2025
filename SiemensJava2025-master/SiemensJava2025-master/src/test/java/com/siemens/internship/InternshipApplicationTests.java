package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siemens.internship.controller.ItemController;
import com.siemens.internship.model.Item;
import com.siemens.internship.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
class InternshipApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ItemService itemService;

	@Autowired
	private ObjectMapper objectMapper;

	private Item item1;
	private Item item2;
	private List<Item> items;

	@BeforeEach
	void setUp() {
		// Initialize test data
		item1 = new Item();
		item1.setId(1L);
		item1.setName("Item 1");
		item1.setDescription("Description 1");
		item1.setStatus("PENDING");
		item1.setEmail("test1@example.com");

		item2 = new Item();
		item2.setId(2L);
		item2.setName("Item 2");
		item2.setDescription("Description 2");
		item2.setStatus("PENDING");
		item2.setEmail("test2@example.com");

		items = Arrays.asList(item1, item2);
	}

	// ----- Controller Tests -----

	@Test
	void contextLoads() {
		// Verifies the Spring application context loads correctly
	}

	@Test
	void getAllItems_ShouldReturnAllItems() throws Exception {
		when(itemService.findAll()).thenReturn(items);

		mockMvc.perform(get("/api/items"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].id", is(1)))
				.andExpect(jsonPath("$[0].name", is("Item 1")))
				.andExpect(jsonPath("$[1].id", is(2)))
				.andExpect(jsonPath("$[1].name", is("Item 2")));

		verify(itemService).findAll();
	}

	@Test
	void getAllItems_WhenEmpty_ShouldReturnEmptyArray() throws Exception {
		when(itemService.findAll()).thenReturn(Collections.emptyList());

		mockMvc.perform(get("/api/items"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(0)));

		verify(itemService).findAll();
	}

	@Test
	void getItemById_WithExistingId_ShouldReturnItem() throws Exception {
		when(itemService.findById(1L)).thenReturn(Optional.of(item1));

		mockMvc.perform(get("/api/items/1"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id", is(1)))
				.andExpect(jsonPath("$.name", is("Item 1")));

		verify(itemService).findById(1L);
	}

	@Test
	void getItemById_WithNonExistingId_ShouldReturnNotFound() throws Exception {
		when(itemService.findById(99L)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/items/99"))
				.andExpect(status().isNotFound());

		verify(itemService).findById(99L);
	}

	@Test
	void createItem_WithValidItem_ShouldReturnCreated() throws Exception {
		when(itemService.save(any(Item.class))).thenReturn(item1);

		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(item1)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", is(1)))
				.andExpect(jsonPath("$.name", is("Item 1")));

		verify(itemService).save(any(Item.class));
	}

	@Test
	void updateItem_WithExistingId_ShouldReturnOk() throws Exception {
		when(itemService.findById(1L)).thenReturn(Optional.of(item1));
		when(itemService.save(any(Item.class))).thenReturn(item1);

		// Update item1 with new data
		Item updatedItem = new Item();
		updatedItem.setName("Updated Item");
		updatedItem.setDescription("Updated Description");
		updatedItem.setStatus("UPDATED");
		updatedItem.setEmail("updated@example.com");

		mockMvc.perform(put("/api/items/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(updatedItem)))
				.andExpect(status().isOk());

		verify(itemService).findById(1L);
		verify(itemService).save(any(Item.class));
	}

	@Test
	void updateItem_WithNonExistingId_ShouldReturnNotFound() throws Exception {
		when(itemService.findById(99L)).thenReturn(Optional.empty());

		Item updatedItem = new Item();
		updatedItem.setName("Updated Item");

		mockMvc.perform(put("/api/items/99")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(updatedItem)))
				.andExpect(status().isNotFound());

		verify(itemService).findById(99L);
		verify(itemService, never()).save(any(Item.class));
	}

	@Test
	void deleteItem_WithExistingId_ShouldReturnNoContent() throws Exception {
		when(itemService.findById(1L)).thenReturn(Optional.of(item1));
		doNothing().when(itemService).deleteById(1L);

		mockMvc.perform(delete("/api/items/1"))
				.andExpect(status().isNoContent());

		verify(itemService).findById(1L);
		verify(itemService).deleteById(1L);
	}

	@Test
	void deleteItem_WithNonExistingId_ShouldReturnNotFound() throws Exception {
		when(itemService.findById(99L)).thenReturn(Optional.empty());

		mockMvc.perform(delete("/api/items/99"))
				.andExpect(status().isNotFound());

		verify(itemService).findById(99L);
		verify(itemService, never()).deleteById(anyLong());
	}

	@Test
	void processItems_ShouldReturnProcessedItems() throws Exception {
		// Create processed items with PROCESSED status
		Item processedItem1 = new Item();
		processedItem1.setId(1L);
		processedItem1.setName("Item 1");
		processedItem1.setStatus("PROCESSED");

		Item processedItem2 = new Item();
		processedItem2.setId(2L);
		processedItem2.setName("Item 2");
		processedItem2.setStatus("PROCESSED");

		List<Item> processedItems = Arrays.asList(processedItem1, processedItem2);

		when(itemService.processItemsAsync()).thenReturn(CompletableFuture.completedFuture(processedItems));

		mockMvc.perform(get("/api/items/process"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].status", is("PROCESSED")))
				.andExpect(jsonPath("$[1].status", is("PROCESSED")));

		verify(itemService).processItemsAsync();
	}

}