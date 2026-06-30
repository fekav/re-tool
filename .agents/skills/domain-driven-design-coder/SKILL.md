---
name: domain-driven-design-coder
description: >
  Use this skill when you implement a new domain centric feature. When modifying domain logic, this skill probably has been applied, use only when explicitly invoked for refactoring/modifying existing code. Do not use for infrastructure tasks
  (database migrations, build scripts, CI configuration) that carry no domain semantics.
license: MIT
---

# Domain-Driven Design Coder (Java)

You are a domain modeler whose medium happens to be Java. Every class, interface, method, and variable name is a sentence in the domain language. Technical constructs are a vehicle for business meaning — never a substitute for it. Business decisions that hide inside `if`, `switch`, `case`, or decision-making expressions are bugs in the model. Make them explicit. 

---

## DDD Concept → Java Construct Mapping

Every DDD building block maps to exactly one Java construct. This is not a style preference — it is the encoding of the model. Apply it, when project  

| DDD Concept | Java Construct | Key Constraints |
|---|---|---|
| **Value Object** | `record` | Immutable; no identity field; equality by structure; invariants in compact constructor |
| **Entity** | `class` | Has a typed identity field (e.g., `OrderId`); package-private constructor; mutation only via domain methods |
| **Aggregate Root** | `final class` with `private` constructor + `public static` factory method | Consistency boundary; exposes domain methods that enforce invariants; raises domain events; all internal entities are package-private |
| **Domain Event** | `record` implementing `DomainEvent` | Immutable; named in past tense (e.g., `OrderPlaced`, `PaymentConfirmed`); carries only the data needed by subscribers |
| **Repository** | `interface` in the slice `domain` package | Collection-like semantics (`add`, `findBy`, `remove`); no persistence details; one interface per aggregate root; keep the repository in the owning slice |
| **Domain Service** | `final class` (stateless) | Used only when logic spans multiple aggregates and belongs in no single one; injected as a dependency, never `new`-ed inline |
| **Application Service** | `class` in the slice `application` package | Orchestrates use cases; delegates all domain logic to aggregates/domain services; handles transactions and events |
| **Factory** | `class` with `static` factory method, or dedicated `XxxFactory class` | Creates complex objects whose construction involves domain decisions; returns the aggregate/entity, not raw data |
| **Domain Policy / Business Rule** | `sealed interface` + implementing `class` per variant | Each variant encodes exactly one named business decision; no conditionals inside the interface; variants are interchangeable; separate policy behavior from policy configuration by using constructor parameter: `ConceptMatchingPolicy(double autoMapThreshold)` |
| **Specification** | `interface Specification<T>` + implementing `class` per rule | Named, composable business predicate; use `boolean isSatisfiedBy(T candidate)` as the single method |
| **Port (inbound / outbound)** | `interface` in a context or slice `application` or `domain` package | Dependency inversion boundary; describes *what* is needed, not *how* |
| **Adapter** | `class implements <Port>` in a context or slice `interfaces` or `infrastructure` package | One adapter per technology; never bleeds into domain logic; describes *how* |
| **Command** | `record` | Immutable instruction to change state; named in imperative mood (e.g., `PlaceOrder`, `CancelShipment`) |
| **Query** | `record` | Immutable read request; named as a question or noun phrase (e.g., `FindActiveOrdersByCustomer`) |
| **Bounded Context** | top-level package with its own hexagonal shell | Explicit language boundary; inbound entrypoints and outbound adapters sit at the context boundary |
| **Identity / ID Type** | `record` wrapping a primitive or `UUID` | One `record` per aggregate root identity (e.g., `record OrderId(UUID value) {}`); never use raw `UUID` or `Long` as IDs |
| **Domain State** | `enum` with behavior |  

Use interface for repositories, ports, policies, specifications, and publishers.

Use sealed interface when the domain defines a closed set of outcomes, commands, events, states, or policies.

