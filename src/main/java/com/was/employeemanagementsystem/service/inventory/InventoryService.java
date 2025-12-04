package com.was.employeemanagementsystem.service.inventory;

import com.was.employeemanagementsystem.dto.inventory.InventoryItemDTO;
import com.was.employeemanagementsystem.dto.inventory.InventoryTransactionDTO;
import com.was.employeemanagementsystem.entity.inventory.InventoryCategory;
import com.was.employeemanagementsystem.entity.inventory.InventoryItem;
import com.was.employeemanagementsystem.entity.inventory.InventoryTransaction;
import com.was.employeemanagementsystem.repository.inventory.InventoryCategoryRepository;
import com.was.employeemanagementsystem.repository.inventory.InventoryItemRepository;
import com.was.employeemanagementsystem.repository.inventory.InventoryTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryCategoryRepository inventoryCategoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    @Transactional(readOnly = true)
    public List<InventoryItemDTO> getAllItems(String organizationUuid) {
        log.info("📦 Fetching all inventory items for organization: {}", organizationUuid);
        List<InventoryItem> items = inventoryItemRepository.findByOrganizationUuidAndIsActiveTrue(organizationUuid);
        return items.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventoryItemDTO getItemById(Long id) {
        log.info("📦 Fetching inventory item by ID: {}", id);
        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory item not found with ID: " + id));
        return convertToDTO(item);
    }

    @Transactional
    public InventoryItemDTO createItem(InventoryItemDTO dto, String organizationUuid, Long userId) {
        log.info("📦 Creating new inventory item: {} for organization: {}", dto.getName(), organizationUuid);

        if (inventoryItemRepository.existsByItemCodeAndOrganizationUuid(dto.getItemCode(), organizationUuid)) {
            throw new RuntimeException("Item code already exists: " + dto.getItemCode());
        }

        InventoryItem item = new InventoryItem();
        item.setItemCode(dto.getItemCode());
        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setQuantity(dto.getQuantity() != null ? dto.getQuantity() : 0);
        item.setReorderLevel(dto.getReorderLevel());
        item.setUnitPrice(dto.getUnitPrice());
        item.setUnit(dto.getUnit());
        item.setSupplier(dto.getSupplier());
        item.setBarcode(dto.getBarcode());
        item.setOrganizationUuid(organizationUuid);
        item.setCreatedBy(userId);
        item.setUpdatedBy(userId);

        if (dto.getCategoryId() != null) {
            InventoryCategory category = inventoryCategoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            item.setCategory(category);
        }

        InventoryItem saved = inventoryItemRepository.save(item);
        log.info("✅ Inventory item created successfully with ID: {}", saved.getId());
        return convertToDTO(saved);
    }

    @Transactional
    public InventoryItemDTO updateItem(Long id, InventoryItemDTO dto, Long userId) {
        log.info("📦 Updating inventory item ID: {}", id);

        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory item not found"));

        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setReorderLevel(dto.getReorderLevel());
        item.setUnitPrice(dto.getUnitPrice());
        item.setUnit(dto.getUnit());
        item.setSupplier(dto.getSupplier());
        item.setBarcode(dto.getBarcode());
        item.setIsActive(dto.getIsActive());
        item.setUpdatedBy(userId);

        if (dto.getCategoryId() != null) {
            InventoryCategory category = inventoryCategoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            item.setCategory(category);
        }

        InventoryItem updated = inventoryItemRepository.save(item);
        log.info("✅ Inventory item updated successfully");
        return convertToDTO(updated);
    }

    @Transactional
    public void deleteItem(Long id) {
        log.info("🗑️ Soft deleting inventory item ID: {}", id);
        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory item not found"));
        item.setIsActive(false);
        inventoryItemRepository.save(item);
        log.info("✅ Inventory item soft deleted successfully");
    }

    @Transactional
    public InventoryTransactionDTO recordTransaction(InventoryTransactionDTO dto, String organizationUuid, Long userId) {
        log.info("📝 Recording inventory transaction for item ID: {}", dto.getItemId());

        InventoryItem item = inventoryItemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new RuntimeException("Inventory item not found"));

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setItem(item);
        transaction.setTransactionType(dto.getTransactionType());
        transaction.setQuantity(dto.getQuantity());
        transaction.setUnitPrice(dto.getUnitPrice());
        transaction.setReferenceNumber(dto.getReferenceNumber());
        transaction.setRemarks(dto.getRemarks());
        transaction.setOrganizationUuid(organizationUuid);
        transaction.setPerformedBy(userId);

        // Update item quantity
        int multiplier = dto.getTransactionType().getQuantityMultiplier();
        int newQuantity = item.getQuantity() + (dto.getQuantity() * multiplier);

        if (newQuantity < 0) {
            throw new RuntimeException("Insufficient inventory. Current quantity: " + item.getQuantity());
        }

        item.setQuantity(newQuantity);
        inventoryItemRepository.save(item);

        InventoryTransaction saved = inventoryTransactionRepository.save(transaction);
        log.info("✅ Transaction recorded. New quantity for {}: {}", item.getName(), newQuantity);

        return convertTransactionToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<InventoryItemDTO> getItemsNeedingReorder(String organizationUuid) {
        log.info("📦 Fetching items needing reorder for organization: {}", organizationUuid);
        List<InventoryItem> items = inventoryItemRepository.findItemsNeedingReorder(organizationUuid);
        return items.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryItemDTO> searchItems(String organizationUuid, String search) {
        log.info("🔍 Searching inventory items with query: {}", search);
        List<InventoryItem> items = inventoryItemRepository.searchItems(organizationUuid, search);
        return items.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryTransactionDTO> getTransactionsByItem(Long itemId) {
        log.info("📝 Fetching transactions for item ID: {}", itemId);
        List<InventoryTransaction> transactions = inventoryTransactionRepository.findByItemIdOrderByTransactionDateDesc(itemId);
        return transactions.stream().map(this::convertTransactionToDTO).collect(Collectors.toList());
    }

    private InventoryItemDTO convertToDTO(InventoryItem item) {
        InventoryItemDTO dto = new InventoryItemDTO();
        dto.setId(item.getId());
        dto.setItemCode(item.getItemCode());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setQuantity(item.getQuantity());
        dto.setReorderLevel(item.getReorderLevel());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setUnit(item.getUnit());
        dto.setSupplier(item.getSupplier());
        dto.setBarcode(item.getBarcode());
        dto.setImagePath(item.getImagePath());
        dto.setIsActive(item.getIsActive());
        dto.setNeedsReorder(item.needsReorder());
        dto.setOrganizationUuid(item.getOrganizationUuid());

        if (item.getCategory() != null) {
            dto.setCategoryId(item.getCategory().getId());
            dto.setCategoryName(item.getCategory().getName());
        }

        return dto;
    }

    private InventoryTransactionDTO convertTransactionToDTO(InventoryTransaction transaction) {
        InventoryTransactionDTO dto = new InventoryTransactionDTO();
        dto.setId(transaction.getId());
        dto.setItemId(transaction.getItem().getId());
        dto.setItemCode(transaction.getItem().getItemCode());
        dto.setItemName(transaction.getItem().getName());
        dto.setTransactionType(transaction.getTransactionType());
        dto.setQuantity(transaction.getQuantity());
        dto.setUnitPrice(transaction.getUnitPrice());
        dto.setTotalAmount(transaction.getTotalAmount());
        dto.setReferenceNumber(transaction.getReferenceNumber());
        dto.setRemarks(transaction.getRemarks());
        dto.setPerformedBy(transaction.getPerformedBy());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setOrganizationUuid(transaction.getOrganizationUuid());
        return dto;
    }
}

