# AEM Core Concepts — Developer Reference

> A structured technical reference for all core AEM/OSGi/Sling patterns implemented in this project.

---

## Table of Contents

1. [Sling Models](#sling-models)
  - [Best Practice: Interface + Implementation](#best-practice-interface--implementation)
  - [Adaptables vs Adapters](#adaptables-vs-adapters)
  - [Injection Strategies](#injection-strategies)
  - [@PostConstruct](#postconstruct)

2. [OSGi Service Registry](#osgi-service-registry)
  - [How Multiple Implementations of a Service Are Resolved](#how-multiple-implementations-of-a-service-are-resolved)
  - [Option A — Service Ranking](#option-a--service-ranking)
  - [Option B — Named Services with Targeted Injection](#option-b--named-services-with-targeted-injection)

3. [OSGi Configuration Registry](#osgi-configuration-registry)
  - [Pattern Overview](#pattern-overview)
  - [Container-Managed Lifecycle (v1 — Bind/Unbind Pattern)](#container-managed-lifecycle-v1--bindunbind-pattern)
  - [Application-Managed Lifecycle (v2 — Self-Registration Pattern)](#application-managed-lifecycle-v2--self-registration-pattern)
  - [TL;DR Comparison](#tldr-comparison)

4. [Sling Servlets](#sling-servlets)
  - [serialVersionUID](#serialversionuid)
  - [Servlet Registration: Two Approaches](#servlet-registration-two-approaches)
  - [SlingSafeMethodsServlet vs SlingAllMethodsServlet](#slingsafemethodsservlet-vs-slingallmethodsservlet)

5. [Sling Jobs](#sling-jobs)
  - [Why Sling Jobs Instead of Plain Schedulers?](#why-sling-jobs-instead-of-plain-schedulers)
  - [End-to-End Flow of a Scheduled Sling Job](#end-to-end-flow-of-a-scheduled-sling-job)
  - [Complete Flow Diagram](#complete-flow-diagram)
  - [Multi-Timezone Job Pattern](#multi-timezone-job-pattern)

6. [Sling Event Handlers & Resource Listeners](#sling-event-handlers--resource-listeners)
  - [When to Use Which](#when-to-use-which)
  - [Common Event Topics](#common-event-topics)
  - [The Golden Rule — Keep the Handler Fast](#the-golden-rule--keep-the-handler-fast)
  - [ResourceChangeListener — Path Registration with Glob Patterns](#resourcechangelistener--path-registration-with-glob-patterns)

7. [Cluster-Aware Listeners](#cluster-aware-listeners)
  - [The Problem](#the-problem)
  - [The Two Interfaces](#the-two-interfaces)
  - [The Key Method: isExternal()](#the-key-method-isexternal)
  - [When to Use Each Approach](#when-to-use-each-approach)
  - [The Duplicate Job Problem](#the-duplicate-job-problem)
  - [Author Cluster vs Publish Farm](#author-cluster-vs-publish-farm)
  - [Gotchas](#gotchas)
  - [Evolution of Listener APIs — What to Use](#evolution-of-listener-apis--what-to-use)
  - [OSGi Config Properties That Matter](#osgi-config-properties-that-matter)
  - [Key Differences From Deprecated Approaches](#key-differences-from-deprecated-approaches)

8. [Sling Filters](#sling-filters)
  - [What Is a Sling Filter?](#what-is-a-sling-filter)
  - [Old Way vs New Way](#old-way-vs-new-way)
  - [The Five Filter Scopes](#the-five-filter-scopes)
  - [Filter Narrowing Properties](#filter-narrowing-properties)
  - [Service Ranking — Filter Execution Order](#service-ranking--filter-execution-order)
  - [The Filter Chain](#the-filter-chain)
  - [Response Wrapping — Modifying the Response Body](#response-wrapping--modifying-the-response-body)
  - [Disabling a Filter at Runtime](#disabling-a-filter-at-runtime)
  - [Common Mistakes](#common-mistakes)
  - [Filters vs Event Handlers vs Schedulers](#filters-vs-event-handlers-vs-schedulers)

9. [Request Flow — Browser → CDN → Dispatcher → AEM](#request-flow--browser--cdn--dispatcher--aem)
  - [Full Request Lifecycle](#full-request-lifecycle)
  - [Practical Implications](#practical-implications)
  - [Security Headers and Caching — A Critical Note](#security-headers-and-caching--a-critical-note)
  - [Dispatcher Cache Invalidation and Filters](#dispatcher-cache-invalidation-and-filters)

10. [AEM Workflows](#aem-workflows)
- [Engine Architecture](#engine-architecture)
- [Granite Workflow vs. Sling Jobs](#granite-workflow-vs-sling-jobs)
- [State Management — The Three Metadata Maps](#state-management--the-three-metadata-maps)
- [WorkItem: The Runtime Container](#workitem-the-runtime-container)
- [MetaDataMap (args): The Design-Time Configuration](#metadatamap-args-the-design-time-configuration)
- [WorkItem vs MetaDataMap (args) — Detailed Comparison](#workitem-vs-metadatamap-args--detailed-comparison)
- [Execution Patterns](#execution-patterns)
- [Transient Workflows](#transient-workflows)
- [Participant Steps and the AEM Inbox](#participant-steps-and-the-aem-inbox)
- [Advanced Routing](#advanced-routing)
- [Automation & Launchers](#automation--launchers)
- [6.5 vs AEM as a Cloud Service — The Asset Microservices Shift](#65-vs-aem-as-a-cloud-service--the-asset-microservices-shift)
- [Performance: Throttling and Purging](#performance-throttling-and-purging)
- [Best Practices](#best-practices)

11. [Product Catalog Scenario — Connecting dots](#product-catalog-scenario--connecting-dots-child-resource-custom-serializer-replication-notification)
- [@ChildResource — Multifield to Sling Model List](#childresource--multifield-to-sling-model-list)
- [Custom Jackson Serializer](#custom-jackson-serializer)
- [Programmatic Replication Trigger](#programmatic-replication-trigger)
- [Workflow Replication Step + Notification Step](#workflow-replication-step--notification-step)
- [Common Interview Questions — These Patterns](#common-interview-questions--these-patterns)

12. [Custom Widget, Content Fragment & Adobe Launch — Applied Scenario](#custom-widget-content-fragment--adobe-launch--applied-scenario)
- [Granite UI Custom Widget](#granite-ui-custom-widget)
- [Content Fragment API](#content-fragment-api)
- [Adobe Launch / Client Context](#adobe-launch--client-context)

13. [CSRF Token Handling](#csrf-token-handling)
- [What CSRF is](#what-csrf-is)
- [How the token fixes it](#how-the-token-fixes-it)
- [Correct pattern — use the OOTB endpoint, no custom GET needed](#correct-pattern--use-the-ootb-endpoint-no-custom-get-needed)
- [Three-pillar security model](#three-pillar-security-model)

14. [XSS Protection — XSSAPI](#xss-protection--xssapi)
- [The core rule: the right escaper for the right context](#the-core-rule-the-right-escaper-for-the-right-context)
- [Why HTML escaping is WRONG inside a `<script>` block](#why-html-escaping-is-wrong-inside-a-script-block)
- [Injection points that developers commonly miss](#injection-points-that-developers-commonly-miss)
- [JCR values vs request parameters](#jcr-values-vs-request-parameters)

15. [Thread Dumps — Reading & Analysis](#thread-dumps--reading--analysis)
- [What it is](#what-it-is)
- [How to take one](#how-to-take-one)
- [Thread states](#thread-states)
- [Four patterns to look for](#four-patterns-to-look-for)

16. [Heap Dumps — Analysis with Eclipse MAT](#heap-dumps--analysis-with-eclipse-mat)
- [What it is](#what-it-is-1)
- [Eclipse MAT — four views](#eclipse-mat--four-views)
- [Three most common AEM heap problems](#three-most-common-aem-heap-problems)

17. [CIF GraphQL Caching](#cif-graphql-caching)
- [Architecture](#architecture)
- [Cache key](#cache-key)
- [OSGi config (factory — one per commerce endpoint)](#osgi-config-factory--one-per-commerce-endpoint)
- [How to see cache stats](#how-to-see-cache-stats)
- [How to clear the cache](#how-to-clear-the-cache)

18. [AEM Architecture Decision Matrix](#aem-architecture-decision-matrix)
---

# Sling Models

## What You'll Learn

- Why Sling Models exist and when to use them.
- The difference between Adaptables and Adapters.
- Injection annotations and lifecycle.
- The role of `@PostConstruct`.
- Best practices for writing maintainable Sling Models.
- Common pitfalls seen in production projects.

## Best Practice: Interface + Implementation

Always define a public interface and keep the implementation class package-private. Components and tests depend on the interface, not the implementation.

```java
@Model(
    adaptables = { Resource.class, SlingHttpServletRequest.class },
    adapters    = Author.class,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL,
    resourceType = AuthorImpl.RESOURCE_TYPE
)
@Exporter(
    name       = "jackson",
    selector   = "model",
    extensions = "json",
    options    = {
        @ExporterOption(name = "SerializationFeature.WRAP_ROOT_VALUE",        value = "true"),
        @ExporterOption(name = "MapperFeature.SORT_PROPERTIES_ALPHABETICALLY", value = "true")
    }
)
@JsonRootName("AuthorDetails")
public class AuthorImpl implements Author {}
```

## Adaptables vs Adapters

### Adaptables
What types of objects this model can adapt from.
Specifies the source object types that can be adapted into this sling model.
SlingHttpServletRequest.class → when adapting from sling request.
Resource.class → when adapting from JCR resource.

### Adapters
What types of objects this model can adapt to.
Specifies the target that this model can adapt to.
Usually the model itself is the adapter, but you can expose it as an interface.

| Term | Meaning | Example |
|---|---|---|
| **Adaptables** | Where the model comes from — the **input** types it can be created from | `Resource.class`, `SlingHttpServletRequest.class` |
| **Adapters** | What the model can be seen as — the **output** type it exposes | `Author.class` (the interface) |

- Use `SlingHttpServletRequest.class` when you need access to request attributes, headers, or session data.
- Use `Resource.class` when adapting from a JCR node directly (e.g. in a background context with no active request).

## Injection Strategies

`defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL` means a missing property will not cause the model to fail — the field simply remains `null`. Use `REQUIRED` only when the property is truly mandatory for the model to function.

## @PostConstruct

```java
@PostConstruct
protected void init() {
    // Runs once, after ALL field injections have completed.
    // Safe to use injected fields here.
}
```

Use `@PostConstruct` for derived fields, null-checks, and any initialization logic that depends on injected values.

## Best Practices

### Keep models focused on presentation

A Sling Model should prepare data for rendering. Business logic, repository updates, and integrations with external systems should be delegated to OSGi services.

### Prefer interfaces over implementation classes

Expose models through interfaces wherever possible. This improves encapsulation, makes testing easier, and allows implementations to evolve without affecting consumers.

### Select the appropriate adaptable

Use `Resource` whenever request-specific objects are unnecessary. Use `SlingHttpServletRequest` only when request attributes, selectors, suffixes, headers, or cookies are required.

Choosing the least restrictive adaptable improves reusability.

### Keep initialization inside `@PostConstruct`

Use `@PostConstruct` to prepare derived values, validate injected fields, adapt child resources, or initialize collections.

Avoid long-running tasks, external API calls, repository writes, or replication operations during model initialization.

### Delegate business logic

Sling Models should coordinate data. Business rules should reside inside OSGi services.

## Common Mistakes

- Injecting implementation classes instead of interfaces.
- Using `SlingHttpServletRequest` as the adaptable when `Resource` is sufficient.
- Marking every injection as REQUIRED.
- Putting expensive calculations inside getter methods.
- Opening `ResourceResolver`s inside Sling Models.
- Calling external REST APIs during model adaptation.

## Real Project Scenario

### Product Details Component

A commerce application displays product information, pricing, inventory, and available variants.
The Sling Model adapts the current product resource, delegates pricing and inventory retrieval to an OSGi service, and exposes presentation-ready data to HTL.

```
Current Resource
        │
        ▼
  Sling Model
        │
        ▼
 Commerce Service
        │
        ▼
   GraphQL API
        │
        ▼
 Presentation DTO
        │
        ▼
      HTL
```

## Key Takeaways

- Sling Models simplify mapping repository content to Java objects.
- Prefer adapting from `Resource` unless request-specific information is required.
- Keep models lightweight.
- Use `@PostConstruct` only for initialization.
- Delegate business logic to services.
- [ ] Interface-based design
- [ ] Correct adaptable selected
- [ ] OPTIONAL injection where appropriate
- [ ] Lightweight model
- [ ] No repository writes
- [ ] No external HTTP calls
- [ ] Unit tests available

## Architecture Decision Guide

| Requirement | Recommendation | Why |
|-------------|---------------|-----|
| Display component data | Sling Model | Presentation layer |
| Business logic | OSGi Service | Reusable and testable |
| Repository query | Service | Separation of concerns |
| Request parameter processing | Request adaptable Model | Request object available |
| Child resources | @ChildResource | Automatic adaptation |
| JSON response | Exporter / DTO | Clean serialization |

---

# OSGi Service Registry

## How Multiple Implementations of a Service Are Resolved

When multiple classes implement the same OSGi service interface, the framework must pick one. By default, the implementation with the **lowest ServiceID** (the one registered first) is used. This is non-deterministic across restarts.

There are two reliable ways to control which implementation is used.

### Option A — Service Ranking

The implementation with the **highest ranking integer** wins.

```java
@Component(service = MyService.class)
@ServiceRanking(1001)
public class MyServiceImplA implements MyService { }

@Component(service = MyService.class)
@ServiceRanking(1002)
public class MyServiceImplB implements MyService { }
```

`ImplB` (ranking 1002) will always be injected wherever `MyService` is referenced, because it has the higher ranking.

**Injecting in a Sling Model:**
```java
@OsgiService
private MyService service;
```

**Injecting in another OSGi component:**
```java
@Reference
private MyService service;
```

### Option B — Named Services with Targeted Injection

Give each implementation a unique name and use an LDAP filter to select a specific one at the injection point.

```java
@Component(service = MyService.class, name = "impla")
public class MyServiceImplA implements MyService { }

@Component(service = MyService.class, name = "implb")
public class MyServiceImplB implements MyService { }
```

**Inject `ImplA` by name in a Sling Model:**
```java
@OsgiService(filter = "(component.name=impla)")
private MyService service;
```

**Inject `ImplB` by name in an OSGi component:**
```java
@Reference(target = "(component.name=implb)")
private MyService service;
```

> **When to use which:**  
> Use **Service Ranking** when one implementation should always win globally.  
> Use **Named Services** when different consumers legitimately need different implementations.

## Best Practices

- Keep services stateless.
- Prefer constructor/DS injection over service lookups.
- Use `service.ranking` only when implementations are interchangeable.
- Avoid storing request-specific data in services.

## Common Mistakes

* Using static fields.
* Depending on service activation order.
* Ignoring dynamic binding.
* Hardcoding implementation classes.

## Real Project Scenario

Multiple payment gateway implementations (Stripe, Adyen, Razorpay) selected using service ranking or targeted references.
1. [x] Stateless service.
2. [x] Proper lifecycle methods.
3. [x] Dynamic references understood.
4. [x] Unit tests written.

## Key Takeaways
Services should encapsulate business logic and remain reusable.

## Architecture Decision Guide

| Situation | Use |
|-----------|-----|
| Single implementation | Normal @Reference |
| Multiple implementations | Service Ranking |
| Select specific implementation | Target Filter |
| Runtime replacement | Dynamic Reference |
| Shared business logic | OSGi Service |
| Per-request data | Sling Model / Servlet |

---

# OSGi Configuration Registry

This section covers how to manage multiple factory instances of a service (e.g. one reCAPTCHA config per site) and the two lifecycle patterns for maintaining a runtime registry of those instances.

## Pattern Overview

**Reference files:**
- `v1`: `services/impl/v1/GoogleRecaptchaConfigServiceImpl.java` — Container-managed
- `v2`: `services/impl/v2/GoogleRecaptchaConfigServiceImpl.java` — Application-managed

## Container-Managed Lifecycle (v1 — Bind/Unbind Pattern)

The OSGi container watches the service registry and calls your `bind()` / `unbind()` methods whenever a matching service appears or disappears. **You only react — the container decides when.**

```java
@Reference(
    service     = GoogleRecaptchaConfigService.class,
    cardinality = ReferenceCardinality.MULTIPLE,
    policy      = ReferencePolicy.DYNAMIC
)
protected void bind(GoogleRecaptchaConfigService service) {
    REGISTRY.put(service.getSiteName(), service);
}

protected void unbind(GoogleRecaptchaConfigService service) {
    REGISTRY.remove(service.getSiteName());
}
```

**The container handles:**
- Watching the service registry for new/removed instances
- Deciding when `bind()` / `unbind()` are called
- Enforcing ordering, ranking, and thread safety
- Dynamic hot-swap when a config changes at runtime

**Here you tell OSGi:**
@Reference(cardinality = MULTIPLE, policy = DYNAMIC)
Now the container (OSGi runtime):
watches the service registry
decides when a service appears/disappears
calls your bind() / unbind() methods
enforces ordering, ranking, thread safety
You are just reacting to container events.
That’s why it’s container-managed.

## Application-Managed Lifecycle (v2 — Self-Registration Pattern)

Each factory instance registers itself into a static map on activation and removes itself on deactivation. **Your application code owns the lifecycle.** Static registry is application-managed because you own the lifecycle.

```java
private static final Map<String, GoogleRecaptchaConfigService> REGISTRY =
    new ConcurrentHashMap<>();

@Activate
@Modified
public void activate(GoogleRecaptchaConfig config) {
    siteName = config.siteName();
    // ...
    REGISTRY.put(siteName, this);   // application registers itself
}

@Deactivate
public void deactivate() {
    REGISTRY.remove(siteName);      // application removes itself
}
```

**Your code owns:**
- When to register and deregister
- Where instances are stored
- Thread safety of the map (use `ConcurrentHashMap`)
- Handling config updates (`@Modified` must re-register)

**Your application code decides:**
when to register
where to store
how to update
how to remove
@Activate  → REGISTRY.put(this)
@Deactivate → REGISTRY.remove(this)
**The container only:**
creates the object
calls @Activate / @Deactivate
Everything else is your responsibility. That’s why it’s application-managed.

## Comparison

| Concept | Static Registry (v2 — App-managed) | Bind/Unbind (v1 — Container-managed) |
|---|---|---|
| Who tracks instances? | Your code | OSGi runtime |
| Who decides when to add/remove? | You (`@Activate` / `@Deactivate`) | Container (`bind()` / `unbind()`) |
| Failure handling | You write it | Container handles it |
| Thread safety | You manage | OSGi ensures |
| Dynamic hot-swap | Harder — you implement it | Built-in |
| Code complexity | Simpler | Slightly more boilerplate |

> Use **v1 (container-managed)** when hot-swap correctness and thread safety guarantees matter (multi-bundle, production-critical).  
> Use **v2 (application-managed)** for simpler cases where all factory instances live in the same bundle.

## Best Practices
* Prefer typed configuration interfaces.
* Validate configuration values during activation.
* Use factory configurations for multiple instances.

## Common Mistakes
* Reading configuration repeatedly from the repository.
* Assuming configuration never changes.
* Ignoring modified() callbacks.
* 
## Real Project Scenario
Multiple external APIs with different credentials using factory configurations.

## Key Takeaways
Keep configuration external to code and design for runtime updates.
1. [x] Uses @Designate.
2. [x] Supports runtime updates.
3. [x] Validates mandatory properties.

## Architecture Decision Guide

| Requirement | Use |
|------------|-----|
| Single configuration | @Designate |
| Multiple instances | Factory Configuration |
| Runtime update | modified() |
| Startup configuration | activate() |
| Environment-specific values | OSGi Config |
| Business constants | Java Constant |

---

# Sling Servlets

## serialVersionUID

```java
private static final long serialVersionUID = 1L;
```

`HttpServlet` implements `Serializable`, so every servlet inherits that requirement. The `serialVersionUID` is a version-control ID used during Java deserialization to verify that the class definition on the receiving end matches the one that serialized the object.

If the IDs differ at deserialization time, Java throws `java.io.InvalidClassException`. Always declare it explicitly to avoid compiler warnings and unexpected runtime errors.

---

## Servlet Registration: Two Approaches

### ResourceType Registration (Best Practice)

```java
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "sibi-aem-one/services/getDetails",
    methods       = { HttpConstants.METHOD_GET, HttpConstants.METHOD_POST },
    selectors     = "data",
    extensions    = "json"
)
public class ResourceTypeRegistrationServlet extends SlingAllMethodsServlet { }
```

- The servlet is bound to a resource type, not a path.
- A JCR node with that resource type must exist for the servlet to be reachable.
- **ACLs of that JCR node apply to the servlet** — giving you repository-level access control for free.
- This is the currently recommended approach.

### Path Registration (Legacy)

```java
@Component(service = Servlet.class)
@SlingServletPaths("/bin/sibi-aem-one/services/getDetails")
public class PathRegistrationServlet extends SlingAllMethodsServlet { }
```

- The servlet is bound to a fixed URL path.
- By default all path-registered servlets require authentication.
- To open one to anonymous access, add the path to the `SlingAuthenticator` OSGi config:

  ```
  ui.config/.../org.apache.sling.engine.impl.auth.SlingAuthenticator.cfg.json
  ```

- Avoid in new development — path registration bypasses JCR ACLs and introduces security risks.

---

## SlingSafeMethodsServlet vs SlingAllMethodsServlet

| Class | Use when |
|---|---|
| `SlingSafeMethodsServlet` | Read-only (GET, HEAD) — idempotent operations |
| `SlingAllMethodsServlet` | Write operations (POST, PUT, DELETE) |

## Best Practices
* Prefer resource type servlets over path-based servlets.
* Validate all request parameters.
* Return meaningful HTTP status codes.
* Keep servlets thin; delegate work to services.

## Common Mistakes
* Business logic inside servlets.
* Returning HTML from JSON APIs.
* Missing input validation.
* Using path servlets unnecessarily.

## Real Project Scenario
AJAX endpoint returning product recommendations by delegating to a recommendation service and returning JSON.

## Key Takeaways
Servlets coordinate requests; services perform business logic.
1. [x] Resource-type servlet used where appropriate.
2. [x] Input validated.
3. [x] Response codes handled correctly.
4. [x] Business logic delegated.
5. [x] Unit tests available.

---

# Sling Jobs

## Why Sling Jobs Instead of Plain Schedulers?

| Feature | Plain Sling Scheduler | Scheduled Sling Job |
|---|---|---|
| Survives server restart? | No | **Yes** — persisted in JCR |
| Cluster-aware execution? | No — runs on every node | **Yes** — runs on exactly one node |
| Retry on failure? | No | **Yes** — automatic with backoff |
| Persistent audit trail? | No | Yes — `/var/eventing/jobs/` |

---

## End-to-End Flow of a Scheduled Sling Job

### Step 1 — OSGi Config is Loaded

AEM reads your `.cfg.json` on startup or bundle deploy:

```json
{
  "enabled": true,
  "timezone1.cron": "0 0 5 * * ?",
  "timezone1.id": "America/New_York"
}
```

Each JSON key maps to an `@interface Config` method using the **underscore-to-dot rule**: `timezone1_cron()` → `"timezone1.cron"`.

### Step 2 — Config is Injected into the Component

`@Designate(ocd = Config.class)` links the component to the config schema. OSGi calls `@Activate` and passes in the populated config object.

```
.cfg.json  →  @interface Config  →  @Activate(Config config)
```

If the config is later changed in the Felix console, `@Modified` fires — old jobs are unscheduled and new ones registered with the updated values.

### Step 3 — JobManager Registers the Scheduled Job

```java
jobManager
    .createJob(TOPIC)
    .properties(props)       // e.g. timezoneId, schedulerName
    .schedule()
    .cron("0 0 5 * * ?")
    .add();
```

> **Important:** Before calling `.add()`, always call `getScheduledJobs()` to check for duplicates. Without this guard, the same job gets re-registered on every bundle restart.

### Step 4 — Job is Persisted in JCR

Unlike a plain `Sling Scheduler`, a scheduled Sling Job is written to the JCR at:

```
/var/eventing/scheduled-jobs/
```

This means it **survives a server restart**. The schedule is not lost when AEM goes down.

### Step 5 — Cron Fires, Job Instance is Created

When the cron expression fires, Sling Eventing creates a job instance at:

```
/var/eventing/jobs/
```

The properties attached when scheduling (e.g. `timezoneId`, `schedulerName`) are carried into the job instance and available to the consumer.

### Step 6 — JobConsumer Processes the Job

```java
@Component(
    service  = JobConsumer.class,
    property = { JobConsumer.PROPERTY_TOPICS + "=com/example/myapp/topic" }
)
public class MyConsumer implements JobConsumer {
    public JobResult process(Job job) {
        // heavy work here
        return JobResult.OK;
    }
}
```

In a clustered AEM/AMS environment, Sling ensures the job runs on **exactly one node** — no manual cluster coordination needed.

### Step 7 — JobResult Determines What Happens Next

| Return | Meaning | Job removed? | Retried? |
|---|---|---|---|
| `OK` | Success | Yes | No |
| `FAILED` | Error — try again | No | Yes (with automatic backoff) |
| `CANCEL` | Intentional abort | Yes | No |

Retry count and backoff delay are configurable at `/system/console/configMgr` in the OSGi job queue config.

---

## Complete Flow Diagram

```
.cfg.json
   ↓ OSGi reads and maps keys
@interface Config
   ↓ @Designate + @Activate
JobRegistrar (Producer)
   ↓ @Reference + createJob().schedule().cron().add()
JobManager
   ↓ persists to JCR
/var/eventing/scheduled-jobs   ← survives server restart
   ↓ cron fires
/var/eventing/jobs             ← job instance created with properties
   ↓ topic matched
JobConsumer.process(job)
   ↓ returns
OK → done | FAILED → retry | CANCEL → abort
```

---

## Multi-Timezone Job Pattern

To run a job at midnight in three different timezones, register three separate scheduled jobs from a single producer — one per timezone. Each job carries its timezone ID as a property, and the consumer uses `ZoneId.of(timezoneId)` to log and process in the correct local time.

**Deduplication key:** Store a `schedulerName` as a job property and use it to detect duplicates in `getScheduledJobs()` before calling `.add()`.

```java
// Duplicate guard
Collection<ScheduledJobInfo> existing = jobManager.getScheduledJobs(TOPIC, -1, null);
for (ScheduledJobInfo info : existing) {
    if (schedulerName.equals(info.getJobProperties().get("schedulerName"))) {
        return; // already registered
    }
}
```

## Best Practices

### Keep jobs idempotent
A failed Sling Job may be retried automatically. Ensure that processing the same payload multiple times produces the same result without corrupting data.

### Keep payloads lightweight
Pass identifiers such as repository paths or IDs instead of serializing large objects into the job properties.

### Avoid long-running synchronous work
Jobs should delegate independent tasks to separate services rather than becoming monolithic processing units.

### Monitor retry counts
Unexpected retries often indicate repository exceptions, permission issues, or unavailable external services.

## Common Mistakes
- Storing large objects inside job properties.
- Assuming jobs execute immediately.
- Writing non-idempotent processors.
- Forgetting retry handling.
- Running business logic inside the scheduler instead of a JobConsumer.

## Real Project Scenario

### Nightly Product Synchronization
Every night an external ERP exports updated product data.
Instead of importing everything synchronously:
```
Scheduler
↓
Creates Sling Job
↓
Job Consumer
↓
Commerce Import Service
↓
Repository Updates
↓
Replication
↓
Dispatcher Flush
```
If one import fails, only that job retries instead of restarting the entire synchronization.

## Production Readiness Checklist
1. [x] Job is idempotent
2. [x] Payload contains only identifiers
3. [x] Retry logic verified
4. [x] Exceptions logged properly
5. [x] Monitoring configured
6. [x] Processing delegated to services

## Architecture Decision Guide

| Requirement | Use |
|------------|-----|
| Background work | Sling Job |
| Periodic trigger | Scheduler |
| Human approval | Workflow |
| Repository event | Listener |
| Retry required | Sling Job |
| Fire-and-forget | Event |

---

# Sling Event Handlers & Resource Listeners

## When to Use Which

| | OSGi `EventHandler` | `ResourceChangeListener` |
|---|---|---|
| Use for | System events (replication, DAM, workflow, bundle) | JCR content / resource changes |
| Package | `org.osgi.service.event` | `org.apache.sling.api.resource.observation` |
| Status | Current | Current (replaces deprecated EventHandler + SlingConstants resource topics) |
| `immediate = true` required? | Always | Always |
| Heavy work inside? | Never — delegate to Sling Job | Never — delegate to Sling Job |

## Common Event Topics

**OSGi EventHandler topics:**

| Event | Topic Constant |
|---|---|
| Page activated / deactivated | `ReplicationAction.EVENT_TOPIC` |
| Page created / modified / deleted | `PageEvent.EVENT_TOPIC` |
| DAM asset events | `DamEvent.EVENT_TOPIC` |
| Workflow completed | `WorkflowEvent.TOPIC` |
| Bundle started / stopped | `BundleEvent.TOPIC` |

**ResourceChangeListener change types:**

| Constant | Meaning |
|---|---|
| `ADDED` | Node/resource created |
| `CHANGED` | Node/resource modified |
| `REMOVED` | Node/resource deleted |

## The Golden Rule — Keep the Handler Fast

The event thread processes events serially. Any slow work inside `handleEvent()` or `onChange()` will back up the entire event queue for that AEM instance.

**Never do heavy work inside an event handler. Always delegate to a Sling Job:**

```
EventHandler.handleEvent()
└── jobManager.addJob(topic, props)   ← fire and return immediately
        │
        ▼
JobConsumer.process(job)              ← heavy work here, with automatic retry
```

## ResourceChangeListener — Path Registration with Glob Patterns

```java
@Component(service = ResourceChangeListener.class, immediate = true, property = {
    ResourceChangeListener.PATHS   + "=/content/sibi-aem-one",
    ResourceChangeListener.PATHS   + "=glob:/content/sibi-aem-one/**/*.html",
    ResourceChangeListener.PATHS   + "=glob:/content/sibi-aem-one/**/products/**",
    ResourceChangeListener.PATHS   + "=glob:/content/sibi-aem-one/**/jcr:content",
    ResourceChangeListener.CHANGES + "=ADDED",
    ResourceChangeListener.CHANGES + "=CHANGED",
    ResourceChangeListener.CHANGES + "=REMOVED"
})
```

- Use the `glob:` prefix for wildcard path matching.
- Subscribe only to the change types you actually need — over-subscribing fires unnecessary events.

## Best Practices

### Keep handlers fast
Event handlers should react to events, not perform lengthy processing.
Delegate expensive work to Sling Jobs.

### Filter aggressively
Listen only for the event topics and repository paths you actually need.
Broad listeners increase repository load.

### Avoid recursive updates
If your listener modifies the same resource it is observing, ensure safeguards exist to prevent endless event loops.

## Common Mistakes
- Listening under /content unnecessarily.
- Executing expensive logic directly.
- Ignoring external cluster events.
- Triggering recursive repository updates.

## Real Project Scenario

When a Content Fragment changes:
```
Repository Event
↓
ResourceChangeListener
↓
Validation Service
↓
Generate Search Metadata
↓
Schedule Re-index Job
↓
Search Platform
```
Instead of blocking repository commits, the expensive indexing work is deferred.

## Production Readiness Checklist
1. [x] Listener scope minimized
2. [x] Path filters configured
3. [x] Expensive work delegated
4. [x] Recursive updates prevented
5. [x] Proper logging added

---

# Cluster-Aware Listeners

## The Problem

In AEM AMS, the author tier runs as a cluster (typically 2 nodes) that share a single JCR repository. When node 1 makes a change, a plain `ResourceChangeListener` on node 2 will **silently miss it**. Cluster-aware listeners solve this.

## The Two Interfaces

```java
// Listens to changes on THIS node only
public class MyListener implements ResourceChangeListener { }

// Listens to changes from THIS node AND all other cluster nodes
public class MyListener implements ResourceChangeListener, ExternalResourceChangeListener { }
```

Both are in `org.apache.sling.api.resource.observation`.

## The Key Method: isExternal()

Inside `onChange()`, use `change.isExternal()` to know where the change came from:

| Returns | Meaning |
|---|---|
| `false` | Change happened on **this** node (local) |
| `true` | Change happened on **another** cluster node (external) |

This is the only way to distinguish local from external events.

## When to Use Each Approach

| Use case | Approach |
|---|---|
| Cache invalidation, search index updates — every node must react | Implement `ExternalResourceChangeListener`, process all changes |
| Background processing — only one node should do the work | Check `!isExternal()` and skip if external |
| Primary + secondary store updates on different nodes | Check `isExternal()` and branch logic accordingly |

**Process all changes regardless of origin** — cache invalidation, search index
updates, anything where every node must react to every change no matter where
it came from.

**Skip external changes** — background processing jobs, data sync tasks where
only one node should do the work. The node that made the change processes it;
other nodes ignore it to avoid duplicate job execution.

**Handle local and external differently** — primary data store updates on the
originating node, secondary data store sync on all other nodes.

## The Duplicate Job Problem

The most common mistake: implementing `ExternalResourceChangeListener` and firing a Sling Job **without** checking `isExternal()`.

- 2-node cluster → 2 identical jobs
- 4-node cluster → 4 identical jobs

**Fix:** Check `isExternal()` before firing. Fire a job only from the node where the change originated, or rely on the fact that a Sling Job itself is cluster-aware and will run on exactly one node.

```java
@Override
public void onChange(List<ResourceChange> changes) {
    for (ResourceChange change : changes) {
        if (!change.isExternal()) {
            jobManager.addJob("my.topic.internal", props); // only this node fires
        } else {
            jobManager.addJob("my.topic.external", props); // other cluster nodes
        }
    }
}
```

## Author Cluster vs Publish Farm

| | Author Cluster | Publish Farm |
|---|---|---|
| Nodes | Peers sharing a single JCR (MongoMK/TarMK-shared) | Independent — each node has its own JCR |
| How changes propagate | Oak/Jackrabbit replication between peers | Author-to-publish replication via Sling replication |
| `ExternalResourceChangeListener` useful? | **Yes** | **No** — publish nodes do not share a JCR |

## Gotchas
**REMOVED events may be for a parent node.** If `/content/mysite` is deleted, you get one REMOVED event for the parent, not individual events for each child. Your handler must check whether the removed path is an ancestor of your registered path, not just an exact match.
**Do not open a ResourceResolver inside `onChange()`.** The event thread has no session. Always use a service ResourceResolver from `ResourceResolverFactory`, and close it in a `finally` block.
**External events may arrive slightly delayed.** Oak replication has a small lag. Design your handler to tolerate out-of-order or delayed events.
**`immediate = true` is mandatory.** Without it, the OSGi framework may create and destroy the component for every event, causing missed events during instantiation.

## Evolution of Listener APIs — What to Use

| API | Status | Notes |
|---|---|---|
| `JCR EventListener` | Legacy — avoid | Raw JCR API, no cluster awareness, no glob paths |
| `OSGi EventHandler` + `SlingConstants` resource topics | Deprecated for resource changes | No cluster awareness, no glob paths |
| `ResourceChangeListener` alone | Current | Correct, but misses external cluster events |
| `ResourceChangeListener` + `ExternalResourceChangeListener` | Current — preferred | Receives changes from all cluster nodes |

## OSGi Config Properties That Matter

`ResourceChangeListener.PATHS` — one or more JCR paths or glob patterns to
watch. Use `glob:` prefix for pattern matching. Use `.` to watch everything
(use with caution in production).
`ResourceChangeListener.CHANGES` — which change types to subscribe to. Values
are ADDED, CHANGED, REMOVED, PROVIDER_ADDED, PROVIDER_REMOVED. Only subscribe
to what you actually need.

## Key Differences From Deprecated Approaches

`JCR EventListener` — raw JCR API, no cluster awareness, no glob paths,
legacy, avoid.
`OSGi EventHandler` with `SlingConstants` resource topics — deprecated for
resource changes, no cluster awareness, no glob paths, avoid for content events.
`ResourceChangeListener` alone — correct API, but misses external cluster
events. Use when your logic only needs to react to local changes.
`ResourceChangeListener` + `ExternalResourceChangeListener` — correct and
complete. Use whenever your logic must react to changes from any node in
the cluster.

## Best Practices
* Only process external events when synchronization between cluster nodes is required.
* Always verify whether duplicate processing can occur in Author clusters.
* Avoid assuming a listener executes only once.

## Common Mistakes
- Ignoring isExternal().
- Processing every cluster event.
- Triggering duplicate workflows.
- Assuming Publish farms behave like clustered Authors.

## Real Project Scenario
An asset approval workflow replicates content.
**Without cluster awareness:**
```
Author A
↓
Workflow
↓
Replication
↓
Author B receives same event
↓
Workflow starts again
↓
Duplicate replication
```
Using cluster-aware listeners prevents repeated processing.

## Production Readiness Checklist
1. [x] Cluster topology understood
2. [x] Duplicate execution prevented
3. [x] isExternal() evaluated
4. [x] Workflow loops avoided

## Architecture Decision Guide
| Environment | Recommendation |
|-------------|---------------|
| Single Author | ResourceChangeListener |
| Clustered Author | ExternalResourceChangeListener |
| Publish Farm | Normal Listener |
| Shared repository | Check isExternal() |

---

# Sling Filters

## What Is a Sling Filter?

A Sling Filter is a Java class that intercepts HTTP requests and responses in AEM — before they reach a servlet or component, and after the response is generated. It follows the standard `javax.servlet.Filter` contract.

**Use a filter when you need cross-cutting logic that applies to many requests:**
- Authentication and token validation
- Adding or modifying response headers
- Logging and request monitoring
- Response body modification (e.g. injecting scripts)
- Global error handling

## Old Way vs New Way

**Old way** — string-based properties in `@Component` (error-prone, no type safety):

```java
@Component(service = Filter.class, property = {
    EngineConstants.SLING_FILTER_SCOPE + "=" + EngineConstants.FILTER_SCOPE_REQUEST
})
@ServiceRanking(-700)
public class LoggingFilter implements Filter { }
```

**New way** — type-safe `@SlingServletFilter` annotation (recommended for all new development):

```java
@Component(property = { "service.ranking:Integer=-800" })
@SlingServletFilter(
    scope         = { SlingServletFilterScope.REQUEST },
    pattern       = "/content/sibi-aem-one/.*",
    resourceTypes = { "sibi-aem-one/components/page" },
    selectors     = { "print", "mobile" },
    extensions    = { "html", "json" },
    methods       = { "GET", "POST", "HEAD" }
)
public class ModernLoggingFilter implements Filter { }
```

## The Five Filter Scopes

| Scope | When it fires |
|---|---|
| `REQUEST` | Every incoming HTTP request from a client. The most commonly used scope. |
| `INCLUDE` | When `RequestDispatcher.include()` is called — one component including another. |
| `FORWARD` | When `RequestDispatcher.forward()` is called — uncommon in AEM. |
| `ERROR` | When `sendError()` is called or an uncaught `Throwable` escapes the servlet. |
| `COMPONENT` | Legacy — fires across REQUEST, INCLUDE, and FORWARD. Avoid in new code. |

## Filter Narrowing Properties

Every property you add to `@SlingServletFilter` narrows the set of requests that trigger the filter. Only requests matching **all** specified conditions will call `doFilter()`.

| Property | What it restricts |
|---|---|
| `pattern` | Regex match on the request resource path. Omitting it fires on every request — almost never correct in production. |
| `extensions` | File extensions (e.g. `html`, `json`). A filter for `html` will not fire on `json` API calls. |
| `methods` | HTTP methods (GET, POST, HEAD). Only subscribe to what you need. |
| `resourceTypes` | `sling:resourceType` of the resolved resource. Targets a specific component type. |
| `selectors` | Sling selectors present in the URL. |

## Service Ranking — Filter Execution Order

A **more negative** value means **higher priority** (the filter runs earlier).

| Ranking Range | Recommended use |
|---|---|
| `-100` to `-500` | Authentication checks, security headers |
| `-500` to `-800` | Logging, monitoring |
| `-800` to `-1000` | Response modification, caching |

If two filters have the same ranking, execution order is not guaranteed. Always assign explicit rankings when order matters.

## The Filter Chain

Every filter receives a FilterChain object. Calling chain.doFilter() passes
the request to the next filter in the chain, or to the servlet if no more
filters remain.

Code written before chain.doFilter() is pre-processing. It runs before the
request reaches the servlet or component. This is where you do auth checks,
validation, and request modification.

Code written after chain.doFilter() is post-processing. It runs after the
response has been generated. This is where you add response headers, log
response status codes, or modify the response body.

```java
@Override
public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

    // --- PRE-PROCESSING ---
    // Runs before the request reaches the servlet/component.
    // Auth checks, validation, request header modification go here.

    chain.doFilter(request, response); // ← MUST call this or the request is blocked

    // --- POST-PROCESSING ---
    // Runs after the response has been generated.
    // Add response headers, log status code, modify response body here.
}
```

**If you do not call `chain.doFilter()`**, the request is blocked. The client receives only what your filter writes. This is intentional for auth filters that must return 401 or 403.

## Response Wrapping — Modifying the Response Body

You cannot read or modify the response body after it has been written. The solution is to wrap the response *before* calling `chain.doFilter()`:
If you need to read or modify the response body, you cannot do it directly
because by the time your post-processing code runs the response has already
been written. You must wrap the response before calling chain.doFilter().

A response wrapper intercepts the output that the servlet writes and buffers
it in memory. After chain.doFilter() returns you read the buffer, modify it,
and write the modified content to the real response.

Response wrapping is expensive because the full response body is held in
memory. Only use it when you genuinely need to modify the output. Never use
it on high-traffic paths without measuring the memory impact first.

```java
// 1. Wrap the response to intercept the servlet's output
BufferedHttpResponseWrapper wrappedResponse =
    new BufferedHttpResponseWrapper((HttpServletResponse) response);

// 2. Let the servlet render into the buffer
chain.doFilter(request, wrappedResponse);

// 3. Read the buffered content
String html = wrappedResponse.getBufferedContent();

// 4. Modify and write to the real response
String modified = html.replace("</body>",
    "<script src='/etc/clientlibs/tracking.js'></script></body>");
response.setContentLength(modified.getBytes(response.getCharacterEncoding()).length);
response.getWriter().write(modified);
```

> **Performance warning:** Response wrapping holds the entire page in memory. Never use it on high-traffic paths without profiling. Only use it when you genuinely need to modify the output.

## Disabling a Filter at Runtime

Push an OSGi config that sets `sling.filter.scope` to an invalid value (e.g. `disabled`). Sling ignores filters with an unrecognised scope. The filter stops executing immediately — no redeployment needed.

## Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Forgetting `chain.doFilter()` in a conditional branch | Request is silently blocked for that condition | Trace every code path; ensure `chain.doFilter()` always executes when the request should proceed |
| Heavy work inside `doFilter()` | Blocks the request thread; every request on that path is slowed | Delegate to a Sling Job for any non-trivial work |
| No `pattern` or `methods` restriction | Filter fires on every request including internal Sling calls | Always scope as tightly as possible |
| Modifying headers after the response is committed | Headers are silently ignored | Set headers before calling `chain.doFilter()` or immediately after, while the response is still open |
| Hardcoding `UTF-8` in response wrapping | Encoding mismatch on non-UTF-8 responses | Always use `response.getCharacterEncoding()` |

## Filters vs Event Handlers vs Schedulers

| | Sling Filter | Event Handler / Listener | Scheduler |
|---|---|---|---|
| Execution thread | Request thread — synchronous | Background event thread | Background scheduler thread |
| Triggered by | HTTP request | JCR/OSGi event | Cron expression or interval |
| Must be fast? | Yes — directly impacts request latency | Yes — backs up event queue | Less critical — no user waiting |
| Right tool for | Inspecting / modifying HTTP requests & responses | Reacting to content changes or system events | Periodic background tasks |

## Best Practices

* Prefer resource-type restrictions whenever possible.
* Keep filters stateless.
* Always continue the filter chain unless intentionally terminating the request.
* Use response wrappers only when response modification is genuinely required.

## Common Mistakes

- Forgetting chain.doFilter().
- Modifying binary responses.
- Heavy processing inside filters.
- Registering filters too broadly.
- Depending on service ranking unintentionally.

## Real Project Scenario

Marketing requires every HTML page to include a Content Security Policy header.
```
Browser
↓
Dispatcher
↓
AEM
↓
Response Filter
↓
Adds CSP Header
↓
Browser
```
The filter injects headers without modifying application code.

## Production Readiness Checklist

1. [x] Stateless implementation
2. [x] Correct scope selected
3. [x] Filter chain preserved
4. [x] Headers validated
5. [x] Logging added
6. [x] Unit tests written

## Architecture Decision Guide
| Requirement | Use |
|------------|-----|
| Modify request | Filter |
| Modify response | Response Wrapper |
| Add headers | Filter |
| Authentication | Authentication Handler |
| Authorization | Dispatcher + ACL |

---

# Request Flow — Browser → CDN → Dispatcher → AEM

## Full Request Lifecycle
```
Browser
   │
   │ HTTP request
   ▼
CDN
   ├── cache hit?  ──► return cached response to browser
   │                   (AEM, Dispatcher, and filters never see this request)
   │ cache miss
   ▼
Dispatcher
   ├── cache hit?  ──► return cached HTML to CDN
   │                   (AEM and filters never see this request)
   │ cache miss or invalidation
   ▼
AEM Publish
   │
   ▼
Sling Authentication (login / session resolution)
   │
   ▼
REQUEST Filter — pre-processing          ◄── your filter fires here (before chain.doFilter)
   │
   ▼
Sling Resource Resolution (path → resource → component)
   │
   ▼
[INCLUDE scope filters fire per component inclusion]
   │
   ▼
Servlet / Component renders HTML
   │
   ▼
REQUEST Filter — post-processing         ◄── your filter fires here (after chain.doFilter)
   │
   ▼
Response leaves AEM
   │
   ▼
Dispatcher caches HTML on filesystem → returns to CDN
   │
   ▼
CDN caches response → returns to Browser
   │
   ▼
Browser renders page
```

## Practical Implications

**Your filter only fires when the request actually reaches AEM.** If CDN or Dispatcher serves a cached response, your filter is never called for that request.

This means:
- You **cannot** use a Sling Filter to intercept every single user request.
- Logic that must run on every page view (analytics, personalisation) belongs in the browser via JavaScript.
- Logic that only needs to run when AEM renders a page (security headers, token validation, response modification) is correct in a Sling Filter.

## Security Headers and Caching — A Critical Note

Security headers added by a Sling Filter (e.g. `X-Frame-Options`, `Content-Security-Policy`) will only be present on responses that AEM renders directly. **Cached responses from Dispatcher or CDN will not carry those headers.**

**Recommended approach:** Configure security headers at the Dispatcher level using the Apache `mod_headers` directive. This ensures headers are present on all responses, including cached ones.

## Dispatcher Cache Invalidation and Filters

When an author publishes a page:

1. AEM sends a cache invalidation request to Dispatcher.
2. Dispatcher marks the cached file as stale.
3. The next request for that page misses the Dispatcher cache.
4. The request reaches AEM — **your filter fires**.
5. AEM renders the page fresh.
6. Dispatcher caches the new HTML.

Your filter therefore fires on the **first request after every publish event**, and then not again until the cache is invalidated next time.

## Best Practices

### Cache everything that can be cached
AEM is optimized when most requests are served by Dispatcher rather than the Publish instance.
Dynamic rendering should be reserved for genuinely personalized or uncacheable content.

### Keep Dispatcher filters restrictive
Only expose URLs that must be publicly accessible.
Deny-by-default is safer than allow-by-default.

### Separate security from caching
Security headers should be added consistently regardless of whether the response comes from Dispatcher or AEM.

### Minimize Publish traffic
A Publish server should spend its CPU rendering new pages, not repeatedly rendering identical cached pages.

## Common Mistakes

- Caching authenticated pages.
- Disabling Dispatcher cache unnecessarily.
- Allowing unrestricted Dispatcher filters.
- Assuming Dispatcher replaces CDN caching.
- Forgetting cache invalidation after replication.

## Real Project Scenario

### Global Marketing Website
A marketing website receives over one million requests per day.

```
Browser
    │
    ▼
CloudFront CDN
    │
    ▼
Dispatcher
    │
Cache Hit?
 ├── Yes → Return cached HTML
 └── No
         │
         ▼
     Publish
         │
         ▼
Rendered HTML
         │
         ▼
Dispatcher Cache
         │
         ▼
Browser
```

Only a small percentage of requests reach AEM, dramatically reducing infrastructure costs and improving response times.

## Production Readiness Checklist

1. [x] Dispatcher cache configured
2. [x] Cache invalidation tested
3. [x] Dispatcher filters secured
4. [x] CDN configured correctly
5. [x] Compression enabled
6. [x] Security headers verified

## Architecture Decision Guide

| Requirement | Layer |
|------------|------|
| SSL | CDN |
| Static cache | CDN |
| HTML cache | Dispatcher |
| Rendering | Publish |
| Content storage | Oak |
| Authentication | Publish |

---

# AEM Workflows

## Engine Architecture

The AEM Workflow engine is built on the **Granite Workflow** framework and operates as a state machine. Each step transition is managed by the **Workflow Engine Service**.

Under the hood, workflow steps are executed as **Sling Jobs**:
- In clustered environments, Sling Job distribution can offload steps to different nodes.
- Every step completion triggers a JCR write that persists state. If an instance crashes, the workflow resumes from the last persisted checkpoint.

**JCR node locations:**

| Data | Path |
|---|---|
| Workflow model definition | `/conf` or `/etc/workflow/models` |
| Runtime instance data (history, state, actors) | `/var/workflow/instances` |

## Granite Workflow vs. Sling Jobs

While workflows appear as a continuous process in the UI, they are technically executed as **Sling Jobs**.
* **Job Offloading:** In clustered 6.5 environments, the Sling Job distribution ensures that workflow steps can be offloaded to different instances.
* **Consistency:** Every step completion triggers a JCR write to persist the state, ensuring that if an instance crashes, the workflow can resume from the last persisted "checkpoint."
*

## State Management — The Three Metadata Maps

Understanding these three maps is fundamental to workflow development.

| Map | Scope | Lifetime | Primary Use |
|---|---|---|---|
| `args` (MetaDataMap) | Step-specific | Immutable — set in the Workflow Model editor | Reading static config values defined at design time |
| `item.getMetaDataMap()` | Step-specific runtime | Duration of the current step only | Short-lived data used within a single step's logic |
| `item.getWorkflowData().getMetaDataMap()` | Instance-wide | Entire workflow lifetime | **Passing data between steps** — the shared memory of the workflow |

## **WorkItem: The Runtime Container**
The `WorkItem` is the object that represents the current instance of a workflow as it passes through a specific step. It acts as the "handle" for the engine's execution state.

* **Identity:** It contains the ID of the current step and the overall workflow instance.
* **Data Access:** It provides the primary gateway to the **WorkflowData**, which contains the payload (the asset or page path).
* **Persistence:** It is used to access the long-term memory of the workflow that persists across different steps.

## **MetaDataMap (args): The Design-Time Configuration**
The `MetaDataMap` passed as the third parameter in the `execute` method represents the **Process Arguments**. These are values configured by the developer or author within the Workflow Model editor.

* **Function:** It allows a single Java class to be reused across different workflow models by passing unique parameters.
* **Scope:** It is local to the current step configuration.
* **Source:** Values are sourced from the "Process Arguments" text field or the metadata dialog in the Workflow Step UI.

## WorkItem vs MetaDataMap (args) — Detailed Comparison

| Feature | `WorkItem` | `MetaDataMap args` |
|---|---|---|
| Java Type | `com.adobe.granite.workflow.exec.WorkItem` | `com.adobe.granite.workflow.metadata.MetaDataMap` |
| Purpose | "What is happening?" — runtime execution context | "How should it behave?" — design-time configuration |
| Payload access | Yes — `item.getWorkflowData().getPayload()` | No |
| Data lifetime | Duration of the workflow instance | Immutable; defined in the model |
| Typical use | Getting the asset/page path being processed | Reading an API key, folder path, or flag from the model UI |

**Practical Java example:**

```java
public void execute(WorkItem item, WorkflowSession session, MetaDataMap args)
        throws WorkflowException {

    // 1. Get the content path being processed (WorkItem — "The What")
    String payloadPath = item.getWorkflowData().getPayload().toString();

    // 2. Get design-time config from the model's Process Arguments field (args — "The How")
    String folderName = args.get("PROCESS_ARGS", "default-folder");

    // 3. Pass data to the NEXT step via WorkflowData metadata (shared memory)
    item.getWorkflowData().getMetaDataMap().put("processingComplete", true);
}
```

**Memory analogy:**
- The **WorkItem** is the **Passenger** — it knows where it is going (payload path) and carries a suitcase (WorkflowData Metadata) from step to step.
- The **MetaDataMap (args)** is the **House Manual** — each step has its own instructions telling the passenger how to behave while inside that step.

## Execution Patterns

### The Three Maps of Workflow Development
As a senior developer, you must distinguish between the three different metadata maps available during a process step execution:
1.  **Step Metadata (`args`):**
  * *Source:* The "Arguments" in the Workflow Model Step dialog.
  * *Usage:* Reading static configurations for the code.
2.  **WorkItem Metadata (`item.getMetaDataMap()`):**
  * *Source:* Specific to the current execution of this step.
  * *Usage:* Very short-lived data used within the step's logic.
3.  **WorkflowData Metadata (`item.getWorkflowData().getMetaDataMap()`):**
  * *Source:* The shared memory for the entire workflow instance.
  * *Usage:* **Crucial.** Use this to pass data from Step A to Step B (e.g., a "route" flag or an external system ID).


### Transient Workflows

For high-volume automation where an audit trail is not required.

- Do **not** create nodes under `/var/workflow/instances`.
- The entire process runs in memory and is only committed to JCR at the final step.
- **Benefit:** Dramatically reduces JCR contention and prevents Oak index bloat during bulk asset imports.

### Participant Steps and the AEM Inbox

For human-in-the-loop approval processes.

- A **Participant Step** places a task in the **AEM Inbox** for a specific user or group.
- A **Dynamic Participant Chooser** evaluates the payload at runtime and returns the appropriate user/group ID.

```java
@Component(
    service  = ParticipantStepChooser.class,
    property = { "chooser.label=Locale-Based Translator Chooser" }
)
public class LocaleParticipantChooser implements ParticipantStepChooser {
    public String getParticipant(WorkItem item, WorkflowSession session,
                                 MetaDataMap args) throws WorkflowException {
        String path = item.getWorkflowData().getPayload().toString();
        if (path.contains("/fr/")) return "fr-translators";
        if (path.contains("/de/")) return "de-translators";
        return "global-reviewers";
    }
}
```

## Advanced Routing

| Feature | Description |
|---|---|
| **OR Split** | Routes the workflow into one of multiple branches based on a routing expression or script. Set a metadata variable in a process step; the OR Split evaluates it. |
| **AND Split** | Parallelises execution across multiple branches. The workflow only continues once all branches have reached the join point. |
| **Goto Step** | Jumps backward or forward in the model graph based on a metadata value — enables retry loops and conditional skips without duplicating model steps. |

**Retry loop pattern using Goto Step:**

```java
// In the process step:
if (retryCount < maxRetries) {
    item.getWorkflowData().getMetaDataMap().put("retryCount", retryCount + 1);
    item.getWorkflowData().getMetaDataMap().put("syncStatus", "RETRY");
    // The Goto Step in the model checks syncStatus == "RETRY" and loops back
} else {
    item.getWorkflowData().getMetaDataMap().put("syncStatus", "FAILED");
    throw new WorkflowException("Max retries exceeded");
}
```

## Automation & Launchers

Launchers are the event listeners that bridge the JCR and the Workflow Engine — they start a workflow automatically when content changes.

- **Trigger events:** `NODE_CREATED`, `NODE_MODIFIED`, `NODE_REMOVED`
- **Path filtering:** Use glob patterns to target specific content trees.
- **Property conditions:** e.g. `jcr:content/metadata/dc:format == image/jpeg` to target only specific asset types.
- **Exclusion lists:** Always configure exclusions to prevent infinite loops (a workflow that modifies a property should not re-trigger itself).

## 6.5 vs AEM as a Cloud Service — The Asset Microservices Shift

| | AEM 6.5 | AEM as a Cloud Service |
|---|---|---|
| Asset processing | **DAM Update Asset** workflow — all binary processing in-JVM | **Asset Microservices** — external cloud services handle rendition generation |
| Custom workflows | Can manipulate binaries directly | Should run as post-processing workflows after microservices complete |
| Binary handling | Direct JVM manipulation | Avoid — use metadata triggers and external API calls instead |

## Performance: Throttling and Purging

**Throttled Task Runner (OSGi config):**
- Set **Max Parallel Jobs** to cap concurrent workflow steps.
- Set **Resource Limits** to stop spawning new threads when CPU or heap exceeds a threshold.

**Workflow Purging:**
- `/var/workflow/instances` grows indefinitely for non-transient workflows.
- Regular purging is mandatory for JCR performance.
- Target purge configurations by status (`COMPLETED`, `TERMINATED`, `ABORTED`) and age.

## Best Practices

- **Service User:** All automated process steps must use a restricted service user, not an admin session.
- **Single-purpose steps:** Break complex logic into multiple small steps — easier to debug, test, and reuse.
- **Event-driven external integration:** Use Adobe I/O events to offload waiting on external systems. Do not leave a workflow in `RUNNING` state waiting for a slow API — this holds threads and resources.
- **Transient workflows for bulk operations:** Always use transient workflows for high-volume asset processing to avoid JCR bloat.

## Best Practices

### Keep workflow steps independent
Every workflow step should perform one responsibility only.
Small workflow steps are easier to maintain and reuse.

### Prefer transient workflows
If audit history is unnecessary, transient workflows significantly reduce repository writes.

### Keep process steps fast
Long-running operations should be delegated to asynchronous processing.

### Minimize workflow metadata
Store only information required by later workflow steps.

## Common Mistakes

- Using workflows for simple background processing.
- Storing large objects inside workflow metadata.
- Creating unnecessarily long workflows.
- Ignoring transient workflows.
- Performing replication inside multiple workflow steps.

## Real Project Scenario

### Asset Approval Workflow
```
Author uploads Asset
↓
Metadata Extraction
↓
AI Tagging
↓
Legal Approval
↓
Marketing Approval
↓
Replication
↓
Dispatcher Flush
↓
Published
```

Each participant performs one business responsibility, making the workflow easier to maintain and troubleshoot.

## Production Readiness Checklist

1. [x] Workflow kept modular
2. [x] Metadata minimized
3. [x] Transient workflow evaluated
4. [x] Launchers filtered correctly
5. [x] Participant steps clearly assigned
6. [x] Purge configuration enabled

## Architecture Decision Guide

| Requirement | Recommendation |
|------------|---------------|
| Human approval | Workflow |
| Background processing | Sling Job |
| Scheduled execution | Scheduler |
| Immediate processing | Service |
| Asset processing | Workflow |
| Notification | Workflow / Event |

---

# Product Catalog Scenario — Connecting dots (Child Resource, Custom Serializer, Replication, Notification)

> All four are demonstrated together via one connected real-world scenario: an e-commerce product page with auto-publish and an approval workflow.

## @ChildResource — Multifield to Sling Model List

A dialog multifield stores repeating child nodes (e.g. product size/colour variants) under a container node. `@ChildResource(name = "variants")` injects them as `List<Resource>`; each `Resource` is then adapted to its own Sling Model (`adaptables = Resource.class`) inside `@PostConstruct`.

```java
@ChildResource(name = "variants")
private List<Resource> variantResources;

@PostConstruct
protected void init() {
    variants = variantResources.stream()
            .map(r -> r.adaptTo(ProductVariant.class))
            .filter(Objects::nonNull)   // adaptTo() can return null — never skip this
            .collect(Collectors.toList());
}
```

Child models always use `adaptables = Resource.class` — they have no servlet request of their own.

## Custom Jackson Serializer

The default exporter dumps every getter on a Sling Model into JSON. A custom `JsonSerializer<List<X>>`, applied via `@JsonSerialize(using = ...)` on a single DTO field, lets you control exactly which fields appear, rename them, and conditionally omit entries — none of which is possible with plain `@JsonIgnore`/`@JsonProperty` annotations alone.

## Programmatic Replication Trigger

Calling `Replicator.replicate()` directly from Java — from a `ResourceChangeListener` or a workflow step — instead of relying on a human clicking "Publish".

```java
ReplicationOptions options = new ReplicationOptions();
options.setSynchronous(false);     // never block the event thread
replicator.replicate(session, ReplicationActionType.ACTIVATE, pagePath, options);
```

Always use a service-user session for this in a listener (never admin), and always set `setSynchronous(false)` so the calling thread isn't blocked waiting on the publish instance.

## Workflow Replication Step + Notification Step

A two-step pattern: one `WorkflowProcess` activates the payload via `Replicator` and records the outcome (`SUCCESS`/`FAILED`) into `WorkflowData`'s `MetaDataMap`; a second step reads that outcome and emails the workflow initiator using AEM's native `MessageGatewayService` (NOT `org.apache.sling.commons.mail.MailService`, which is not the standard AEM mail API).

```java
// Step 1 — publish, write outcome to shared workflow metadata
workItem.getWorkflowData().getMetaDataMap().put("publishStatus", "SUCCESS");

// Step 2 — read it back, send notification
MessageGateway<HtmlEmail> gateway = messageGatewayService.getGateway(HtmlEmail.class);
gateway.send(email);
```

A notification failure must never fail the workflow if the actual business action (publishing) already succeeded — log and swallow, don't throw `WorkflowException`.

## Common Interview Questions — These Patterns

**Q: Why does `@ChildResource` need `adaptables = Resource.class` on the child model?**  
Because individual child nodes of a multifield don't have an associated `SlingHttpServletRequest` — only the top-level component resource being rendered does. The child is just a JCR resource.

**Q: When do you need a custom Jackson serializer instead of `@JsonIgnore`?**  
When the exclusion/inclusion logic depends on a computed value rather than presence/absence of a field — e.g. "only include variants that are in stock" can't be expressed by an annotation; it needs imperative code in a `JsonSerializer`.

**Q: Why must replication calls from a `ResourceChangeListener` be asynchronous?**  
The resource-change event thread is shared and processes events serially. A synchronous `replicate()` call blocks that thread until the publish instance responds over the network — backing up the entire observation event queue for the whole node.

**Q: What's the correct AEM API for sending email from a workflow step?**  
`com.day.cq.mailer.MessageGatewayService` → `getGateway(HtmlEmail.class)` → `gateway.send(email)`. There is no `org.apache.sling.commons.mail.MailService.sendEmail(Email, String[])` API in standard AEM.

## Best Practices

* Keep serialization independent from Sling Models.
* Use services to coordinate replication.
* Avoid placing notification logic inside workflow steps.
* Keep commerce integrations loosely coupled.

## Common Mistakes

- Returning repository objects directly as JSON.
- Mixing serialization with business logic.
- Replicating inside Sling Models.
- Tight coupling to Commerce APIs.

## Real Project Scenario

An editor updates product availability.
```
Content Fragment
↓
Workflow
↓
Serializer
↓
Commerce Sync
↓
Replication
↓
Dispatcher Flush
↓
Customer Website
```

---

# Custom Widget, Content Fragment & Adobe Launch — Applied Scenario

Extending the Property Listing component with three previously-missing patterns, all in one connected scenario.

## Granite UI Custom Widget

A custom dialog field needs: a component extending the base form field, HTL markup with a real hidden `<input>` that Coral's submit logic actually reads, and JS that calls `.trigger("change")` on every value update — without it, Coral's dirty-tracking never notices the field changed, and the value silently fails to save despite looking correct on screen.

## Content Fragment API

```java
ContentFragment fragment = resource.adaptTo(ContentFragment.class);
Integer score = fragment.getElement("walkabilityScore").getValue(Integer.class);
```

Structured fields (number/boolean/date/multi-value) use `getValue(Class)`; plain text uses `getContent()`. Reference fields (a property page pointing at a separately-authored fragment) are resolved in two steps: get the `Resource` at the stored path, then adapt/construct the model from THAT resource — not from the calling component's own resource.

## Adobe Launch / Client Context

Page-load facts are rendered server-side via a Sling Model that composes with (not duplicates) the existing component model. Interaction events are fired client-side in JS, but always read their payload from a `data-*` attribute the Java model already rendered — keeping the model the single source of truth for what the data IS, while JS only decides WHEN to send it.

```html
<script>window.adobeDataLayer.push(${dataLayer.dataLayerJson @ context='unsafe'});</script>
<button data-property-id="${dataLayer.propertyId}">RSVP</button>
```

## Best Practices

* Keep Granite UI components reusable.
* Avoid embedding business logic in dialog JavaScript.
* Use Content Fragments as structured content rather than page fragments.
* Separate Launch implementation from business logic.

## Common Mistakes

- Hardcoding dialog values.
- Treating Content Fragments like page components.
- Writing large client-side Launch scripts.
- Ignoring cache implications.

## Real Project Scenario

Marketing creates campaign-specific content.
```
Author
↓
Content Fragment
↓
Experience Fragment
↓
Adobe Launch
↓
Analytics
↓
Personalized Experience
```

---

# CSRF Token Handling

## What CSRF is

A third-party site forges a POST request to AEM on behalf of a logged-in user — the browser automatically attaches the session cookie, so AEM cannot distinguish it from a real request.

## How the token fixes it

AEM generates a random token tied to the user's session and puts it in a response body (not a cookie). Cross-origin scripts cannot read response bodies (browser Same-Origin Policy). Without the token, the forged POST is rejected with HTTP 403 by `CSRFFilter` before your servlet ever runs.

## Correct pattern — use the OOTB endpoint, no custom GET needed

```javascript
// Fetch once on page load from AEM's built-in endpoint
fetch("/libs/granite/csrf/token.json", { credentials: "same-origin" })
    .then(res => res.json())
    .then(data => { csrfToken = data.token; });

// Attach to every POST
fetch("/bin/myservlet", {
    method: "POST",
    credentials: "same-origin",
    headers: { "CSRF-Token": csrfToken }
});
```

```html
<!--/* Plain HTML form — render token server-side */-->
<sly data-sly-use.csrf="/libs/granite/csrf/token.json"/>
<form method="POST">
    <input type="hidden" name=":cq_csrf_token" value="${csrf.token}"/>
</form>
```

**Do NOT** write a custom GET endpoint to issue tokens — `/libs/granite/csrf/token.json` already exists. **Do NOT** call `csrfTokenManager.isValidToken()` manually in your servlet — `CSRFFilter` already did it before `doPost()` was reached.

## Three-pillar security model

- HttpOnly session cookie — JS on evil.com can't steal it
- Same-Origin Policy — evil.com can't read response bodies from your domain
- Token tied to session — a stolen token string is useless without the matching session cookie

## Best Practices

### Always use the OOTB CSRF framework
AEM already provides a CSRF token endpoint and validation filter. Avoid implementing custom CSRF solutions unless absolutely necessary.

### Validate all state-changing requests
Any request that modifies repository content should require a valid CSRF token.

### Never disable CSRF validation globally
Disabling CSRF protection exposes every authenticated author to cross-site request forgery attacks.
Instead, whitelist only trusted endpoints when required.

### Keep authentication and CSRF separate
Authentication proves who the user is.
CSRF proves the request originated from a trusted page.

## Common Mistakes

- Creating custom token generators.
- Disabling the CSRF filter.
- Assuming GET requests require CSRF protection.
- Sending tokens through URL parameters.
- Forgetting to include the token in AJAX POST requests.

## Real Project Scenario

An author edits a Content Fragment using a custom React application.
```
React UI
↓
GET /libs/granite/csrf/token.json
↓
Receive Token
↓
POST Content Update
↓
Granite CSRF Filter
↓
Servlet
↓
Repository
```
Every POST request includes the token, ensuring the update originates from a trusted application.

## Architecture Decision Guide

| Situation | Recommendation |
|----------|---------------|
| GET | No CSRF |
| POST | CSRF Token |
| PUT | CSRF Token |
| DELETE | CSRF Token |
| AJAX | Token Header |

---

# XSS Protection — XSSAPI

## The core rule: the right escaper for the right context

| Context | Method | What it escapes |
|---|---|---|
| HTML body text | `encodeForHTML(v)` | `< > & " '` |
| HTML attribute value | `encodeForHTMLAttr(v)` | `" '` and attribute-breakers |
| JavaScript string literal | `encodeForJSString(v)` | `\ ' " newlines </script>` |
| URL in href/src | `filterURLProtocols(v)` then `encodeForHTML(v)` | Dangerous schemes + HTML chars |
| Rich text / RTE output | `filterHTML(v)` | Strips script/iframe/on* events |
| Redirect target | `getValidHref(v)` | Validates structure + protocol |

## Why HTML escaping is WRONG inside a `<script>` block

`&#39;` (HTML entity for `'`) is NOT decoded by the JavaScript engine — it's only decoded by the HTML parser in body text. Inside a `<script>` block the browser is in JS mode. The literal `'` character still breaks out of the string.

`encodeForJSString()` escapes the `'` as `\'` — a JavaScript backslash escape — which the JS engine understands as "this quote is part of the string, not the end of it."

## Injection points that developers commonly miss

- `data-*` attributes (still need `encodeForHTMLAttr`)
- Redirect URLs (`getValidHref` + relative-path check to prevent open redirect)
- The `</script>` sequence inside a JS string (closes the script block in the HTML parser — `encodeForJSString` escapes the `/`)

## JCR values vs request parameters

JCR properties authored via AEM dialog are generally trusted (AEM sanitised them on save). Request parameters are NEVER trusted — treat every `request.getParameter()` as potentially malicious.

## Best Practices

* Always escape output according to the rendering context.
* HTML, JavaScript, CSS, and URLs each require different escaping strategies.
* Prefer HTL's automatic context-aware escaping whenever possible.
* Use XSSAPI only when generating output programmatically in Java.
* Never trust repository content simply because it was authored internally.
* Repository values should be treated as untrusted input.

## Common Mistakes

- Using HTML escaping inside JavaScript.
- Rendering request parameters directly.
- Concatenating HTML strings.
- Disabling HTL context escaping.
- Trusting Content Fragment values.

## Real Project Scenario

A product title contains quotation marks.
**Without context-aware escaping:**
```
<script>

var title="John's "Special" Product";

</script>
```
The page breaks.
Using JavaScript context encoding produces valid JavaScript while preventing XSS.

## Architecture Decision Guide

| Output Context | Escaping Method |
|---------------|----------------|
| HTML | encodeForHTML |
| HTML Attribute | encodeForHTMLAttr |
| JavaScript | encodeForJSString |
| URL | encodeForURL |
| CSS | encodeForCSSString |

---

# Thread Dumps — Reading & Analysis

## What it is
A snapshot of every JVM thread's call stack at one moment. Take 3 dumps 10 seconds apart — patterns across all three reveal genuinely stuck threads vs momentarily slow ones.

## How to take one
```bash
# Method 1: jstack (most reliable)
jstack <PID> > /tmp/threaddump-$(date +%H%M%S).txt

# Method 2: kill signal (Linux — does NOT kill the process)
kill -3 <PID>   # output goes to crx-quickstart/logs/stderr.log

# Method 3: AEM URL (author only)
GET http://localhost:4502/system/console/status-jstack
```

## Thread states
| State | Meaning | AEM implication |
|---|---|---|
| `RUNNABLE` | Actively executing | Normal |
| `BLOCKED` | Waiting to acquire a monitor lock | **Problem — contention** |
| `TIMED_WAITING` | Sleeping with timeout | Usually normal for schedulers |
| `WAITING` | Waiting indefinitely | Could be blocked on I/O or lock |

## Four patterns to look for
**1. Many threads BLOCKED on the same hex address** — find who holds that lock:
```
"http-nio-4502-exec-23" BLOCKED on <0x7b3c>
"http-nio-4502-exec-24" BLOCKED on <0x7b3c>   ← 47 threads all waiting
...
"scheduler-1-thread-1" RUNNABLE - locked <0x7b3c>   ← this one holds it
  at com.sibi.aem.one.core.services.ExternalApiServiceImpl.fetchProductData():89
```
Fix: remove unnecessary `synchronized`, or add HTTP connection request timeout.

**2. "Found one Java-level deadlock"** at the bottom — two threads each holding a lock the other needs. Fix: standardize lock acquisition order.

**3. All request threads WAITING on HTTP pool** — `PoolingHttpClientConnectionManager.requestConnection()` — pool exhausted. Fix: increase `maxConnections` or add timeouts.

**4. Same thread RUNNABLE in same call stack across all 3 dumps** — runaway loop. Cross-reference with `top -H -p <PID>`, convert OS thread ID to hex, find matching `nid=` in the dump.

## Best Practices

* Capture multiple thread dumps 10–15 seconds apart during an issue.
* Comparing multiple dumps provides much more insight than analyzing a single snapshot.
* Always correlate thread dumps with CPU utilization, memory usage, and application logs.
* A thread dump alone rarely tells the complete story.

**Learn to recognize common thread names such as:**
- Jetty
- Sling
- Oak
- Workflow
- JobConsumer
- Dispatcher Flush

## Common Mistakes

- Capturing only one dump.
- Ignoring WAITING threads.
- Looking only for deadlocks.
- Ignoring application logs.
- Confusing blocked threads with sleeping threads.

## Real Project Scenario

Production latency suddenly increases.
```
High CPU
↓
Capture 3 Thread Dumps
↓
Compare Stack Traces
↓
Identify QueryBuilder Loop
↓
Optimize Oak Index
↓
CPU Normal
```

## Architecture Decision Guide

| Symptom | Investigation |
|---------|--------------|
| High CPU | RUNNABLE |
| Hung request | BLOCKED |
| Deadlock | Deadlock section |
| Slow response | Compare dumps |

---

# Heap Dumps — Analysis with Eclipse MAT

## What it is
A binary snapshot of the entire JVM heap. Taken proactively (`jmap`) or automatically on OutOfMemoryError.

```bash
# Auto-dump on OOME — add to JVM args in crx-quickstart/bin/start
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/aem-heapdump.hprof

# Manual dump (pauses JVM briefly)
jmap -dump:live,format=b,file=/tmp/heapdump-live.hprof <PID>
```

## Eclipse MAT — four views
**1. Leak Suspects** — start here. MAT auto-identifies the single biggest accumulation. In AEM this is almost always an unclosed ResourceResolver or unbounded cache.

**2. Histogram** — all classes sorted by Retained Heap (memory freed if that whole class were collected). Look for unexpected counts:
- Thousands of `JcrResourceResolver` → ResourceResolver leak
- One enormous `HashMap` → unbounded cache (fixed in InventoryServiceImpl earlier)

**3. Dominator Tree** — which single object retains the most memory. Traces "lots of X" back to "this piece of your code created and is holding them."

**4. OQL** — query language for targeted hunting:
```sql
SELECT * FROM org.apache.sling.jcr.resource.internal.JcrResourceResolver
SELECT * FROM java.util.HashMap m WHERE m.size > 10000
```

## Three most common AEM heap problems
**1. ResourceResolver leak** — fix: always use try-with-resources:
```java
try (ResourceResolver resolver = factory.getServiceResourceResolver(params)) {
    // resolver.close() called automatically even on exception
}
```
**2. Unbounded cache** — fix: Guava Cache with `maximumSize` + `expireAfterWrite` (fixed in InventoryServiceImpl).

**3. QueryBuilder session leak** — fix: cast query to `Closeable` and close it after iterating hits.

## Best Practices

* Always analyze heap dumps together with GC logs.
* Memory growth alone does not necessarily indicate a leak.
* Look for retained memory rather than shallow memory.
* Objects consuming little memory individually may retain large object graphs.
* Investigate long-lived caches carefully before assuming memory leaks.

## Common Mistakes

- Looking only at object count.
- Ignoring retained size.
- Treating caches as leaks.
- Analyzing very old dumps.
- Ignoring GC activity.

## Real Project Scenario

Publish instances begin restarting with OutOfMemoryError.
```
OOM
↓
Heap Dump
↓
MAT
↓
Leak Suspects
↓
Large Cache
↓
Configuration Fix
↓
Stable Publish
```

## Production Readiness Checklist

* Heap analyzed with MAT
* Dominator Tree reviewed
* Retained memory analyzed
* GC logs reviewed
* Memory fix validated

---

# CIF GraphQL Caching

## Architecture
```
Browser → AEM Publish (CIF + GraphqlClientImpl Guava Cache) → Adobe Commerce/Magento
```
CIF caches GraphQL responses in-process inside the AEM JVM — not in Dispatcher, because GraphQL POST requests have no URL to cache against.

## Cache key
SHA-256 hash of (GraphQL query string + serialised variables). Same query + same variables = cache hit.

## OSGi config (factory — one per commerce endpoint)
```json
// com.adobe.cq.commerce.graphql.client.impl.GraphqlClientConfiguration~mysite.cfg.json
{
  "cacheEnabled": true,
  "cachingTime": 300,
  "cacheSize": 100,
  "httpMethod": "GET"
}
```

## How to see cache stats
```
/system/console/status-com.adobe.cq.commerce.graphql.client  ← hit/miss/eviction rates
/system/console/jmx → search "GraphqlClient"                 ← live stats + invalidateAll
```
Enable DEBUG logging for `com.adobe.cq.commerce.graphql` to see HIT/MISS per query in error.log.

## How to clear the cache
| Method | When to use |
|---|---|
| Wait for TTL | Normal — entries self-expire after `cachingTime` seconds |
| JMX `invalidateAll` | Immediate manual clear without restart |
| OSGi config save | Triggers `@Modified` → cache reinitialised |
| Bundle restart | Nuclear — use only when JMX unavailable |

## Best Practices

* Cache only deterministic GraphQL queries.
* Avoid caching user-specific responses.
* Monitor cache hit ratios regularly.
* A cache with a low hit rate provides little performance benefit.
* Separate cache configuration for each commerce endpoint whenever possible.

## Common Mistakes

- Caching personalized responses.
- Sharing one cache across environments.
- Forgetting cache invalidation.
- Oversized cache configuration.

## Real Project Scenario

An ecommerce site receives heavy product traffic.
```
Browser
↓
AEM
↓
GraphQL Cache
↓
Cache Hit?
├── Yes
│
└── Return Product
↓
No
↓
Commerce API
↓
Cache
↓
Browser
```

Frequently viewed products are served from the GraphQL cache, reducing backend response times and external API load.

---

# AEM Architecture Decision Matrix

| Requirement | Recommended AEM Feature |
|-------------|-------------------------|
| Presentation Logic | Sling Model |
| Business Logic | OSGi Service |
| REST Endpoint | Servlet |
| Background Processing | Sling Job |
| Scheduled Execution | Scheduler |
| Human Approval | Workflow |
| Repository Event | ResourceChangeListener |
| Request Interception | Sling Filter |
| Configuration | OSGi Configuration |
| Repository Query | QueryBuilder |
| Content Rendering | HTL |
| Commerce Integration | CIF GraphQL |
| JSON Serialization | Jackson |
| External API | OSGi Service |
| Authentication | Authentication Handler |
| Authorization | ACL + Dispatcher |
| Cache | Dispatcher / CDN |
| Logging | SLF4J |
| Unit Testing | AEM Mocks + Mockito |

---
