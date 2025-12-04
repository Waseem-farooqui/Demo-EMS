package com.was.employeemanagementsystem.controller.inventory;

import com.was.employeemanagementsystem.dto.inventory.InventoryItemDTO;
import com.was.employeemanagementsystem.dto.inventory.InventoryTransactionDTO;
import com.was.employeemanagementsystem.security.UserDetailsImpl;
import com.was.employeemanagementsystem.service.inventory.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("${api.base-path:/api}/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<List<InventoryItemDTO>> getAllItems(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            log.info("📦 GET /api/inventory/items - User: {}, Org: {}",
                    userDetails.getUsername(), userDetails.getOrganizationUuid());
            List<InventoryItemDTO> items = inventoryService.getAllItems(userDetails.getOrganizationUuid());
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            log.error("❌ Failed to fetch inventory items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<InventoryItemDTO> getItemById(@PathVariable Long id) {
        try {
            log.info("📦 GET /api/inventory/items/{}", id);
            InventoryItemDTO item = inventoryService.getItemById(id);
            return ResponseEntity.ok(item);
        } catch (RuntimeException e) {
            log.error("❌ Item not found: {}", id, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("❌ Failed to fetch inventory item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<?> createItem(
            @RequestBody InventoryItemDTO dto,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            log.info("📦 POST /api/inventory/items - Creating: {}", dto.getName());
            InventoryItemDTO created = inventoryService.createItem(
                    dto,
                    userDetails.getOrganizationUuid(),
                    userDetails.getId()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            log.error("❌ Failed to create inventory item", e);
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("❌ Failed to create inventory item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<?> updateItem(
            @PathVariable Long id,
            @RequestBody InventoryItemDTO dto,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            log.info("📦 PUT /api/inventory/items/{} - Updating", id);
            InventoryItemDTO updated = inventoryService.updateItem(id, dto, userDetails.getId());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.error("❌ Failed to update inventory item", e);
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("❌ Failed to update inventory item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> deleteItem(@PathVariable Long id) {
        try {
            log.info("🗑️ DELETE /api/inventory/items/{}", id);
            inventoryService.deleteItem(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Item deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Failed to delete inventory item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/transactions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<?> recordTransaction(
            @RequestBody InventoryTransactionDTO dto,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            log.info("📝 POST /api/inventory/transactions - Recording transaction");
            InventoryTransactionDTO transaction = inventoryService.recordTransaction(
                    dto,
                    userDetails.getOrganizationUuid(),
                    userDetails.getId()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
        } catch (RuntimeException e) {
            log.error("❌ Failed to record transaction", e);
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("❌ Failed to record transaction", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/items/{id}/transactions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<List<InventoryTransactionDTO>> getTransactionsByItem(@PathVariable Long id) {
        try {
            log.info("📝 GET /api/inventory/items/{}/transactions", id);
            List<InventoryTransactionDTO> transactions = inventoryService.getTransactionsByItem(id);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            log.error("❌ Failed to fetch transactions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/items/reorder")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<InventoryItemDTO>> getItemsNeedingReorder(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            log.info("📦 GET /api/inventory/items/reorder");
            List<InventoryItemDTO> items = inventoryService.getItemsNeedingReorder(
                    userDetails.getOrganizationUuid()
            );
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            log.error("❌ Failed to fetch items needing reorder", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/items/search")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<List<InventoryItemDTO>> searchItems(
            @RequestParam String query,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            log.info("🔍 GET /api/inventory/items/search?query={}", query);
            List<InventoryItemDTO> items = inventoryService.searchItems(
                    userDetails.getOrganizationUuid(),
                    query
            );
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            log.error("❌ Failed to search items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

