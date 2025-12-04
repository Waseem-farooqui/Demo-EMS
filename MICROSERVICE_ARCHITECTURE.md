# 🏗️ Microservice Architecture - Multi-System Support

## 📋 Overview

The Employee Management System has been transformed into a **microservice-based multi-tenant architecture** that supports multiple business domains based on the organization's system type. The same codebase now serves:

1. **Employee Management System (EMS)** - Full employee lifecycle management
2. **Inventory Management System (IMS)** - Complete inventory tracking and control
3. **Hybrid System** - Both EMS and IMS features combined

## 🎯 Key Features

### System Type Based Routing
- Each organization is assigned a `system_type` (EMPLOYEE_MANAGEMENT, INVENTORY_MANAGEMENT, or HYBRID)
- Users see different features and UI based on their organization's system type
- API endpoints are contextually available based on system type

### Multi-Tenant Support
- Complete data isolation per organization
- Shared infrastructure with organization-scoped data
- Each organization can choose their business domain

### Feature Flags
- Dynamic feature availability based on organization configuration
- Frontend adapts UI/UX based on available features
- Backend enforces feature access control

## 📦 Architecture Components

### 1. System Type Enum
```java
public enum SystemType {
    EMPLOYEE_MANAGEMENT("EMS", "Employee Management System"),
    INVENTORY_MANAGEMENT("IMS", "Inventory Management System"),
    HYBRID("HYBRID", "Hybrid System");
}
```

### 2. Organization Entity
- **New Field**: `systemType` - Determines which features are available

### 3. Inventory Module (New)

#### Entities:
- **InventoryItem** - Products/items tracked in inventory
- **InventoryCategory** - Product categorization
- **InventoryTransaction** - All inventory movements (purchase, sale, adjustment, etc.)

#### Transaction Types:
- PURCHASE - Add stock
- SALE - Remove stock
- RETURN - Return to inventory
- ADJUSTMENT_IN/OUT - Manual adjustments
- DAMAGE - Record damaged goods
- TRANSFER_IN/OUT - Inter-location transfers

#### Repositories:
- `InventoryItemRepository`
- `InventoryCategoryRepository`
- `InventoryTransactionRepository`

#### Services:
- `InventoryService` - Core inventory operations
- `SystemContextService` - Determines system features

#### Controllers:
- `InventoryController` - `/api/inventory/*` endpoints
- `SystemContextController` - `/api/system/*` endpoints

## 🔌 API Endpoints

### System Context API

#### Get System Context
```http
GET /api/system/context
Authorization: Bearer <token>

Response:
{
  "systemType": "IMS",
  "systemName": "Inventory Management System",
  "features": {
    "employeeManagement": false,
    "inventoryManagement": true
  },
  "organizationUuid": "xxx-xxx-xxx",
  "username": "admin@company.com",
  "role": "ADMIN"
}
```

#### Check Feature Availability
```http
GET /api/system/features/inventory
Authorization: Bearer <token>

Response:
{
  "available": true
}
```

### Inventory Management API

#### Get All Items
```http
GET /api/inventory/items
Authorization: Bearer <token>
```

#### Get Item by ID
```http
GET /api/inventory/items/{id}
Authorization: Bearer <token>
```

#### Create Item
```http
POST /api/inventory/items
Authorization: Bearer <token>
Content-Type: application/json

{
  "itemCode": "ITEM001",
  "name": "Product Name",
  "description": "Description",
  "categoryId": 1,
  "quantity": 100,
  "reorderLevel": 20,
  "unitPrice": 10.50,
  "unit": "pieces",
  "supplier": "Supplier Name",
  "barcode": "123456789"
}
```

#### Update Item
```http
PUT /api/inventory/items/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Updated Name",
  "unitPrice": 12.00,
  ...
}
```

#### Record Transaction
```http
POST /api/inventory/transactions
Authorization: Bearer <token>
Content-Type: application/json

{
  "itemId": 1,
  "transactionType": "PURCHASE",
  "quantity": 50,
  "unitPrice": 10.00,
  "referenceNumber": "PO-2025-001",
  "remarks": "Monthly stock replenishment"
}
```

