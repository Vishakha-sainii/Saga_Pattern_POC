package com.example.saga.autoconfig;

import com.example.saga.aop.SagaAspect;
import com.example.saga.coordinator.DistributedSaga;
import com.example.saga.coordinator.SagaCoordinatorService;
import com.example.saga.coordinator.SagaStepRegistrar;
import com.example.saga.coordinator.ServiceStepRegistry;
import com.example.saga.core.SagaManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration(proxyBeanMethods = false)
@EnableAspectJAutoProxy
public class SagaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SagaManager sagaManager() {
        return new SagaManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public SagaAspect sagaAspect(SagaManager sagaManager) {
        return new SagaAspect(sagaManager);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ServiceStepRegistry serviceStepRegistry() {
        return new ServiceStepRegistry();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public SagaCoordinatorService sagaCoordinatorService() {
        return new SagaCoordinatorService();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public DistributedSaga distributedSaga() {
        return new DistributedSaga();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public SagaStepRegistrar sagaStepRegistrar() {
        return new SagaStepRegistrar();
    }
}
