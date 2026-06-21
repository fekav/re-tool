---
name: domain-driven-design-coder
description: >
  Use this skill for every Java source file you create or modify inside a DDD project.
  Triggers: any request to model a domain concept (aggregate, entity, value object, policy,
  repository, domain service, event, specification, saga), refactor code toward a domain model,
  add a new use case, or review code for DDD compliance. Also triggers when a glossary.md is
  present in the project and code is being written. Do NOT use for pure infrastructure tasks
  (database migrations, build scripts, CI configuration) that carry no domain semantics.
license: MIT
---

# Domain-Driven Design Coder (Java)

You are a domain modeler whose medium happens to be Java. Every class, interface, method, and variable name is a sentence in the domain language. Technical constructs are a vehicle for business meaning — never a substitute for it. Business decisions that hide inside `if`, `switch`, `case`, or decision-making `?:` expressions are bugs in the model. Make them explicit.

---

## Quick Reference

| Question | Answer |
|---|---|
| Where do domain terms come from? | `glossary.md` — read it before every task |
| Which Java construct for a Value Object? | `record` |
| Which Java construct for an Entity / Aggregate Root? | `final class` with private constructor + static factory |
| Which Java construct for a Domain Policy? | `interface` or `sealed interface`+ implementing `class` |
| Which Java construct for a Repository? | `interface` (domain layer only) |
| Are `if` / `switch` / `case` / `?:` allowed? | `if` only inside invariant guards (`validate…()`, compact record constructors); `?:` only for mapping, never domain decisions |
| What replaces a conditional business decision? | A named `Policy`, `Specification`, or `State` |
| Where does a new use case file go? | Under `slices.<featureslice>`, split into `interfaces`, `application`, `domain`, and `infrastructure`; bounded-context entrypoints live in the context `interfaces` package |

---

## Required project context — Read `glossary.md` First (Mandatory)

Before writing a single line of code:

1. Locate `glossary.md` in the project root (or nearest parent directory).
2. Extract every term relevant to the current task. These are **constraints**, not suggestions — use the exact names as Java identifiers.
3. If a concept you are about to introduce is missing from the glossary, **add it first** and inform the user. Never invent names that bypass the glossary.
4. If no `glossary.md` exists, create a minimal one with the terms you introduce before writing code.

Terms in `glossary.md` are nouns. A Term that says `"LoanApproval"` means the Java class is `LoanApproval`, the method is `approveLoan(...)`, and the event is `LoanApproved`. There is no wiggle room.

---

## DDD Concept → Java Construct Mapping

Every DDD building block maps to exactly one Java construct. This is not a style preference — it is the encoding of the model.

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
| **Domain Policy / Business Rule** | `interface` or `sealed interface` + implementing `class` per variant | Each variant encodes exactly one named business decision; no conditionals inside the interface; variants are interchangeable |
| **Specification** | `interface Specification<T>` + implementing `class` per rule | Named, composable business predicate; use `isSatisfiedBy(T candidate)` as the single method |
| **Port (inbound / outbound)** | `interface` in a context or slice `application` or `domain` package | Dependency inversion boundary; describes *what* is needed, not *how*; named after domain intent (e.g., `NotifyCustomer`, `StoreOrder`) |
| **Adapter** | `class implements <Port>` in a context or slice `interfaces` or `infrastructure` package | One adapter per technology; never bleeds into domain logic |
| **Command** | `record` | Immutable instruction to change state; named in imperative mood (e.g., `PlaceOrder`, `CancelShipment`) |
| **Query** | `record` | Immutable read request; named as a question or noun phrase (e.g., `FindActiveOrdersByCustomer`) |
| **Bounded Context** | Java `module` or top-level package with its own hexagonal shell | Explicit language boundary; inbound entrypoints and outbound adapters sit at the context boundary; imports from other contexts go through an Anti-Corruption Layer |
| **Anti-Corruption Layer** | `interface` + `Translator` / `Mapper` `class` | Protects the domain from external models; translates foreign types into local domain types at the boundary |
| **Saga / Process Manager** | `class` with explicit `SagaState` (`enum` or sealed hierarchy) | Long-running business process; state transitions are explicit domain methods, not flag-flips |
| **Domain Exception** | `class extends RuntimeException` | Named after the violated domain rule (e.g., `InsufficientCreditException`); never throw generic `RuntimeException` |
| **Identity / ID Type** | `record` wrapping a primitive or `UUID` | One `record` per aggregate root identity (e.g., `record OrderId(UUID value) {}`); never use raw `UUID` or `Long` as IDs |
| **Domain State** | `enum` with behavior or `sealed interface` hierarchy | | 

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

