#!/bin/bash

# Quick test script for the enhanced update flow
# Usage: ./test-update-flow.sh

BASE_URL_INVENTORY="http://localhost:9090"
BASE_URL_ORDER="http://localhost:9091" 
BASE_URL_BILLING="http://localhost:8082"

echo "🧪 Enhanced SAGA Coordinator - Update Flow Testing"
echo "=================================================="
echo ""

# Function to make curl request with better output
make_request() {
    local method=$1
    local url=$2
    local description=$3
    
    echo "📡 $description"
    echo "   Request: $method $url"
    
    response=$(curl -s -w "HTTP_STATUS:%{http_code}" -X "$method" "$url")
    
    # Extract HTTP status and body
    http_status=$(echo "$response" | grep -o "HTTP_STATUS:[0-9]*" | cut -d: -f2)
    body=$(echo "$response" | sed 's/HTTP_STATUS:[0-9]*$//')
    
    if [[ $http_status -ge 200 && $http_status -lt 300 ]]; then
        echo "   ✅ Success ($http_status): $body"
    else
        echo "   ❌ Failed ($http_status): $body"
    fi
    echo ""
}

# Function to check if services are running
check_services() {
    echo "🔍 Checking if services are running..."
    
    services=(
        "Inventory:$BASE_URL_INVENTORY/actuator/health"
        "Order:$BASE_URL_ORDER/actuator/health"
        "Billing:$BASE_URL_BILLING/actuator/health"
    )
    
    all_healthy=true
    
    for service in "${services[@]}"; do
        name=$(echo "$service" | cut -d: -f1)
        url=$(echo "$service" | cut -d: -f2-)
        
        status=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")
        
        if [[ $status == "200" ]]; then
            echo "   ✅ $name Service: Running"
        else
            echo "   ❌ $name Service: Not available (HTTP $status)"
            all_healthy=false
        fi
    done
    
    echo ""
    
    if [[ $all_healthy == false ]]; then
        echo "⚠️  Some services are not running. Please start all services before testing."
        echo ""
        exit 1
    fi
}

# Main test sequence
main() {
    # Check services first
    check_services
    
    echo "🚀 Starting Update Flow Tests..."
    echo ""
    
    # Step 1: Create test item
    make_request "POST" "$BASE_URL_INVENTORY/inventory/create-flow2" \
        "Creating test inventory item"
    
    # Step 2: Test current flow-2 update
    make_request "PUT" "$BASE_URL_INVENTORY/inventory/update-flow2?ItemId=1&qty=25" \
        "Testing current Flow-2 update (shows the issue)"
    
    # Step 3: Test enhanced update flow
    make_request "PUT" "$BASE_URL_INVENTORY/inventory/update-enhanced?ItemId=1&qty=30" \
        "Testing enhanced update flow (shows the solution)"
    
    # Step 4: Check compensation endpoint health
    echo "🏥 Checking Compensation Endpoint Health..."
    make_request "GET" "$BASE_URL_ORDER/orders/compensate/health" \
        "Order compensation health check"
    
    make_request "GET" "$BASE_URL_BILLING/billing/compensate/health" \
        "Billing compensation health check"
    
    # Step 5: Test compensation functionality
    echo "🔧 Testing Compensation Functionality..."
    make_request "GET" "$BASE_URL_ORDER/orders/compensate/can-compensate?sagaId=test-123&inventoryId=1" \
        "Checking if order can be compensated"
    
    make_request "GET" "$BASE_URL_BILLING/billing/compensate/can-compensate?sagaId=test-123&inventoryId=1" \
        "Checking if billing can be compensated"
    
    # Step 6: Simulate failure scenario (optional)
    echo "💥 Testing Failure Scenario (Simulated)..."
    make_request "POST" "$BASE_URL_BILLING/billing/compensate/simulate-failure?sagaId=test-failure&inventoryId=1" \
        "Simulating billing failure to test rollback"
    
    echo "🎉 Test Summary:"
    echo "==============="
    echo ""
    echo "✅ What was tested:"
    echo "   • Current Flow-2 update (demonstrates the issue)"
    echo "   • Enhanced coordinator update (demonstrates the solution)"
    echo "   • Compensation endpoint health checks"
    echo "   • Compensation capability checks"
    echo "   • Failure simulation and rollback"
    echo ""
    echo "🔍 Key Difference:"
    echo "   • Current Flow: Only inventory rolls back on billing failure"
    echo "   • Enhanced Flow: ALL services roll back automatically"
    echo ""
    echo "📖 For detailed explanation, see:"
    echo "   • COORDINATOR_SOLUTION_DEMO.md"
    echo "   • README_COORDINATOR_SOLUTION.md"
    echo "   • CURL_TESTING_GUIDE.md"
}

# Run the tests
main