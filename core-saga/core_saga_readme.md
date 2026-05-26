# Core Saga Framework (AOP-based)

This document explains **how to build, publish, and use the `core-saga` library**, and clarifies **how `@SagaOrchestrated` and compensation actually work at runtime**.

---

## 1. What this project is

`core-saga` is a **lightweight Saga Orchestration library** implemented using:

- Spring AOP
- Custom `@SagaOrchestrated` annotation
- In-memory `SagaContext`

It is **NOT** a Spring Boot application.
It is a **pure Java / Spring library** that you import into other services (Inventory, Order, Payment, etc.).

---

## 2. Build prerequisites

- Java 17
- Gradle Wrapper (`./gradlew`)

Verify Java:

```bash
java -version
```

---

## 3. Clean build (IMPORTANT)

Always do a clean build if you faced Gradle or dependency issues earlier.

```bash
./gradlew clean
rm -rf ~/.gradle/caches
./gradlew clean
```

---

## 4. Build core-saga JAR

```bash
./gradlew build
```

Expected output:

```
BUILD SUCCESSFUL
```

Generated artifact:

```
build/libs/core-saga-0.0.1-SNAPSHOT.jar
```

---

## 5. Publish to local Maven repository

This makes the library usable by other services.

```bash
./gradlew publishToMavenLocal
```

Jar will be available at:

```
~/.m2/repository/com/example/core-saga/0.0.1-SNAPSHOT/
```

---

## 6. Use core-saga in another service (Inventory / Order)

### Add dependency

```gradle
dependencies {
    implementation 'com.example:core-saga:0.0.1-SNAPSHOT'
}
```

---

## 7. Enable Saga support in consuming service

```java
@SpringBootApplication
@EnableAspectJAutoProxy
public class InventoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryApplication.class, args);
    }
}
```

Ensure `spring-aop` dependency is present.

---

## 8. Important truth about Saga.compensate()

```java
Saga.compensate(() -> {
    repo.deleteById(itemId);
    sagaLogger.logRollback(sagaId, "ITEM", itemId);
});
```

⚠️ **This code does NOTHING automatically**

- `Saga.compensate(...)` does **NOT** execute rollback
- It only **registers a compensation step**
- Execution depends entirely on **Saga AOP flow**

---

## 9. What `@SagaOrchestrated` REALLY means

`@SagaOrchestrated` is **not a Spring magic annotation**.
It works **only because an Aspect intercepts it**.

### Typical AOP execution flow

```
@SagaOrchestrated method starts
        ↓
SagaAspect creates SagaContext
        ↓
Saga.compensate(...) registers rollback steps
        ↓
Business logic executes
        ↓
IF exception occurs
        ↓
SagaContext.rollback()
        ↓
Compensation steps execute (reverse order)
```

---

## 10. Runtime behavior example

```java
@SagaOrchestrated
public void createInventory(String name, int qty) {

    String sagaId = UUID.randomUUID().toString();

    Item item = new Item();
    item.setName(name);
    item.setQuantity(qty);

    item = repo.save(item);

    Long itemId = item.getId();

    Saga.compensate(() -> {
        repo.deleteById(itemId);
        sagaLogger.logRollback(sagaId, "ITEM", itemId);
    });

    orderClient.createOrder(itemId, sagaId); // may fail
}
```

### Runtime sequence

1. Aspect starts and creates `SagaContext`
2. Inventory saved
3. Compensation registered (NOT executed)
4. `orderClient.createOrder()` fails
5. Exception thrown
6. Aspect catches exception
7. `SagaContext.rollback()` invoked
8. Compensation executes:
   - Inventory deleted
   - Rollback logged
9. Context cleared

---

## 11. Key rules to remember

- ❌ No exception → No rollback
- ✅ Exception → Compensation runs
- Compensation runs in **reverse order**
- Local variables used in lambdas must be **final or effectively final**

---

## 12. What this saga implementation is NOT

- ❌ Not distributed transaction manager
- ❌ No automatic retry
- ❌ No persistence by default

(It is intentionally simple and synchronous.)

---

## 13. Recommended next steps

- Persist SagaContext to DB
- Add retry & recovery jobs
- Add Kafka-based choreography
- Add saga status tracking

---

## 14. Summary

- `core-saga` is a reusable AOP-based saga framework
- `Saga.compensate()` only registers rollback
- `@SagaOrchestrated` controls lifecycle via Aspect
- Rollback happens ONLY on exception

---