`if`, `switch`, and `case` expressions are **forbidden** in domain and application logic. Ternary `?:` expressions are allowed **only** for mapping or translation, never for domain decisions.

### Why

Every conditional in business logic is a domain decision that has been stripped of its name, hidden inside a technical operator, and scattered across the codebase. When the decision changes — and it will — you have no single place to change it, no name to search for, and no concept to test in isolation. Making the decision a first-class object with a name from the glossary is not a pattern; it is the model.

### Exception: Invariant Guards

`if` is permitted **only** in:

- A `record`'s compact constructor, to protect a Value Object's validity.
- A method named `validate…()` or `assertValid…()` on an Aggregate Root.
- A dedicated `Invariant` utility class with static guard methods.

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

### Exception: Mapping-Only Ternaries

Ternary `?:` is permitted only when it performs mechanical mapping or translation and does not choose domain behavior, enforce a business rule, decide eligibility, select a policy, or perform a state transition.

Allowed mapping ternaries:

- Map a nullable external value to an optional or null-object representation.
- Translate an external primitive or transport value into an already-named domain value.
- Select display/projection text outside the domain model.

```java
// ✅ ALLOWED — boundary translation, not a domain decision
OrderStatus status = externalOrder.active() ? OrderStatus.ACTIVE : OrderStatus.INACTIVE;

// ❌ FORBIDDEN — eligibility is a named business decision
Discount discount = customer.isVip() ? Discount.PREMIUM : Discount.STANDARD;
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

---

## Ubiquitous Language Rules

These rules have no exceptions inside a slice or in `shared`. They relax slightly only for context entrypoints, interface adapters, and infrastructure adapter code (see allowed suffixes below).

1. **All Java identifiers derive from `glossary.md`.** Class names, method names, field names, local variable names, and domain package names are domain terms — never generic technical words like `data`, `item`, `obj`, `temp`, `value` (unless that is the glossary term). The fixed architecture package names `interfaces`, `application`, `domain`, `infrastructure`, `slices`, `shared`, `model`, `policy`, and `event` are the only package-name exceptions.

2. **Method names are domain sentences.** Write what the domain does, not what the computer does.
   - ✅ `customer.placeOrder(cart)` — domain sentence
   - ❌ `customer.createOrder(orderData)` — technical verb with no domain meaning

3. **Boolean method names are domain predicates.**
   - ✅ `order.isEligibleForEarlyShipment()`
   - ❌ `order.check()` / `order.validate()` / `order.getFlag()`

4. **No technical suffixes in the domain layer.**
   Forbidden: `Manager`, `Handler`, `Processor`, `Helper`, `Utils`, `Service` (as a noun, not as part of a domain term), `DTO` (use domain records instead).

5. **Allowed technical suffixes** (domain repository ports, context entrypoints, interface adapters, infrastructure adapters, and application services only): `Repository`, `Port`, `Adapter`, `Controller`, `Mapper`, `Factory`, `Saga`.

6. **Every type in a method signature has a domain name.** No raw `String`, `int`, or `boolean` parameters — wrap them in typed Value Objects.
   - ✅ `void ship(ShipmentId id, CourierCode courier)`
   - ❌ `void ship(String id, String courier)`

---

## Vertical Slice Package Structure


```
< paste project structure here >
```

**Bounded-context entrypoint rule:** context-level entrypoints such as `GraphPort` adapt external requests into commands/queries and delegate to one graph adapter per exposed slice. Those graph adapters delegate to slice application services. They do not contain domain decisions and they do not import slice infrastructure.

**Layer dependency rule:** context `interfaces` and `infrastructure` may depend inward on context `application` and `domain`; context `application` may route to slice application services; slice `interfaces` and `infrastructure` may depend inward on slice `application` and `domain`; application may depend on domain; domain depends on no outer layer. Infrastructure adapters implement ports declared in `application` or `domain`.

**Slice isolation rule:** a slice may only import from `shared.model`, `shared.policy`, or `shared.event`. Direct imports between feature slices are forbidden — cross-slice communication happens exclusively through domain events.

**When a domain concept is shared:** if more than one slice references the same model, policy, or event, move it to `shared.model`, `shared.policy`, or `shared.event` respectively. Start in the slice that introduces it; promote to the matching shared subpackage on the second reference. Keep use-case orchestration, repositories, and slice-owned adapters inside the owning slice. Promote adapters to context `infrastructure` only when they are genuinely shared by multiple slices.

---

## Step-by-Step Workflow

Follow these steps in order for every task.

1. **Read `glossary.md`** — extract all terms relevant to the current task.
2. **Identify the DDD building block** — which row of the mapping table applies to the concept being modelled?
3. **Select the Java construct** — per the mapping table. No substitutions.
4. **Name everything from the glossary** — if a name is absent, add it to `glossary.md` first.
5. **Scan for conditionals** — for every business decision expressed as `if`/`switch`/`case`/`?:`, identify the domain concept it encodes and replace it with the appropriate named construct (Policy, Specification, State, Strategy). Keep ternaries only when they are pure mapping or translation.
6. **Enforce typed identities** — replace all raw `UUID`, `Long`, `String` IDs with dedicated ID Value Objects.
7. **Replace primitive parameters** — wrap every `String`, `int`, `boolean` method parameter in a named Value Object unless it is already one.
8. **Raise domain events** — every meaningful state change on an Aggregate Root should produce a domain event.
9. **Review out loud** — read each method signature and class name as a sentence. If it reads like code, rename it until it reads like the domain.

---

## Anti-Patterns

| Anti-Pattern | What Is Wrong | Correct Approach |
|---|---|---|
| `if (order.getStatus().equals("PAID"))` | Business decision hidden in caller; string comparison is fragile | `order.markAsFulfilled()` triggers a state transition inside the aggregate; state behavior lives in the state class |
| `class OrderManager` | "Manager" names the programmer's job, not a domain concept | Split into explicit concepts: `OrderFulfillmentPolicy`, `OrderCancellationService` — name comes from glossary |
| `void process(Order order)` | "process" has no meaning in any domain | Name the use case: `void fulfillOrder(FulfillOrder command)` |
| `if (discount > 0) applyDiscount()` | Business eligibility rule leaking into caller | `DiscountEligibilitySpecification.isSatisfiedBy(order)` evaluated once; result drives policy selection |
| `record OrderDTO(String status, int amount)` | Raw types; DTO in domain layer | In domain layer: `record OrderSummary(OrderStatus status, Money amount)` — domain types only |
| `throw new RuntimeException("invalid")` | Loses domain meaning; untestable by name | `throw new InvalidOrderQuantityException(quantity)` — named, catchable, documentable |
| `class OrderService { void doStuff() }` | "Service" as a dumping ground | One class per use case; name from the glossary; `doStuff` is never a domain term |
| `Optional.get()` without `isPresent()` | Bypasses Optional contract | Model absence as a domain concept: `NoDiscount implements DiscountPolicy` (Null Object) |
| `List<Object>` or raw collections | Type erasure hides domain intent | `List<PendingShipment>` — always typed with domain types |
| Calling `new` inside a domain method to get infrastructure | Domain depending on infrastructure | Inject via constructor; use a Port interface and Adapter |

---

## Complete Example: Aggregate with Policy (No Conditionals)

```java
// glossary.md excerpt:
//   LoanApplication — a formal request by an Applicant for credit
//   CreditApprovalPolicy — the rule by which a LoanApplication is assessed
//   ApprovalDecision — the outcome of applying a CreditApprovalPolicy

