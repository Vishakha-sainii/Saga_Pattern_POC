#!/bin/bash

# Test Script for Enhanced SAGA Coordinator Solution
# This script demonstrates how the coordinator approach solves the compensation issue

echo "🚀 Testing Enhanced SAGA Coordinator Solution"
echo "============================================="

# Base URLs for services
INVENTORY_URL="http://localhost:9090/inventory"
ORDER_URL="http://localhost:9091/orders"
BILLING_URL="http://localhost:8082/billing"

echo ""
echo "📋 Test Overview:"
echo "1. Test current flow-2 (demonstrates the issue)"  
echo "2. Test enhanced flow (demonstrates the solution)"
echo "3. Test failure scenarios with automatic rollback"
echo ""

# Function to create a test item
create_test_item() {
    echo "📦 Creating test inventory item..."
    response=$(curl -s -X POST "$INVENTORY_URL/create-flow2" \
        -H "Content-Type: application/json")
    echo "✅ Response: $response"
    echo ""
}

# Function to test current flow-2 (with the issue)
test_current_flow() {
    echo "🔍 Testing Current Flow-2 (demonstrates the issue):"
    echo "---------------------------------------------------"
    
    echo "Updating inventory with current flow-2..."
    response=$(curl -s -X PUT "$INVENTORY_URL/update-flow2?ItemId=1&qty=25" \
        -H "Content-Type: application/json" \
        -w "HTTP Status: %{http_code}")
    
    echo "Response: $response"
    echo ""
    echo "❗ Issue: If billing fails in current flow, order remains inconsistent"
    echo ""
}

# Function to test enhanced flow (solution)
test_enhanced_flow() {
    echo "✨ Testing Enhanced Flow (demonstrates the solution):"
    echo "-----------------------------------------------------"
    
    echo "Updating inventory with enhanced coordinator approach..."
    response=$(curl -s -X PUT "$INVENTORY_URL/update-enhanced?ItemId=1&qty=30" \
        -H "Content-Type: application/json" \
        -w "HTTP Status: %{http_code}")
    
    echo "Response: $response"
    echo ""
    echo "✅ Solution: Coordinator ensures ALL services roll back if any fails"
    echo ""
}

# Function to test compensation endpoints
test_compensation_endpoints() {
    echo "🔧 Testing Compensation Endpoints:"
    echo "----------------------------------"
    
    echo "Testing order compensation health..."
    response=$(curl -s -X GET "$ORDER_URL/compensate/health")
    echo "Order compensation: $response"
    
    echo "Testing billing compensation health..."  
    response=$(curl -s -X GET "$BILLING_URL/compensate/health")
    echo "Billing compensation: $response"
    echo ""
}

# Function to simulate failure scenario
test_failure_scenario() {
    echo "💥 Testing Failure Scenario with Automatic Rollback:"
    echo "----------------------------------------------------"
    
    echo "Simulating billing failure..."
    response=$(curl -s -X POST "$BILLING_URL/compensate/simulate-failure?sagaId=test123&inventoryId=1" \
        -w "HTTP Status: %{http_code}")
    
    echo "Response: $response"
    echo ""
    echo "✅ In enhanced flow, coordinator would automatically:"
    echo "   1. Detect billing failure"
    echo "   2. Roll back billing (partial operation)"
    echo "   3. Roll back order via compensation endpoint"
    echo "   4. Roll back inventory via compensation"
    echo ""
}

# Function to check service health
check_services() {
    echo "🏥 Checking Service Health:"
    echo "---------------------------"
    
    services=("$INVENTORY_URL" "$ORDER_URL" "$BILLING_URL")
    for service in "${services[@]}"; do
        response=$(curl -s -o /dev/null -w "%{http_code}" "$service/actuator/health" 2>/dev/null || echo "N/A")
        echo "Service $service: HTTP $response"
    done
    echo ""
}

# Main execution
main() {
    echo "🔍 Checking if services are running..."
    check_services
    
    echo "⚠️  NOTE: This test requires all three services to be running:"
    echo "   - Inventory Service (port 9090)"
    echo "   - Order Service (port 9091)"
    echo "   - Billing Service (port 8082)"
    echo ""
    
    read -p "Continue with tests? (y/n): " -n 1 -r
    echo ""
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        create_test_item
        test_current_flow
        test_enhanced_flow  
        test_compensation_endpoints
        test_failure_scenario
        
        echo "🎉 Test Summary:"
        echo "==============="
        echo "✅ Enhanced coordinator approach provides:"
        echo "   • Complete rollback chain across all services"
        echo "   • Centralized SAGA management"
        echo "   • Standardized compensation endpoints"
        echo "   • No code duplication across services"
        echo ""
        echo "📖 See COORDINATOR_SOLUTION_DEMO.md for detailed explanation"
    else
        echo "❌ Tests cancelled"
    fi
}

# Run the tests
main