#### Get Items Needing Reorder
```http
GET /api/inventory/items/reorder
Authorization: Bearer <token>
```

#### Search Items
```http
GET /api/inventory/items/search?query=laptop
Authorization: Bearer <token>
```

#### Get Item Transactions
```http
GET /api/inventory/items/{id}/transactions
Authorization: Bearer <token>
```

## 🗄️ Database Schema

### Organizations Table (Updated)
```sql
ALTER TABLE organizations 
ADD COLUMN system_type VARCHAR(50) NOT NULL DEFAULT 'EMPLOYEE_MANAGEMENT';
```

### Inventory Categories Table
```sql
CREATE TABLE inventory_categories (
    id BIGINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    organization_id BIGINT NOT NULL,
    organization_uuid VARCHAR(36) NOT NULL,
    is_active BOOLEAN,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_category_code_org (code, organization_uuid)
);
```

### Inventory Items Table
```sql
CREATE TABLE inventory_items (
    id BIGINT PRIMARY KEY,
    item_code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    category_id BIGINT,
    quantity INT DEFAULT 0,
    reorder_level INT DEFAULT 10,
    unit_price DECIMAL(10, 2),
    unit VARCHAR(50),
    supplier VARCHAR(100),
    barcode VARCHAR(100),
    image_path VARCHAR(500),
    image_data LONGBLOB,
    organization_id BIGINT NOT NULL,
    organization_uuid VARCHAR(36) NOT NULL,
    is_active BOOLEAN,
    created_at DATETIME,
    updated_at DATETIME,
    created_by BIGINT,
    updated_by BIGINT,
    UNIQUE KEY uk_item_code_org (item_code, organization_uuid),
    FOREIGN KEY (category_id) REFERENCES inventory_categories(id)
);
```

### Inventory Transactions Table
```sql
CREATE TABLE inventory_transactions (
    id BIGINT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2),
    total_amount DECIMAL(10, 2),
    reference_number VARCHAR(100),
    remarks VARCHAR(1000),
    organization_id BIGINT NOT NULL,
    organization_uuid VARCHAR(36) NOT NULL,
    performed_by BIGINT NOT NULL,
    transaction_date DATETIME,
    created_at DATETIME,
    FOREIGN KEY (item_id) REFERENCES inventory_items(id)
);
```

## 🚀 Deployment Steps

### 1. Run Database Migration
```bash
mysql -u root -p employee_management_system < database/microservice-architecture-migration.sql
```

### 2. Update Existing Organizations
```sql
-- Set system type for organizations
UPDATE organizations 
SET system_type = 'EMPLOYEE_MANAGEMENT' 
WHERE system_type IS NULL;

-- Enable inventory for specific organizations
UPDATE organizations 
SET system_type = 'INVENTORY_MANAGEMENT' 
WHERE id IN (1, 2, 3);

-- Enable hybrid mode
UPDATE organizations 
SET system_type = 'HYBRID' 
WHERE id = 4;
```

### 3. Rebuild Backend
```bash
cd C:\Users\waseem.uddin\EmployeeManagementSystem
mvn clean package -DskipTests
```

### 4. Restart Application
```bash
# Docker deployment
docker compose down
docker compose build --no-cache
docker compose up -d

# Or standalone
java -jar target/EmployeeManagementSystem-0.0.1-SNAPSHOT.jar
```

## 🎨 Frontend Integration

### 1. Check System Context on Login
```typescript
// After successful login
this.http.get('/api/system/context').subscribe(context => {
  localStorage.setItem('systemType', context.systemType);
  localStorage.setItem('features', JSON.stringify(context.features));
  
  // Route based on system type
  if (context.features.inventoryManagement) {
    this.router.navigate(['/inventory/dashboard']);
  } else {
    this.router.navigate(['/dashboard']);
  }
});
```

### 2. Conditional Menu Items
```typescript
// In navigation component
get showInventoryMenu(): boolean {
  const features = JSON.parse(localStorage.getItem('features') || '{}');
  return features.inventoryManagement === true;
}

get showEmployeeMenu(): boolean {
  const features = JSON.parse(localStorage.getItem('features') || '{}');
  return features.employeeManagement === true;
}
```

