# 🚀 Curl Testing Guide for Enhanced SAGA Coordinator

## Overview
This guide provides comprehensive curl commands to test the enhanced SAGA coordinator solution for the update flow.

## Prerequisites
Ensure all services are running on their respective ports:
- **Inventory Service**: `localhost:9090`
- **Order Service**: `localhost:9091`  
- **Billing Service**: `localhost:8082`

## Test Scenarios

### 1. 🏗️ Setup - Create Test Data

First, create an inventory item to test updates:

```bash
# Create inventory item (Flow-2)
curl -X POST "http://localhost:9090/inventory/create-flow2" \
  -H "Content-Type: application/json" \
  -v

# Expected Response: "Item Created successfully"
```

### 2. ✅ Test Current Flow-2 Update (Shows the Issue)

```bash
# Test current updateInventoryV2 (demonstrates the compensation issue)
curl -X PUT "http://localhost:9090/inventory/update-flow2?ItemId=1&qty=25" \
  -H "Content-Type: application/json" \
  -v

# Expected Response: "Inventory updated"
# Issue: If billing fails, order service won't roll back
```

### 3. 🎯 Test Enhanced Update Flow (The Solution)

```bash
# Test enhanced update with coordinator approach
curl -X PUT "http://localhost:9090/inventory/update-enhanced?ItemId=1&qty=30" \
  -H "Content-Type: application/json" \
  -v

# Expected Response: "✅ Enhanced inventory update completed successfully"
# Solution: All services roll back if any fails
```

### 4. 🏥 Health Checks

#### Service Health Checks
```bash
# Check inventory service health
curl -X GET "http://localhost:9090/actuator/health" \
  -H "Accept: application/json" \
  -v

# Check order service health  
curl -X GET "http://localhost:9091/actuator/health" \
  -H "Accept: application/json" \
  -v

# Check billing service health
curl -X GET "http://localhost:8082/actuator/health" \
  -H "Accept: application/json" \
  -v
```

#### Compensation Endpoint Health
```bash
# Check order compensation health
curl -X GET "http://localhost:9091/orders/compensate/health" \
  -H "Accept: application/json" \
  -v

# Check billing compensation health
curl -X GET "http://localhost:8082/billing/compensate/health" \
  -H "Accept: application/json" \
  -v
```

### 5. 🔍 Monitor SAGA Status (When Coordinator is Active)

```bash
# View active SAGAs
curl -X GET "http://localhost:9090/saga/coordinator/active" \
  -H "Accept: application/json" \
  -v

# Check specific SAGA status (replace {sagaId} with actual ID)
curl -X GET "http://localhost:9090/saga/coordinator/status/{sagaId}" \
  -H "Accept: application/json" \
  -v

# Coordinator health check
curl -X GET "http://localhost:9090/saga/coordinator/health" \
  -H "Accept: application/json" \
  -v
```

### 6. 💥 Test Failure Scenarios

#### Simulate Billing Failure
```bash
# Simulate billing failure to test rollback mechanism
curl -X POST "http://localhost:8082/billing/compensate/simulate-failure?sagaId=test-123&inventoryId=1" \
  -H "Content-Type: application/json" \
  -v

# Expected: HTTP 500 with error message
# In enhanced flow, this would trigger coordinator rollback
```

#### Manual Rollback Test
```bash
# Manually trigger rollback for a SAGA (replace {sagaId})
curl -X POST "http://localhost:9090/saga/coordinator/rollback/{sagaId}" \
  -H "Content-Type: application/json" \
  -v
```

### 7. 🔧 Direct Compensation Testing

#### Order Service Compensation
```bash
# Test order update compensation
curl -X POST "http://localhost:9091/orders/compensate/update?sagaId=test-123&inventoryId=1" \
  -H "Content-Type: application/json" \
  -v

# Check if order can be compensated
curl -X GET "http://localhost:9091/orders/compensate/can-compensate?sagaId=test-123&inventoryId=1" \
  -H "Accept: application/json" \
  -v
```