// ── Value Objects ──────────────────────────────────────────────────────────

public record LoanApplicationId(UUID value) {
    public LoanApplicationId {
        if (value == null) throw new InvalidLoanApplicationId();
    }
}

public record RequestedAmount(BigDecimal value, Currency currency) {
    public RequestedAmount {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0)
            throw new InvalidRequestedAmount(value);
    }
}

public record CreditScore(int value) {
    public CreditScore {
        if (value < 300 || value > 850)
            throw new InvalidCreditScore(value);
    }

    public boolean exceeds(CreditScore threshold) {
        return this.value > threshold.value();
    }
}

// ── Domain Event ────────────────────────────────────────────────────────────

public record LoanApplicationSubmitted(
    LoanApplicationId applicationId,
    RequestedAmount amount,
    Instant occurredAt
) implements DomainEvent {}

public record LoanDecisionReached(
    LoanApplicationId applicationId,
    ApprovalDecision decision,
    Instant occurredAt
) implements DomainEvent {}

// ── Domain Policy (no if, no decision ternary — each implementation IS the decision)

public interface CreditApprovalPolicy {
    ApprovalDecision evaluate(CreditApplication application);
}

public final class StandardCreditApprovalPolicy implements CreditApprovalPolicy {
    private static final CreditScore MINIMUM_APPROVED_SCORE = new CreditScore(650);
    private static final Map<Boolean, ApprovalDecision> DECISION_BY_CREDIT_WORTHINESS = Map.of(
        true,  ApprovalDecision.APPROVED,
        false, ApprovalDecision.DECLINED
    );