Use enum only when each constant is a domain term and behavior is attached to the enum itself. Do not switch over enums.

---

## Dependency rules

The domain model must not depend on infrastructure.

Allowed dependency direction:

context interfaces/infrastructure -> context application -> slice application -> slice domain

slice interfaces/infrastructure -> slice application -> slice domain

The domain layer may depend on JDK types, domain types in the same bounded context, and shared kernel types from `shared.model`, `shared.policy`, and `shared.event` explicitly allowed by the context map.

The domain layer must not depend on Quarkus, Spring, Jakarta Persistence, Hibernate, Jackson, Lombok, messaging frameworks, HTTP frameworks, database APIs, logging frameworks, generated client code, or other bounded contexts directly.

---

## The No-Conditionals Rule

### The Rule

Domain decisions or domain rules or any domain related condition coded as `if`, `switch`, and `case` expressions are **forbidden** in domain and application logic. Ternary `?:` expressions are allowed **only** for mapping or translation.

### Why

Every conditional in business logic is a domain decision that has been stripped of its name, hidden inside a technical operator, and scattered across the codebase. When the decision changes — and it will — you have no single place to change it, no name to search for, and no concept to test in isolation. Making the decision a first-class object with a name is not a pattern; it is the model.

### Exception: Invariant Guards

`if` is permitted **only** in:

- A `record`'s compact constructor, to protect a Value Object's validity.
- A method named `validate…()` or `assertValid…()` on an Aggregate Root or Entity.

```java
// ✅ ALLOWED — invariant in a Value Object compact constructor
public record CreditLimit(BigDecimal value) {
    public CreditLimit {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0)
            throw new InvalidCreditLimit(value);
    }
}

// ✅ ALLOWED — invariant guard on an Aggregate Root
private void assertWithdrawalIsPossible(Money amount) {
    if (balance.isLessThan(amount))
        throw new InsufficientFundsException(accountId, amount);
}
```
### Alternatives to Conditionals

| Instead of … | Use … |
|---|---|
| `if (order.status == PAID) doX() else doY()` | Polymorphism: each `OrderStatus` state class implements `OrderStatusBehavior` |
| `switch (type) { case A: … case B: … }` | `Map<DomainKey, DomainPolicy>` lookup, or one `Policy` implementation per case |
| `if (customer.isVip()) applyDiscount()` | `DiscountPolicy` interface with `VipDiscount` and `StandardDiscount` implementations |
| Domain decision in `condition ? A : B` | `Map<Boolean, DomainResult>` lookup, or two named `Policy` implementations |
| `if (x != null) use(x)` | `Optional<X>` or Null Object (`NoDiscount`, `NoPenalty`) |
| `if (isEligible) proceed()` | `EligibilitySpecification.isSatisfiedBy(candidate)` |
| Multi-branch factory logic | `Map<DomainTrigger, Supplier<DomainObject>>` strategy map |
| `if (flag1 && flag2)` | Named `Specification` composed with `.and()` |

Prefer `Policy` or `Specification` over java `Map`. Use `Map` for very trivial conditions with less domain decision.
---

## Ubiquitous Language Rules

These rules have no exceptions inside a slice or in `shared`. They relax slightly only for context entrypoints, interface adapters, and infrastructure adapter code (see allowed suffixes below).


**Method names are domain sentences.** Write what the domain does, not what the computer does.
    - ✅ `customer.placeOrder(cart)` — domain sentence
    - ❌ `customer.createOrder(orderData)` — technical verb with no domain meaning
**Boolean method names are domain predicates.**
    - ✅ `order.isEligibleForEarlyShipment()`
    - ❌ `order.check()` / `order.validate()` / `order.getFlag()`
**No technical suffixes in the domain layer.**
    Forbidden: `Manager`, `Handler`, `Processor`, `Helper`, `Utils`,  `DTO` (use domain records instead).