#### Billing Service Compensation
```bash
# Test billing update compensation
curl -X POST "http://localhost:8082/billing/compensate/update?sagaId=test-123&inventoryId=1" \
  -H "Content-Type: application/json" \
  -v

# Check if billing can be compensated
curl -X GET "http://localhost:8082/billing/compensate/can-compensate?sagaId=test-123&inventoryId=1" \
  -H "Accept: application/json" \
  -v
```

## 🧪 Complete Test Flow Example

Here's a complete test sequence to demonstrate the solution:

```bash
#!/bin/bash

echo "🚀 Starting Enhanced SAGA Coordinator Test"
echo "=========================================="

# Step 1: Create test item
echo "📦 Step 1: Creating test inventory item..."
curl -s -X POST "http://localhost:9090/inventory/create-flow2"
echo ""

# Step 2: Test current flow (demonstrates issue)
echo "❗ Step 2: Testing current flow-2 (shows the issue)..."
curl -s -X PUT "http://localhost:9090/inventory/update-flow2?ItemId=1&qty=20"
echo ""

# Step 3: Test enhanced flow (shows solution)
echo "✅ Step 3: Testing enhanced flow (shows the solution)..."
curl -s -X PUT "http://localhost:9090/inventory/update-enhanced?ItemId=1&qty=35"
echo ""

# Step 4: Check compensation health
echo "🏥 Step 4: Checking compensation endpoint health..."
curl -s "http://localhost:9091/orders/compensate/health"
echo ""
curl -s "http://localhost:8082/billing/compensate/health"
echo ""

# Step 5: Simulate failure scenario
echo "💥 Step 5: Testing failure scenario..."
curl -s -X POST "http://localhost:8082/billing/compensate/simulate-failure?sagaId=test-456&inventoryId=1"
echo ""

echo "🎉 Test completed!"
echo ""
echo "📖 Key Differences:"
echo "   Current Flow: Only inventory rolls back on billing failure"
echo "   Enhanced Flow: ALL services (inventory + order + billing) roll back"
```

## 📊 Expected Results

### Successful Update (Enhanced Flow)
```json
HTTP/1.1 200 OK
Content-Type: text/plain

✅ Enhanced inventory update completed successfully
```

### Failure Scenario (Enhanced Flow)
```json
HTTP/1.1 500 Internal Server Error
Content-Type: text/plain

❌ Enhanced update failed: Simulated billing failure after partial operation - SAGA: test-456
```

### Compensation Health Check
```json
HTTP/1.1 200 OK
Content-Type: text/plain

Order compensation service is healthy
```

### Active SAGAs Monitor
```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "saga-123": {
    "sagaId": "saga-123",
    "stepCount": 3,
    "serviceCount": 3,
    "services": ["INVENTORY", "ORDER", "BILLING"]
  }
}
```

## 🔍 Debug Commands

### Check Service Logs
```bash
# If using Docker Compose
docker-compose logs inventory-service
docker-compose logs order-service  
docker-compose logs billing-service

# Or check application logs directly
tail -f /path/to/logs/inventory-service.log
tail -f /path/to/logs/order-service.log
tail -f /path/to/logs/billing-service.log
```

### Verify Database State
```bash
# Check inventory items
curl -X GET "http://localhost:9090/inventory/items" -H "Accept: application/json"

# Check orders
curl -X GET "http://localhost:9091/orders" -H "Accept: application/json"

# Check bills  
curl -X GET "http://localhost:8082/billing/bills" -H "Accept: application/json"
```

## 🎯 Key Test Points

1. **✅ Success Path**: Enhanced update completes all services successfully
2. **❌ Failure Path**: When billing fails, ALL services roll back (not just inventory)
3. **🏥 Health Monitoring**: All compensation endpoints are healthy and responsive
4. **📊 SAGA Tracking**: Coordinator properly tracks and manages SAGA lifecycle
5. **🔄 Rollback Chain**: Complete rollback chain execution across all services

Use these curl commands to thoroughly test the enhanced coordinator solution and verify that the compensation issue is completely resolved!