### 3. Feature Guards
```typescript
// inventory.guard.ts
export class InventoryGuard implements CanActivate {
  canActivate(): boolean {
    const features = JSON.parse(localStorage.getItem('features') || '{}');
    if (!features.inventoryManagement) {
      this.router.navigate(['/unauthorized']);
      return false;
    }
    return true;
  }
}
```

## 📊 Use Cases

### Scenario 1: Pure Employee Management Organization
```javascript
Organization: ABC Corp
System Type: EMPLOYEE_MANAGEMENT

Features Available:
✅ Employee management
✅ Leave management
✅ Attendance tracking
✅ Document management
✅ Rota scheduling
❌ Inventory management
```

### Scenario 2: Pure Inventory Management Organization
```javascript
Organization: XYZ Warehouse
System Type: INVENTORY_MANAGEMENT

Features Available:
❌ Employee management
❌ Leave management
❌ Attendance tracking
✅ Inventory tracking
✅ Stock management
✅ Purchase/Sales recording
✅ Low stock alerts
```

### Scenario 3: Hybrid Organization
```javascript
Organization: DEF Enterprise
System Type: HYBRID

Features Available:
✅ Employee management
✅ Leave management
✅ Attendance tracking
✅ Document management
✅ Rota scheduling
✅ Inventory tracking
✅ Stock management
✅ Purchase/Sales recording
```

## 🔒 Security

### Role-Based Access Control
- **SUPER_ADMIN**: Full access to all features
- **ADMIN**: Manage employees/inventory within their organization
- **USER**: View-only access

### Organization Isolation
- All queries filtered by `organization_uuid`
- Cross-organization data access prevented
- API endpoints validate organization context

## 🧪 Testing

### Test Inventory Features
```bash
# Login
POST /api/auth/login
{
  "username": "admin@warehouse.com",
  "password": "password"
}

# Get system context
GET /api/system/context
# Should return: systemType: "INVENTORY_MANAGEMENT"

# Create inventory item
POST /api/inventory/items
{
  "itemCode": "LAPTOP001",
  "name": "Dell Laptop",
  "quantity": 10,
  "reorderLevel": 5,
  "unitPrice": 750.00
}

# Record purchase transaction
POST /api/inventory/transactions
{
  "itemId": 1,
  "transactionType": "PURCHASE",
  "quantity": 20,
  "unitPrice": 750.00
}

# Check low stock items
GET /api/inventory/items/reorder
```

## 📈 Benefits

1. **Code Reusability**: Single codebase serves multiple business domains
2. **Scalability**: Easy to add new system types (CRM, Sales, etc.)
3. **Flexibility**: Organizations choose their required features
4. **Cost Effective**: Shared infrastructure reduces operational costs
5. **Maintainability**: Centralized updates benefit all system types
6. **Multi-Tenancy**: Complete data isolation with shared platform

## 🛠️ Future Enhancements

- **CRM Module**: Customer relationship management
- **Sales Module**: Sales order processing
- **Accounting Module**: Financial management
- **Manufacturing Module**: Production planning
- **Reporting Module**: Cross-system analytics
- **API Gateway**: Centralized routing and load balancing
- **Service Mesh**: Inter-service communication
- **Event Sourcing**: Audit trail and event replay

## 📝 Migration Checklist

- [x] Add SystemType enum
- [x] Update Organization entity with systemType field
- [x] Create inventory entities (Item, Category, Transaction)
- [x] Create inventory repositories
- [x] Create inventory service layer
- [x] Create inventory REST controllers
- [x] Create SystemContextService
- [x] Create SystemContextController
- [x] Update entity scanning configuration
- [x] Create database migration script
- [x] Add documentation

## ✅ Success Criteria

- Organizations can be configured with different system types
- Users see only relevant features based on organization type
- Inventory module fully functional for IMS organizations
- Employee module fully functional for EMS organizations
- Hybrid organizations have access to both modules
- Complete data isolation maintained
- All APIs properly secured with role-based access

---

**Status**: ✅ **Microservice Architecture Implementation Complete**

The system now supports multi-domain business operations through a unified platform. Organizations can be configured as Employee Management, Inventory Management, or Hybrid systems, with the application dynamically adapting to provide the appropriate features.