**Allowed technical suffixes** (domain repository ports, context entrypoints, interface adapters, infrastructure adapters, and application services only): `Service` (only exception for domain services), `Repository`, `Port`, `Adapter`, `Controller`, `Mapper`, `Factory`.
**Every type in a method signature has a domain name.** No raw `String`, `int`, or `boolean` parameters — wrap them in typed Value Objects.
    - ✅ `void ship(ShipmentId id, CourierCode courier)`
    - ❌ `void ship(String id, String courier)`

---

## Step-by-Step Workflow

Follow these steps in order for every task.

1. **Read `glossary.md`** — extract all terms relevant to the current task.
2. **Identify the DDD building block** — which row of the mapping table applies to the concept being modelled?
3. **Select the Java construct** — per the mapping table. No substitutions.
5. **Scan for conditionals** — for every business decision expressed as `if`/`switch`/`case`/`?:`, identify the domain concept it encodes and replace it with the appropriate named construct (Policy, Specification, Invariant).
6. **Enforce typed identities** — replace all raw `UUID`, `Long`, `String` IDs with dedicated ID Value Objects.
7. **Replace primitive parameters** — wrap every `String`, `int`, `boolean` method parameter in a named Value Object unless it is already one.
8. **Raise domain events** — every meaningful state change on an Aggregate Root should produce a domain event.
9. **Review out loud** — read each method signature and class name as a sentence. If it reads like code, rename it until it reads like the domain.

---

## Anti-Patterns

| Anti-Pattern | What Is Wrong | Correct Approach |
|---|---|---|
| `if (order.getStatus().equals("PAID"))` | Business decision hidden in caller; string comparison is fragile | `order.markAsFulfilled()` triggers a state transition inside the aggregate; state behavior lives in the state class |
| `class OrderManager` | "Manager" names the programmer's job, not a domain concept | Split into explicit concepts: `OrderFulfillmentPolicy`, `OrderCancellationService` |
| `void process(Order order)` | "process" has no meaning in any domain | Name the use case: `void fulfillOrder(FulfillOrder command)` |
| `if (discount > 0) applyDiscount()` | Business eligibility rule leaking into caller | `DiscountEligibilitySpecification.isSatisfiedBy(order)` evaluated once; result drives policy selection |
| `record OrderDTO(String status, int amount)` | Raw types; DTO in domain layer | In domain layer: `record OrderSummary(OrderStatus status, Money amount)` — domain types only |
| `throw new RuntimeException("invalid")` | Loses domain meaning; untestable by name | `throw new InvalidOrderQuantityException(quantity)` — named, catchable, documentable |
| `Optional.get()` without `isPresent()` | Bypasses Optional contract | Model absence as a domain concept: `NoDiscount implements DiscountPolicy` (Null Object) |
| `List<Object>` or raw collections | Type erasure hides domain intent | `List<PendingShipment>` — always typed with domain types |
| Calling `new` inside a domain method to get infrastructure | Domain depending on infrastructure | Inject via constructor; use a Port interface |

---


## Pre-Submit Checklist

Before handing back any code, verify every item:

- [ ] Every DDD concept uses its designated Java construct (mapping table row followed)
- [ ] Zero `if` / `switch` / `case` outside invariant methods
- [ ] Zero `?:` except pure mapping or translation; no ternary encodes a domain decision
- [ ] Every business decision is an explicitly named class or interface or sealed interface
- [ ] No `Manager`, `Helper`, `Utils`, `Processor`, `Handler` anywhere in the domain layer
- [ ] All method signatures use domain types, not raw primitives
- [ ] Every aggregate root has a typed ID record
- [ ] Meaningful state changes raise a named domain event
- [ ] All domain exceptions are named and derived from a domain concept
- [ ] New files are placed in the correct hexagonal layer inside the slice: `interfaces`, `application`, `domain`, or `infrastructure`
- [ ] Shared concepts are promoted only on second use and placed in `shared`
- [ ] No slice imports from another slice (cross-slice communication via domain events only)
- [ ] Every method and class name reads as a sentence in the domain language
