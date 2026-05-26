package com.example.inventory.service;

import com.example.inventory.entity.Item;
import com.example.inventory.repository.ItemOrchestrationRepository;

import com.example.inventory.saga.OrderClient;
import com.example.saga.annotation.SagaOrchestrated;
import com.example.saga.aop.SagaAspect;
import com.example.saga.core.Saga;
import com.example.saga.remote.RemoteSagaStep;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.common.protocol.types.Field.Str;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class InventoryOrchestrationService {

	private final String orderServiceURL = "http://localhost:9091/orders/rollback/";

	private final ItemOrchestrationRepository repo;
	private final RestTemplate restTemplate = new RestTemplate();

	@Autowired
	SagaLogger sagaLogger;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	OrderClient orderClient;


	@SagaOrchestrated
	public void createInventory(String name, int qty) {

	String sagaId = UUID.randomUUID().toString();
		log.info("Starting createInventory saga with sagaId: {}", sagaId);

		Item item = new Item();
		item.setName(name);
		item.setQuantity(qty);
		item = repo.save(item);

		sagaLogger.logCreate(sagaId, "ITEM", item.getId());

		Long itemId = item.getId();

		Saga.compensate(() -> {
			repo.deleteById(itemId);
			sagaLogger.logRollback(sagaId, "ITEM", itemId);
		});

		log.info("Calling order service for sagaId: {}, itemId: {}", sagaId, itemId);
		orderClient.createOrder(item.getId(), sagaId);
	}

	@SagaOrchestrated
	public void updateInventory(Long id, int newQty) throws JsonProcessingException {

		Item item = repo.findById(id).orElseThrow(() -> new RuntimeException("Item not found"));

		String beforeStateJson = objectMapper.writeValueAsString(item);
		Item beforeState = objectMapper.readValue(beforeStateJson, Item.class);

		String sagaId = SagaAspect.context().getSagaId();

		Saga.compensate(() -> {
			repo.save(beforeState);
			sagaLogger.logRollback(sagaId, "ITEM", id);
		});

		item.setQuantity(newQty);
		repo.save(item);

		sagaLogger.logUpdate(sagaId, "ITEM", id, beforeStateJson);

		restTemplate.put("http://localhost:9091/orders/update?sagaId=" + sagaId + "&inventoryId=" + item.getId(), null);
	}

	@SagaOrchestrated
	public void deleteInventory(Long id) throws JsonProcessingException {

		Item item = repo.findById(id).orElseThrow();

		String beforeStateJson = objectMapper.writeValueAsString(item);
		Item beforeState = objectMapper.readValue(beforeStateJson, Item.class);

		String sagaId = SagaAspect.context().getSagaId();

		Saga.compensate(() -> {
			repo.save(beforeState); // re-insert
			sagaLogger.logRollback(sagaId, "ITEM", id);
		});

		repo.delete(item);
		sagaLogger.logDelete(sagaId, "ITEM", id, beforeStateJson);

		restTemplate.delete("http://localhost:9091/orders/delete?sagaId=" + sagaId + "&inventoryId=" + item.getId());
	}

	@SagaOrchestrated
	public void createInventoryV2(String name, int qty) {

		String sagaId = SagaAspect.context().getSagaId();

		log.info("Starting Saga: {}", sagaId);

		// ==================================================
		// STEP 1 - CREATE INVENTORY
		// ==================================================

		Item item = new Item();
		item.setName(name);
		item.setQuantity(qty);

		item = repo.save(item);

		Long itemId = item.getId();

		sagaLogger.logCreate(sagaId, "ITEM", itemId);

		// rollback inventory
		Saga.compensate(() -> {
			log.info("Rollback Inventory {}", itemId);
			repo.deleteById(itemId);
			sagaLogger.logRollback(sagaId, "ITEM", itemId);
		});

		// ==================================================
		// STEP 2 - CREATE ORDER
		// ==================================================

		log.info("Calling Order Service");
		Long orderId =restTemplate.postForObject(
				"http://localhost:9091/orders/create-flow2?inventoryId=" + itemId +
						"&sagaId=" + sagaId,
				null, Long.class
		);

		// rollback order remotely
//		Saga.compensate(() -> {
//			log.info("Rollback Order {}", orderId);
//			restTemplate.delete("http://localhost:9091/orders/rollback/" + orderId + "?sagaId=" + sagaId);
//			sagaLogger.logRollback(sagaId, "ORDER", orderId);
//		});
		Saga.remoteCompensate("ORDER", "DELETE", orderServiceURL + orderId + "?sagaId=" + sagaId);


		// ==================================================
		// STEP 3 - CREATE BILLING
		// ==================================================

		log.info("Calling Billing Service");

		restTemplate.postForObject(
				"http://localhost:8082/billing/create?sagaId=" + sagaId +
						"&inventoryId=" + item.getId(),
				null, Void.class
		);


		log.info("Saga Completed Successfully {}", sagaId);
	}

	@SagaOrchestrated
	public void updateInventoryV2(Long id, int newQty) throws JsonProcessingException {

		Item item = repo.findById(id).orElseThrow(() -> new RuntimeException("Item not found"));

		String beforeStateJson = objectMapper.writeValueAsString(item);
		Item beforeState = objectMapper.readValue(beforeStateJson, Item.class);

		String sagaId = SagaAspect.context().getSagaId();
		
		log.info("Starting updateInventory2 for item {} with SAGA id : {}", id, sagaId);

		// Register local compensation for inventory
		Saga.compensate(() -> {
			repo.save(beforeState);
			sagaLogger.logRollback(sagaId, "ITEM", id);
			log.info("Compensated inventory update for item {}", id);
		});

		// Execute the inventory update
		item.setQuantity(newQty);
		repo.save(item);
		sagaLogger.logUpdate(sagaId, "ITEM", id, beforeStateJson);
		log.info("Updated inventory item {} to quantity {}", id, newQty);

		try {
			// Call order service for update
			restTemplate.put("http://localhost:9091/orders/update-flow2?sagaId=" + sagaId + "&inventoryId=" + id, null);
			log.info("Successfully called order service for SAGA: {}", sagaId);
			
			// Register remote compensation for order service
			// This ensures order will be rolled back if billing fails
			Saga.remoteCompensate("ORDER", "POST", 
				"http://localhost:9091/orders/compensate/update?sagaId=" + sagaId + "&inventoryId=" + id);
			log.info("Registered order compensation for SAGA: {}", sagaId);

			// Call billing service for update
			restTemplate.put("http://localhost:8082/billing/update?sagaId=" + sagaId + "&inventoryId=" + item.getId(), 
					null, Void.class);
			log.info("Successfully called billing service for SAGA: {}", sagaId);
			
		} catch (Exception ex) {
			log.error("Service call failed in updateInventoryV2 for SAGA {}: {}", sagaId, ex.getMessage());
			// Exception will trigger automatic rollback via SagaAspect
			throw new RuntimeException("Update operation failed", ex);
		}

		log.info("updateInventoryV2 completed successfully for SAGA: {}", sagaId);
	}

	@SagaOrchestrated
	public void deleteInventoryV2(Long id) throws JsonProcessingException {

		Item item = repo.findById(id).orElseThrow();

		String beforeStateJson = objectMapper.writeValueAsString(item);
		Item beforeState = objectMapper.readValue(beforeStateJson, Item.class);

		String sagaId = SagaAspect.context().getSagaId();

		Saga.compensate(() -> {
			repo.save(beforeState); // re-insert
			sagaLogger.logRollback(sagaId, "ITEM", id);
		});

		repo.delete(item);
		sagaLogger.logDelete(sagaId, "ITEM", id, beforeStateJson);

		restTemplate
				.delete("http://localhost:9091/orders/delete-flow2?sagaId=" + sagaId + "&inventoryId=" + item.getId());
		log.info("Order deleted with saga id : " + sagaId);

		restTemplate.delete("http://localhost:8082/billing/delete?sagaId=" + sagaId + "&inventoryId=" + item.getId(),
				null, Void.class);
	}

}
