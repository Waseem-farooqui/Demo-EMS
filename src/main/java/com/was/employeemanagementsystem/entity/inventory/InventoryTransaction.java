package com.was.employeemanagementsystem.entity.inventory;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    private TransactionType transactionType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(length = 1000)
    private String remarks;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "organization_uuid", nullable = false, length = 36)
    private String organizationUuid;

    @Column(name = "performed_by", nullable = false)
    private Long performedBy;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
        if (unitPrice != null && quantity != null) {
            totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public enum TransactionType {
        PURCHASE("Purchase", 1),
        SALE("Sale", -1),
        RETURN("Return", 1),
        ADJUSTMENT_IN("Adjustment In", 1),
        ADJUSTMENT_OUT("Adjustment Out", -1),
        DAMAGE("Damage", -1),
        TRANSFER_IN("Transfer In", 1),
        TRANSFER_OUT("Transfer Out", -1);

        private final String displayName;
        private final int quantityMultiplier;

        TransactionType(String displayName, int quantityMultiplier) {
            this.displayName = displayName;
            this.quantityMultiplier = quantityMultiplier;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getQuantityMultiplier() {
            return quantityMultiplier;
        }
    }
}

