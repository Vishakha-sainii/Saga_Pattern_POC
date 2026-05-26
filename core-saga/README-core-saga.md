# 🧩 Core Saga Framework

A lightweight, annotation-driven Saga orchestration framework built using **Spring AOP**.  
It enables **automatic rollback (compensation)** for Create, Update, and Delete operations when downstream services fail.

---

## 📌 Why Core Saga?

In distributed systems, a single business operation often spans multiple services:

- Inventory → Order → Billing  
- If Billing fails, Inventory & Order must roll back

Core Saga provides:
- Annotation-based saga orchestration
- Automatic rollback execution
- No Kafka / workflow engine dependency
- Reusable as a standalone JAR

---

## 🧠 High-Level Flow

```
@SagaOrchestrated method starts
        ↓
SagaAspect creates SagaContext
        ↓
Business logic executes
        ↓
Saga.compensate(...) registers rollback steps
        ↓
If any exception occurs
        ↓
SagaManager triggers rollback (LIFO)
        ↓
Compensation steps executed
        ↓
SagaContext cleared
```

---

## 📂 Project Structure

```
core-saga
 └── src/main/java/com/example/saga
     ├── annotation
     │   └── SagaOrchestrated.java
     ├── aop
     │   └── SagaAspect.java
     ├── autoconfig
     │   └── SagaAutoConfiguration.java
     ├── core
     │   ├── Saga.java
     │   ├── SagaContext.java
     │   ├── SagaManager.java
     │   └── SagaStep.java
```

---

## 1️⃣ SagaOrchestrated.java

Marks a method as a Saga entry point.

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SagaOrchestrated {
}
```

---

## 2️⃣ SagaAspect.java

Intercepts saga methods and controls lifecycle.

```java
@Around("@annotation(sagaOrchestrated)")
public Object handleSaga(...) {
    SagaContext context = new SagaContext();
    CONTEXT.set(context);

    try {
        return sagaManager.execute(pjp, context);
    } catch (Exception ex) {
        sagaManager.rollback(context);
        throw ex;
    } finally {
        CONTEXT.remove();
    }
}
```

---

## 3️⃣ SagaAutoConfiguration.java

Auto-registers Saga beans.

```java
@Configuration
@EnableAspectJAutoProxy
public class SagaAutoConfiguration {
}
```

---

## 4️⃣ Saga.java

Registers rollback steps.

```java
Saga.compensate(() -> repo.deleteById(id));
```

---

## 5️⃣ SagaContext.java

Holds saga data and rollback steps (LIFO).

---

## 6️⃣ SagaManager.java

Executes business logic and rollback.

---

## 7️⃣ SagaStep.java

Functional interface for rollback steps.

```java
@FunctionalInterface
public interface SagaStep {
    void compensate();
}
```

---

## 📦 Build & Publish

```bash
./gradlew clean build
./gradlew publishToMavenLocal
```

---

## ✅ Summary

Core Saga provides a clean, reusable Saga orchestration solution using Spring AOP.


🚀 Future Enhancements

Persistent saga store
Crash recovery
Kafka-based orchestration
Retry & idempotency
Distributed tracing

🚀 Future Enhancements

Persistent saga store
Crash recovery
Kafka-based orchestration
Retry & idempotency
Distributed tracing
