package com.example.saga.coordinator;

/**
 * Interface for standardized compensation endpoints across all services.
 * Services implementing this interface provide consistent rollback capabilities.
 */
public interface CompensationEndpoint {
    
    /**
     * Execute compensation for a specific entity/operation.
     * 
     * @param sagaId The SAGA ID for correlation and logging
     * @param entityId The ID of the entity to compensate (optional)
     * @param compensationData Any additional data needed for compensation
     * @return CompensationResult indicating success/failure
     */
    CompensationResult compensate(String sagaId, String entityId, String compensationData);
    
    /**
     * Check if compensation is possible for a given entity.
     * 
     * @param sagaId The SAGA ID for correlation
     * @param entityId The ID of the entity to check
     * @return true if compensation is possible, false otherwise
     */
    boolean canCompensate(String sagaId, String entityId);
    
    /**
     * Get the service name for this compensation endpoint.
     * 
     * @return The service name
     */
    String getServiceName();
    
    /**
     * Result of a compensation operation.
     */
    class CompensationResult {
        private final boolean success;
        private final String message;
        private final String errorCode;
        
        public CompensationResult(boolean success, String message, String errorCode) {
            this.success = success;
            this.message = message;
            this.errorCode = errorCode;
        }
        
        public static CompensationResult success(String message) {
            return new CompensationResult(true, message, null);
        }
        
        public static CompensationResult failure(String message, String errorCode) {
            return new CompensationResult(false, message, errorCode);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getErrorCode() { return errorCode; }
        
        @Override
        public String toString() {
            return String.format("CompensationResult{success=%s, message='%s', errorCode='%s'}", 
                               success, message, errorCode);
        }
    }
}