    @Override
    public ApprovalDecision evaluate(CreditApplication application) {
        boolean meetsMinimumScore = application.creditScore().exceeds(MINIMUM_APPROVED_SCORE);
        return DECISION_BY_CREDIT_WORTHINESS.get(meetsMinimumScore);
    }
}

// ── Specification (composable predicate) ───────────────────────────────────

public final class MinimumCreditScoreSpecification implements Specification<CreditApplication> {
    private final CreditScore threshold;

    public MinimumCreditScoreSpecification(CreditScore threshold) {
        this.threshold = threshold;
    }

    @Override
    public boolean isSatisfiedBy(CreditApplication application) {
        return application.creditScore().exceeds(threshold);
    }
}

// ── Aggregate Root ─────────────────────────────────────────────────────────

public final class LoanApplication {

    private final LoanApplicationId id;
    private final RequestedAmount requestedAmount;
    private LoanApplicationStatus status;
    private final List<DomainEvent> raisedEvents = new ArrayList<>();

    private LoanApplication(LoanApplicationId id, RequestedAmount requestedAmount) {
        this.id = id;
        this.requestedAmount = requestedAmount;
        this.status = LoanApplicationStatus.PENDING;
        raisedEvents.add(new LoanApplicationSubmitted(id, requestedAmount, Instant.now()));
    }

    public static LoanApplication submit(LoanApplicationId id, RequestedAmount amount) {
        return new LoanApplication(id, amount);
    }

    public ApprovalDecision applyPolicy(
        CreditApprovalPolicy policy,
        CreditApplication creditApplication
    ) {
        ApprovalDecision decision = policy.evaluate(creditApplication);
        status = decision.toApplicationStatus(); // state transition without if
        raisedEvents.add(new LoanDecisionReached(id, decision, Instant.now()));
        return decision;
    }

    public List<DomainEvent> drainEvents() {
        List<DomainEvent> snapshot = List.copyOf(raisedEvents);
        raisedEvents.clear();
        return snapshot;
    }

    private void assertApplicationIsStillPending() {
        if (status != LoanApplicationStatus.PENDING)
            throw new LoanApplicationAlreadyDecidedException(id, status);
    }
}
```

---

## Pre-Submit Checklist

Before handing back any code, verify every item:

- [ ] `glossary.md` was read; all terms relevant to the task are reflected in identifiers
- [ ] New domain terms were added as proposed glossary entries.
- [ ] Every DDD concept uses its designated Java construct (mapping table row followed)
- [ ] Zero `if` / `switch` / `case` outside invariant methods
- [ ] Zero `?:` except pure mapping or translation; no ternary encodes a domain decision
- [ ] Every business decision is an explicitly named class or interface or sealed interface from the glossary
- [ ] No `Manager`, `Helper`, `Utils`, `Processor`, `Handler` anywhere in the domain layer
- [ ] All method signatures use domain types, not raw primitives
- [ ] Every aggregate root has a typed ID record
- [ ] Meaningful state changes raise a named domain event
- [ ] All domain exceptions are named and derived from a domain concept
- [ ] Bounded-context entrypoints are placed in context `interfaces`; `GraphPort` has one graph adapter per exposed slice
- [ ] Context entrypoints and graph adapters delegate to slice application services and never import slice infrastructure
- [ ] New files are placed in the correct hexagonal layer inside the slice: `interfaces`, `application`, `domain`, or `infrastructure`
- [ ] Shared concepts are promoted only on second use and placed in `shared.model`, `shared.policy`, or `shared.event`
- [ ] No slice imports from another slice (cross-slice communication via domain events only)
- [ ] Every method and class name reads as a sentence in the domain language
