# AEM Core Concepts — Senior Developer Interview Reference

> A complete technical reference covering all core AEM/OSGi/Sling patterns, with interview-focused Q&A, traps, and senior-level depth added throughout.

---

## Table of Contents

1. [Sling Models](#1-sling-models)
2. [OSGi Service Registry](#2-osgi-service-registry)
3. [OSGi Configuration Registry](#3-osgi-configuration-registry)
4. [Sling Servlets](#4-sling-servlets)
5. [Sling Jobs](#5-sling-jobs)
6. [Sling Event Handlers & Resource Listeners](#6-sling-event-handlers--resource-listeners)
7. [Cluster-Aware Listeners](#7-cluster-aware-listeners)
8. [Sling Filters](#8-sling-filters)
9. [Request Flow — Browser → CDN → Dispatcher → AEM](#9-request-flow--browser--cdn--dispatcher--aem)
10. [AEM Workflows](#10-aem-workflows)
11. [JCR & Oak — Repository Fundamentals](#11-jcr--oak--repository-fundamentals)
12. [QueryBuilder & Search](#12-querybuilder--search)
13. [Dispatcher](#13-dispatcher)
14. [AEM Security — Service Users & Permissions](#14-aem-security--service-users--permissions)
15. [Sling Context-Aware Configuration (CAConfig)](#15-sling-context-aware-configuration-caconfig)
16. [AEM as a Cloud Service — Key Differences from 6.5](#16-aem-as-a-cloud-service--key-differences-from-65)
17. [Unit Testing in AEM](#17-unit-testing-in-aem)
18. [AEM Component Development & HTL](#18-aem-component-development--htl)
19. [TagManager & Taxonomy](#19-tagmanager--taxonomy)
20. [Replication API](#20-replication-api)
21. [Common Interview Questions — Senior Level](#21-common-interview-questions--senior-level)

---

## 1. Sling Models

### Best Practice: Interface + Implementation

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

### Adaptables vs Adapters

| Term | Meaning | Example |
|---|---|---|
| **Adaptables** | Where the model comes from — the **input** types it can be created from | `Resource.class`, `SlingHttpServletRequest.class` |
| **Adapters** | What the model can be seen as — the **output** type it exposes | `Author.class` (the interface) |

- Use `SlingHttpServletRequest.class` when you need access to request attributes, headers, or session data.
- Use `Resource.class` when adapting from a JCR node directly (e.g. in a background context with no active request).

### Injection Strategies

`defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL` means a missing property will not cause the model to fail — the field simply remains `null`. Use `REQUIRED` only when the property is truly mandatory for the model to function.

### @PostConstruct

```java
@PostConstruct
protected void init() {
    // Runs once, after ALL field injections have completed.
    // Safe to use injected fields here.
}
```

Use `@PostConstruct` for derived fields, null-checks, and any initialization logic that depends on injected values.

### All Sling Model Injector Annotations — Quick Reference

| Annotation | What it injects | Typical use |
|---|---|---|
| `@Inject` | Any value — tries all injectors in order | General-purpose; less explicit |
| `@ValueMapValue` | A property from the resource's `ValueMap` | JCR node properties |
| `@Named("jcr:title")` | Same as above but with a custom property name | Properties with colons or different names |
| `@Default(values="x")` | Fallback value if the property is absent | Paired with any value injector |
| `@ChildResource` | A child node as a `Resource` or `List<Resource>` | Multifield dialog nodes |
| `@RequestAttribute` | A value set on the `SlingHttpServletRequest` | Cross-component data passing in JSP/HTL |
| `@ResourcePath` | Another `Resource` resolved by a stored path string | Linked resource fields in dialog |
| `@OSGiService` / `@OsgiService` | An OSGi service | Injecting services into models |
| `@Self` | The adaptable itself (request or resource) | Accessing the raw request/resource |
| `@ScriptVariable` | A variable from the HTL/JSP script bindings | `currentPage`, `wcmMode`, `resourceResolver` |
| `@SlingObject` | Core Sling objects | `ResourceResolver`, `ResourceResolverFactory`, `SlingHttpServletResponse` |

### @ChildResource — Multifield Pattern

```java
// Dialog multifield creates child nodes: jcr:content/items/item0, item1, ...
@ChildResource
private List<Resource> items;

// In @PostConstruct, adapt each child resource:
items.stream()
     .map(r -> r.adaptTo(LinkItem.class))
     .filter(Objects::nonNull)
     .collect(Collectors.toList());
```

### Sling Model Delegation Pattern

Used to wrap or extend a Core Component model without copying its source:

```java
@Model(adaptables = SlingHttpServletRequest.class,
       adapters   = Teaser.class,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class CustomTeaserImpl implements Teaser {

    @Self
    @Via(type = ResourceSuperType.class)   // delegates up to the Core Component
    private Teaser delegate;

    @Override
    public String getTitle() {
        return delegate.getTitle();        // pass-through
    }
}
```

> **Interview trap:** Interviewers often ask "how do you extend a Core Component without copying its Java?" — the delegation pattern via `@Via(type = ResourceSuperType.class)` is the answer.

### JSON Export — Common Interview Questions

**Q: How do you expose a Sling Model as a JSON endpoint?**  
Add `@Exporter(name="jackson", extensions="json", selector="model")` to the model class. The URL becomes `<resource>.model.json`.

**Q: How do you exclude a field from JSON output?**  
Annotate the getter or field with `@JsonIgnore`.

**Q: How do you rename a JSON key?**  
Annotate the getter with `@JsonProperty("customName")`.

**Q: What is `WRAP_ROOT_VALUE`?**  
It wraps the entire JSON output under the `@JsonRootName` value as a root key. Without it: `{"title":"x"}`. With it: `{"AuthorDetails":{"title":"x"}}`.

### Common Interview Questions — Sling Models

**Q: What is the difference between `@Inject` and `@ValueMapValue`?**  
`@Inject` is a meta-annotation that tries all registered injectors in priority order until one succeeds. `@ValueMapValue` is explicit — it only reads from the resource's `ValueMap`. Using `@ValueMapValue` is preferred because it's predictable and faster (no injector chain traversal).

**Q: Can a Sling Model adapt from both `Resource` and `SlingHttpServletRequest`?**  
Yes — list both in `adaptables`. But be careful: if you inject `@SlingObject SlingHttpServletResponse`, it is only available when adapting from a request, not from a resource. Mixing both adaptables requires `DefaultInjectionStrategy.OPTIONAL` so resource-only adaptation doesn't fail on request-only injections.

**Q: When does `@PostConstruct` fail silently?**  
If `defaultInjectionStrategy = REQUIRED` and any required field is missing, the model adaptation fails before `@PostConstruct` is even called. The `adaptTo()` call returns `null`. Always check for null when using `adaptTo()`.

**Q: How do you unit-test a Sling Model?**  
Use `AemContext` from the `io.wcm.testing.aem-mock` library. Register your model, load mock content, adapt, and assert:
```java
AemContext ctx = new AemContext();
ctx.addModelsForClasses(AuthorImpl.class);
ctx.load().json("/content.json", "/content");
ctx.currentResource("/content/mynode");
Author model = ctx.request().adaptTo(Author.class);
assertNotNull(model);
assertEquals("Sibi", model.getFirstName());
```

---

## 2. OSGi Service Registry

### How Multiple Implementations of a Service Are Resolved

When multiple classes implement the same OSGi service interface, the framework must pick one. By default, the implementation with the **lowest ServiceID** (the one registered first) is used. This is non-deterministic across restarts.

There are two reliable ways to control which implementation is used.

---

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

---

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

### OSGi Component Lifecycle — @Activate, @Modified, @Deactivate

```java
@Activate
protected void activate(MyConfig config) {
    // Called when the component is first created or its config is applied.
    // Safe to read config and initialise resources here.
}

@Modified
protected void modified(MyConfig config) {
    // Called when the OSGi config is updated at runtime (e.g. via Felix console).
    // If not declared, @Activate is called again on modification.
    // Declare @Modified separately when you need different logic (e.g. teardown + reinit).
}

@Deactivate
protected void deactivate() {
    // Called when the bundle stops or the component is deregistered.
    // Release all resources: close HTTP clients, ResourceResolvers, thread pools.
}
```

### OSGi Reference Cardinality & Policy

| Cardinality | Meaning |
|---|---|
| `MANDATORY` (default) | Exactly one — component won't start without it |
| `OPTIONAL` | Zero or one — component starts even if the service is absent |
| `MULTIPLE` | Zero or more — all matching services |
| `AT_LEAST_ONE` | One or more — at least one must be present |

| Policy | Meaning |
|---|---|
| `STATIC` (default) | Reference is bound at activation and fixed until restart |
| `DYNAMIC` | Reference can be updated at runtime — requires `synchronized` bind/unbind |

### ConfigurationPolicy

| Value | Meaning |
|---|---|
| `OPTIONAL` (default) | Component starts even with no explicit OSGi config |
| `REQUIRE` | Component only starts if an explicit config exists in `configMgr` |
| `IGNORE` | Component ignores `@Designate` — never reads config |

> **Interview trap:** Setting `ConfigurationPolicy.REQUIRE` is the right way to ensure a component doesn't start with default values in production. Interviewers often ask why a scheduler isn't running — a common root cause is a missing config file when `REQUIRE` is set.

### Common Interview Questions — OSGi

**Q: What is the difference between `@Component` and `@Service`?**  
`@Service` is a legacy Felix annotation (pre-DS 1.3). `@Component` from `org.osgi.service.component.annotations` is the current standard. Always use the OSGi DS annotations, never the Felix ones.

**Q: What happens if two services have the same `@ServiceRanking`?**  
The one with the lower ServiceID (registered first) wins. This is non-deterministic across restarts. Always use explicit, unique rankings when order matters.

**Q: How do you verify your bundle is active?**  
Check `/system/console/bundles`. A bundle in `Installed` or `Resolved` state (not `Active`) usually means an unresolved import — a missing package dependency in the bundle's manifest.

**Q: What is a bundle fragment? When would you use one?**  
A fragment bundle attaches to a host bundle and contributes its classpath. Used to inject configuration or resources into a host bundle without modifying it. Rare in modern AEM development but appears in legacy setups.

---

## 3. OSGi Configuration Registry

This section covers how to manage multiple factory instances of a service (e.g. one reCAPTCHA config per site) and the two lifecycle patterns for maintaining a runtime registry of those instances.

### Pattern Overview

**Reference files:**
- `v1`: `services/impl/v1/GoogleRecaptchaConfigServiceImpl.java` — Container-managed
- `v2`: `services/impl/v2/GoogleRecaptchaConfigServiceImpl.java` — Application-managed

---

### Container-Managed Lifecycle (v1 — Bind/Unbind Pattern)

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

The container handles:
- Watching the service registry for new/removed instances
- Deciding when `bind()` / `unbind()` are called
- Enforcing ordering, ranking, and thread safety
- Dynamic hot-swap when a config changes at runtime

---

### Application-Managed Lifecycle (v2 — Self-Registration Pattern)

Each factory instance registers itself into a static map on activation and removes itself on deactivation. **Your application code owns the lifecycle.**

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

Your code owns:
- When to register and deregister
- Where instances are stored
- Thread safety of the map (use `ConcurrentHashMap`)
- Handling config updates (`@Modified` must re-register)

---

### TL;DR Comparison

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

### OSGi Config File Naming — Run Mode Targeting

OSGi config files in `ui.config` are placed in folders named by run mode. This is how you have different values per environment without code changes.

```
ui.config/src/main/content/jcr_root/apps/mysite/osgiconfig/
├── config/                        ← applies to ALL run modes
│   └── com.example.MyService.cfg.json
├── config.author/                 ← author only
│   └── com.example.MyService.cfg.json
├── config.publish/                ← publish only
│   └── com.example.MyService.cfg.json
├── config.dev/                    ← dev environment only
│   └── com.example.MyService.cfg.json
├── config.stage/                  ← stage only
│   └── com.example.MyService.cfg.json
└── config.prod/                   ← prod only
    └── com.example.MyService.cfg.json
```

**Factory config naming** — for `@Designate(factory=true)`, the file name must include a unique identifier after the PID, separated by a tilde:

```
com.example.GoogleRecaptchaConfigServiceImpl~site1.cfg.json
com.example.GoogleRecaptchaConfigServiceImpl~site2.cfg.json
```

### Common Interview Questions — OSGi Config

**Q: How do you have different database URLs for dev, stage, and prod without changing code?**  
Place the same `@ObjectClassDefinition` config file in `config.dev/`, `config.stage/`, and `config.prod/` folders under `ui.config`, each with the appropriate value. OSGi reads the most specific matching folder.

**Q: What is the underscore-to-dot naming rule?**  
In `@interface Config`, method names use underscores as separators (Java doesn't allow dots in method names). OSGi maps `my_property()` to the key `"my.property"` in the `.cfg.json` file. Example: `scheduler_expression()` → `"scheduler.expression"`.

**Q: What is a factory configuration vs a singleton configuration?**  
A singleton config (`@Designate(factory=false)`, the default) allows exactly one instance of the component. A factory config (`@Designate(factory=true)`) allows multiple named instances — one per `.cfg.json` file with a unique tilde suffix.

---

## 4. Sling Servlets

### serialVersionUID

```java
private static final long serialVersionUID = 1L;
```

`HttpServlet` implements `Serializable`, so every servlet inherits that requirement. The `serialVersionUID` is a version-control ID used during Java deserialization to verify that the class definition on the receiving end matches the one that serialized the object.

If the IDs differ at deserialization time, Java throws `java.io.InvalidClassException`. Always declare it explicitly to avoid compiler warnings and unexpected runtime errors.

---

### Servlet Registration: Two Approaches

#### ResourceType Registration ✅ (Best Practice)

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

#### Path Registration ⚠️ (Legacy)

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

### SlingSafeMethodsServlet vs SlingAllMethodsServlet

| Class | Use when |
|---|---|
| `SlingSafeMethodsServlet` | Read-only (GET, HEAD) — idempotent operations |
| `SlingAllMethodsServlet` | Write operations (POST, PUT, DELETE) |

### Sling DataSource — Dynamic Dialog Dropdowns

A very common senior-level pattern: populate a Touch UI `select` field dynamically from a servlet instead of hardcoding values in the dialog XML.

```java
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "mysite/datasource/categories",
    methods       = HttpConstants.METHOD_GET
)
public class CategoriesDataSource extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request,
                         SlingHttpServletResponse response) {
        List<ValueMap> options = new ArrayList<>();
        // build from JCR query, API call, or static list
        options.add(new ValueMapDecorator(Map.of("text", "Technology", "value", "tech")));
        options.add(new ValueMapDecorator(Map.of("text", "Sports",     "value", "sport")));

        DataSource ds = new SimpleDataSource(options.stream()
            .map(ValueMapResource::new)
            .iterator());
        request.setAttribute(DataSource.class.getName(), ds);
    }
}
```

In the dialog XML, point the `select` field's `datasource` to this resource type:
```xml
<datasource
    jcr:primaryType="nt:unstructured"
    sling:resourceType="mysite/datasource/categories"/>
```

### Common Interview Questions — Sling Servlets

**Q: How does Sling resolve which servlet handles a request?**  
Sling uses a resolution chain: it matches on `sling:resourceType`, then `selectors`, then `extension`, then `HTTP method`. The most specific match wins. This is called **Servlet Resolution** and is documented in the Sling Servlet Resolution spec.

**Q: What is the difference between `doGet()` and `GET()`?**  
`SlingSafeMethodsServlet` exposes `doGet(SlingHttpServletRequest, SlingHttpServletResponse)`. `SlingAllMethodsServlet` adds `doPost()`, `doPut()`, `doDelete()`. Never override the raw `service()` method — let the base class dispatch.

**Q: How do you return JSON from a servlet?**
```java
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
response.getWriter().write(new Gson().toJson(myObject));
```

**Q: Why should you prefer resource-type registration over path registration?**  
Path-registered servlets bypass JCR ACLs. Any authenticated user can reach them. Resource-type-registered servlets inherit the ACLs of the JCR node with that resource type, giving you repository-level access control. Path registration also has known security vulnerabilities in older AEM versions.

**Q: Can a Sling servlet be called server-side (not just via HTTP)?**  
Yes — using `RequestDispatcher.include()` or `RequestDispatcher.forward()`. This is how AEM's `sling:include` HTL tag works internally.

---

## 5. Sling Jobs

### Why Sling Jobs Instead of Plain Schedulers?

| Feature | Plain Sling Scheduler | Scheduled Sling Job |
|---|---|---|
| Survives server restart? | No | **Yes** — persisted in JCR |
| Cluster-aware execution? | No — runs on every node | **Yes** — runs on exactly one node |
| Retry on failure? | No | **Yes** — automatic with backoff |
| Persistent audit trail? | No | Yes — `/var/eventing/jobs/` |

---

### End-to-End Flow of a Scheduled Sling Job

#### Step 1 — OSGi Config is Loaded

AEM reads your `.cfg.json` on startup or bundle deploy:

```json
{
  "enabled": true,
  "timezone1.cron": "0 0 5 * * ?",
  "timezone1.id": "America/New_York"
}
```

Each JSON key maps to an `@interface Config` method using the **underscore-to-dot rule**: `timezone1_cron()` → `"timezone1.cron"`.

#### Step 2 — Config is Injected into the Component

`@Designate(ocd = Config.class)` links the component to the config schema. OSGi calls `@Activate` and passes in the populated config object.

```
.cfg.json  →  @interface Config  →  @Activate(Config config)
```

If the config is later changed in the Felix console, `@Modified` fires — old jobs are unscheduled and new ones registered with the updated values.

#### Step 3 — JobManager Registers the Scheduled Job

```java
jobManager
    .createJob(TOPIC)
    .properties(props)       // e.g. timezoneId, schedulerName
    .schedule()
    .cron("0 0 5 * * ?")
    .add();
```

> **Important:** Before calling `.add()`, always call `getScheduledJobs()` to check for duplicates. Without this guard, the same job gets re-registered on every bundle restart.

#### Step 4 — Job is Persisted in JCR

Unlike a plain `Sling Scheduler`, a scheduled Sling Job is written to the JCR at:

```
/var/eventing/scheduled-jobs/
```

This means it **survives a server restart**. The schedule is not lost when AEM goes down.

#### Step 5 — Cron Fires, Job Instance is Created

When the cron expression fires, Sling Eventing creates a job instance at:

```
/var/eventing/jobs/
```

The properties attached when scheduling (e.g. `timezoneId`, `schedulerName`) are carried into the job instance and available to the consumer.

#### Step 6 — JobConsumer Processes the Job

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

#### Step 7 — JobResult Determines What Happens Next

| Return | Meaning | Job removed? | Retried? |
|---|---|---|---|
| `OK` | Success | Yes | No |
| `FAILED` | Error — try again | No | Yes (with automatic backoff) |
| `CANCEL` | Intentional abort | Yes | No |

Retry count and backoff delay are configurable at `/system/console/configMgr` in the OSGi job queue config.

---

### Complete Flow Diagram

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

### Multi-Timezone Job Pattern

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

### Common Interview Questions — Sling Jobs

**Q: What is the difference between a Sling Scheduler and a Sling Job?**  
A Sling Scheduler is a simple `Runnable` fired by a cron expression. It runs on every cluster node, has no persistence, and has no retry. A Sling Job is persisted in JCR (`/var/eventing/`), runs on exactly one cluster node, and retries automatically on `FAILED`. Use schedulers for lightweight per-node tasks (e.g. cache warm-up). Use Sling Jobs for all business-critical work.

**Q: What happens if the AEM instance goes down mid-job?**  
For plain schedulers — the job is lost. For Sling Jobs — the job instance in `/var/eventing/jobs/` survives. When AEM restarts, the Sling Eventing framework picks it up and re-delivers it to a consumer.

**Q: How do you pass data from a producer to a consumer?**  
Via job properties. The producer adds key-value pairs to the `Map<String, Object>` when calling `jobManager.addJob(topic, props)`. The consumer reads them via `job.getProperty("myKey", String.class)`.

**Q: How do you prevent a job from running on every node in a cluster?**  
Sling Jobs are cluster-aware by design — the job queue distributes to one node automatically. You do not need to add any cluster coordination. The issue only arises with plain schedulers, which must be guarded with JCR locks or `isLeader()` checks.

**Q: What is `JobResult.CANCEL` vs `JobResult.FAILED`?**  
`FAILED` signals a transient failure — retry will be attempted. `CANCEL` signals a permanent, intentional abort — no retry, job is removed. Use `CANCEL` when the error is unrecoverable (e.g. invalid payload path, configuration error).

---

## 6. Sling Event Handlers & Resource Listeners

### When to Use Which

| | OSGi `EventHandler` | `ResourceChangeListener` |
|---|---|---|
| Use for | System events (replication, DAM, workflow, bundle) | JCR content / resource changes |
| Package | `org.osgi.service.event` | `org.apache.sling.api.resource.observation` |
| Status | Current | Current (replaces deprecated EventHandler + SlingConstants resource topics) |
| `immediate = true` required? | Always | Always |
| Heavy work inside? | Never — delegate to Sling Job | Never — delegate to Sling Job |

---

### Common Event Topics

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

---

### The Golden Rule — Keep the Handler Fast

The event thread processes events serially. Any slow work inside `handleEvent()` or `onChange()` will back up the entire event queue for that AEM instance.

**Never do heavy work inside an event handler. Always delegate to a Sling Job:**

```
EventHandler.handleEvent()
└── jobManager.addJob(topic, props)   ← fire and return immediately
        │
        ▼
JobConsumer.process(job)              ← heavy work here, with automatic retry
```

---

### ResourceChangeListener — Path Registration with Glob Patterns

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

---

## 7. Cluster-Aware Listeners

### The Problem

In AEM AMS, the author tier runs as a cluster (typically 2 nodes) that share a single JCR repository. When node 1 makes a change, a plain `ResourceChangeListener` on node 2 will **silently miss it**. Cluster-aware listeners solve this.

---

### The Two Interfaces

```java
// Listens to changes on THIS node only
public class MyListener implements ResourceChangeListener { }

// Listens to changes from THIS node AND all other cluster nodes
public class MyListener implements ResourceChangeListener, ExternalResourceChangeListener { }
```

Both are in `org.apache.sling.api.resource.observation`.

---

### The Key Method: isExternal()

Inside `onChange()`, use `change.isExternal()` to know where the change came from:

| Returns | Meaning |
|---|---|
| `false` | Change happened on **this** node (local) |
| `true` | Change happened on **another** cluster node (external) |

This is the only way to distinguish local from external events.

---

### When to Use Each Approach

| Use case | Approach |
|---|---|
| Cache invalidation, search index updates — every node must react | Implement `ExternalResourceChangeListener`, process all changes |
| Background processing — only one node should do the work | Check `!isExternal()` and skip if external |
| Primary + secondary store updates on different nodes | Check `isExternal()` and branch logic accordingly |

---

### The Duplicate Job Problem

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

---

### Author Cluster vs Publish Farm

| | Author Cluster | Publish Farm |
|---|---|---|
| Nodes | Peers sharing a single JCR (MongoMK/TarMK-shared) | Independent — each node has its own JCR |
| How changes propagate | Oak/Jackrabbit replication between peers | Author-to-publish replication via Sling replication |
| `ExternalResourceChangeListener` useful? | **Yes** | **No** — publish nodes do not share a JCR |

---

### Gotchas

**REMOVED events may be for a parent node.** If `/content/mysite` is deleted, you get one REMOVED event for the parent, not individual events for each child. Your handler must check whether the removed path is an ancestor of your registered path, not just an exact match.

**Do not open a ResourceResolver inside `onChange()`.** The event thread has no session. Always use a service ResourceResolver from `ResourceResolverFactory`, and close it in a `finally` block.

**External events may arrive slightly delayed.** Oak replication has a small lag. Design your handler to tolerate out-of-order or delayed events.

**`immediate = true` is mandatory.** Without it, the OSGi framework may create and destroy the component for every event, causing missed events during instantiation.

---

### Evolution of Listener APIs — What to Use

| API | Status | Notes |
|---|---|---|
| `JCR EventListener` | Legacy — avoid | Raw JCR API, no cluster awareness, no glob paths |
| `OSGi EventHandler` + `SlingConstants` resource topics | Deprecated for resource changes | No cluster awareness, no glob paths |
| `ResourceChangeListener` alone | Current | Correct, but misses external cluster events |
| `ResourceChangeListener` + `ExternalResourceChangeListener` | Current — preferred | Receives changes from all cluster nodes |

---

## 8. Sling Filters

### What Is a Sling Filter?

A Sling Filter is a Java class that intercepts HTTP requests and responses in AEM — before they reach a servlet or component, and after the response is generated. It follows the standard `javax.servlet.Filter` contract.

**Use a filter when you need cross-cutting logic that applies to many requests:**
- Authentication and token validation
- Adding or modifying response headers
- Logging and request monitoring
- Response body modification (e.g. injecting scripts)
- Global error handling

---

### Old Way vs New Way

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

---

### The Five Filter Scopes

| Scope | When it fires |
|---|---|
| `REQUEST` | Every incoming HTTP request from a client. The most commonly used scope. |
| `INCLUDE` | When `RequestDispatcher.include()` is called — one component including another. |
| `FORWARD` | When `RequestDispatcher.forward()` is called — uncommon in AEM. |
| `ERROR` | When `sendError()` is called or an uncaught `Throwable` escapes the servlet. |
| `COMPONENT` | Legacy — fires across REQUEST, INCLUDE, and FORWARD. Avoid in new code. |

---

### Filter Narrowing Properties

Every property you add to `@SlingServletFilter` narrows the set of requests that trigger the filter. Only requests matching **all** specified conditions will call `doFilter()`.

| Property | What it restricts |
|---|---|
| `pattern` | Regex match on the request resource path. Omitting it fires on every request — almost never correct in production. |
| `extensions` | File extensions (e.g. `html`, `json`). A filter for `html` will not fire on `json` API calls. |
| `methods` | HTTP methods (GET, POST, HEAD). Only subscribe to what you need. |
| `resourceTypes` | `sling:resourceType` of the resolved resource. Targets a specific component type. |
| `selectors` | Sling selectors present in the URL. |

---

### Service Ranking — Filter Execution Order

A **more negative** value means **higher priority** (the filter runs earlier).

| Ranking Range | Recommended use |
|---|---|
| `-100` to `-500` | Authentication checks, security headers |
| `-500` to `-800` | Logging, monitoring |
| `-800` to `-1000` | Response modification, caching |

If two filters have the same ranking, execution order is not guaranteed. Always assign explicit rankings when order matters.

---

### The Filter Chain

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

---

### Response Wrapping — Modifying the Response Body

You cannot read or modify the response body after it has been written. The solution is to wrap the response *before* calling `chain.doFilter()`:

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

---

### Disabling a Filter at Runtime

Push an OSGi config that sets `sling.filter.scope` to an invalid value (e.g. `disabled`). Sling ignores filters with an unrecognised scope. The filter stops executing immediately — no redeployment needed.

---

### Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Forgetting `chain.doFilter()` in a conditional branch | Request is silently blocked for that condition | Trace every code path; ensure `chain.doFilter()` always executes when the request should proceed |
| Heavy work inside `doFilter()` | Blocks the request thread; every request on that path is slowed | Delegate to a Sling Job for any non-trivial work |
| No `pattern` or `methods` restriction | Filter fires on every request including internal Sling calls | Always scope as tightly as possible |
| Modifying headers after the response is committed | Headers are silently ignored | Set headers before calling `chain.doFilter()` or immediately after, while the response is still open |
| Hardcoding `UTF-8` in response wrapping | Encoding mismatch on non-UTF-8 responses | Always use `response.getCharacterEncoding()` |

---

### Filters vs Event Handlers vs Schedulers

| | Sling Filter | Event Handler / Listener | Scheduler |
|---|---|---|---|
| Execution thread | Request thread — synchronous | Background event thread | Background scheduler thread |
| Triggered by | HTTP request | JCR/OSGi event | Cron expression or interval |
| Must be fast? | Yes — directly impacts request latency | Yes — backs up event queue | Less critical — no user waiting |
| Right tool for | Inspecting / modifying HTTP requests & responses | Reacting to content changes or system events | Periodic background tasks |

---

## 9. Request Flow — Browser → CDN → Dispatcher → AEM

### Full Request Lifecycle

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

---

### Practical Implications

**Your filter only fires when the request actually reaches AEM.** If CDN or Dispatcher serves a cached response, your filter is never called for that request.

This means:
- You **cannot** use a Sling Filter to intercept every single user request.
- Logic that must run on every page view (analytics, personalisation) belongs in the browser via JavaScript.
- Logic that only needs to run when AEM renders a page (security headers, token validation, response modification) is correct in a Sling Filter.

---

### Security Headers and Caching — A Critical Note

Security headers added by a Sling Filter (e.g. `X-Frame-Options`, `Content-Security-Policy`) will only be present on responses that AEM renders directly. **Cached responses from Dispatcher or CDN will not carry those headers.**

**Recommended approach:** Configure security headers at the Dispatcher level using the Apache `mod_headers` directive. This ensures headers are present on all responses, including cached ones.

---

### Dispatcher Cache Invalidation and Filters

When an author publishes a page:

1. AEM sends a cache invalidation request to Dispatcher.
2. Dispatcher marks the cached file as stale.
3. The next request for that page misses the Dispatcher cache.
4. The request reaches AEM — **your filter fires**.
5. AEM renders the page fresh.
6. Dispatcher caches the new HTML.

Your filter therefore fires on the **first request after every publish event**, and then not again until the cache is invalidated next time.

---

## 10. AEM Workflows

### Engine Architecture

The AEM Workflow engine is built on the **Granite Workflow** framework and operates as a state machine. Each step transition is managed by the **Workflow Engine Service**.

Under the hood, workflow steps are executed as **Sling Jobs**:
- In clustered environments, Sling Job distribution can offload steps to different nodes.
- Every step completion triggers a JCR write that persists state. If an instance crashes, the workflow resumes from the last persisted checkpoint.

**JCR node locations:**

| Data | Path |
|---|---|
| Workflow model definition | `/conf` or `/etc/workflow/models` |
| Runtime instance data (history, state, actors) | `/var/workflow/instances` |

---

### State Management — The Three Metadata Maps

Understanding these three maps is fundamental to workflow development.

| Map | Scope | Lifetime | Primary Use |
|---|---|---|---|
| `args` (MetaDataMap) | Step-specific | Immutable — set in the Workflow Model editor | Reading static config values defined at design time |
| `item.getMetaDataMap()` | Step-specific runtime | Duration of the current step only | Short-lived data used within a single step's logic |
| `item.getWorkflowData().getMetaDataMap()` | Instance-wide | Entire workflow lifetime | **Passing data between steps** — the shared memory of the workflow |

---

### WorkItem vs MetaDataMap (args) — Detailed Comparison

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

---

### Execution Patterns

#### Transient Workflows

For high-volume automation where an audit trail is not required.

- Do **not** create nodes under `/var/workflow/instances`.
- The entire process runs in memory and is only committed to JCR at the final step.
- **Benefit:** Dramatically reduces JCR contention and prevents Oak index bloat during bulk asset imports.

#### Participant Steps and the AEM Inbox

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

---

### Advanced Routing

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

---

### Automation & Launchers

Launchers are the event listeners that bridge the JCR and the Workflow Engine — they start a workflow automatically when content changes.

- **Trigger events:** `NODE_CREATED`, `NODE_MODIFIED`, `NODE_REMOVED`
- **Path filtering:** Use glob patterns to target specific content trees.
- **Property conditions:** e.g. `jcr:content/metadata/dc:format == image/jpeg` to target only specific asset types.
- **Exclusion lists:** Always configure exclusions to prevent infinite loops (a workflow that modifies a property should not re-trigger itself).

---

### 6.5 vs AEM as a Cloud Service — The Asset Microservices Shift

| | AEM 6.5 | AEM as a Cloud Service |
|---|---|---|
| Asset processing | **DAM Update Asset** workflow — all binary processing in-JVM | **Asset Microservices** — external cloud services handle rendition generation |
| Custom workflows | Can manipulate binaries directly | Should run as post-processing workflows after microservices complete |
| Binary handling | Direct JVM manipulation | Avoid — use metadata triggers and external API calls instead |

---

### Performance: Throttling and Purging

**Throttled Task Runner (OSGi config):**
- Set **Max Parallel Jobs** to cap concurrent workflow steps.
- Set **Resource Limits** to stop spawning new threads when CPU or heap exceeds a threshold.

**Workflow Purging:**
- `/var/workflow/instances` grows indefinitely for non-transient workflows.
- Regular purging is mandatory for JCR performance.
- Target purge configurations by status (`COMPLETED`, `TERMINATED`, `ABORTED`) and age.

---

### Best Practices

- **Service User:** All automated process steps must use a restricted service user, not an admin session.
- **Single-purpose steps:** Break complex logic into multiple small steps — easier to debug, test, and reuse.
- **Event-driven external integration:** Use Adobe I/O events to offload waiting on external systems. Do not leave a workflow in `RUNNING` state waiting for a slow API — this holds threads and resources.
- **Transient workflows for bulk operations:** Always use transient workflows for high-volume asset processing to avoid JCR bloat.

---

## 11. JCR & Oak — Repository Fundamentals

### JCR Node Types You Must Know

| Node Type | Use |
|---|---|
| `cq:Page` | Every AEM page. Must have a `jcr:content` child node. |
| `cq:PageContent` | The `jcr:content` node of a page. Holds all page properties. |
| `dam:Asset` | Every asset in the DAM. Must have a `jcr:content` child. |
| `dam:AssetContent` | The `jcr:content` of an asset. |
| `nt:unstructured` | Untyped, free-form node. Used for component nodes and dialog data. |
| `nt:file` | Binary file node — requires a `jcr:content` child with `jcr:data`. |
| `sling:Folder` | Orderable folder node with resource resolution support. |
| `cq:Template` | Page template stored under `/conf` or `/libs`. |
| `rep:User` | JCR user node stored under `/home/users`. |
| `rep:Group` | JCR group node stored under `/home/groups`. |

### JCR Property Types

| Type | Java equivalent | Notes |
|---|---|---|
| `String` | `String` | Most common |
| `Long` | `Long` | Integer values |
| `Double` | `Double` | Decimal values |
| `Boolean` | `Boolean` | true/false |
| `Date` | `Calendar` | Always stored as ISO-8601 |
| `Binary` | `Binary` | For file data |
| `Path` | `String` | JCR path reference |
| `Reference` | `String` (UUID) | Hard reference — prevents deletion of the referenced node |
| `WeakReference` | `String` (UUID) | Soft reference — deletion of referenced node is allowed |
| `Name` | `String` | JCR qualified name |

### ValueMap — Reading Properties Safely

```java
// Never do: resource.adaptTo(Node.class).getProperty("title").getString()
// Always use ValueMap — null-safe, type-converting:

ValueMap vm = resource.getValueMap();
String title       = vm.get("jcr:title", "Default Title");   // with default
Boolean isHidden   = vm.get("hideInNav", false);
Calendar modified  = vm.get("jcr:lastModified", Calendar.class);  // returns null if absent
String[] tags      = vm.get("cq:tags", String[].class);           // multi-value
```

### Oak Index Types — Critical for Performance

| Index Type | Use case |
|---|---|
| `lucene` | Full-text search, complex property queries. The most powerful but heaviest. |
| `property` | Single or multiple property equality queries. Fast and lightweight. |
| `nodetype` | Filter by `jcr:primaryType` or `jcr:mixinTypes`. Always use in QueryBuilder. |

> **Interview trap:** "What happens when you run a QueryBuilder query without declaring `type=cq:Page`?" — Oak falls back to full repository traversal. This triggers the `Traversal Warning` in logs and is catastrophic on large repositories. Always declare `type` first — it leverages the `nodetype` index.

### Oak Traversal Warning

If you see this in logs, a query is doing a full repository scan:
```
*WARN* Traversal query with more than 100000 nodes: ...
```
**Fix:** Ensure your query uses indexed properties. Check the query explanation via `/system/console/jmx` → `QueryStat` → `Slow Queries`.

### Common Interview Questions — JCR & Oak

**Q: What is the difference between a `Reference` and a `WeakReference` property?**  
A `Reference` (hard reference) prevents the referenced node from being deleted — the JCR will throw a `ReferentialIntegrityException`. A `WeakReference` allows deletion of the referenced node; the property simply becomes a dangling reference.

**Q: What is the difference between `session.save()` and `resourceResolver.commit()`?**  
They both persist changes to JCR, but `session.save()` is the raw JCR API and `resourceResolver.commit()` is the Sling API. In OSGi components, always use `resourceResolver.commit()` — it ensures the Sling lifecycle is respected and works correctly with the Sling resource provider abstraction.

**Q: What is `jcr:lastModifiedBy` and when is it set?**  
It's a standard JCR mixin property (`mix:lastModified`) that records which user last modified the node. AEM sets it automatically when content is saved via the author UI. In code, if you write to JCR as a service user, `jcr:lastModifiedBy` reflects the service user name, not the human author.

**Q: What is the difference between `nt:unstructured` and `nt:base`?**  
`nt:base` is the root node type — every node type extends from it. `nt:unstructured` extends `nt:base` and adds the ability to have any properties and any child nodes without a schema constraint. AEM component dialog data is stored as `nt:unstructured`.

---

## 12. QueryBuilder & Search

### QueryBuilder vs JCR-SQL2 vs XPath

| | QueryBuilder | JCR-SQL2 | XPath |
|---|---|---|---|
| AEM-specific? | Yes — AEM API only | No — JCR standard | No — JCR standard |
| Syntax | Key-value pairs | SQL-like | XPath |
| Joins | No | Yes | Limited |
| Custom predicates | Yes | No | No |
| Pagination | Built-in | Manual `LIMIT`/`OFFSET` | Manual |
| Best for | AEM content queries | Complex joins, migration scripts | Legacy code |

### Essential QueryBuilder Predicates

```java
Map<String, String> params = new LinkedHashMap<>();

// ALWAYS start with type — uses Oak nodetype index
params.put("type",           "cq:Page");

// Scope
params.put("path",           "/content/mysite/en");
params.put("path.exact",     "false");      // search all descendants

// Property filter
params.put("1_property",     "jcr:content/cq:template");
params.put("1_property.value", "/conf/mysite/settings/wcm/templates/article");

// Full-text search
params.put("fulltext",       "AEM performance");
params.put("fulltext.relPath", "jcr:content");  // scope to page content only

// Date range
params.put("daterange.property",  "jcr:content/publishDate");
params.put("daterange.lowerBound", "2024-01-01T00:00:00.000+05:30");
params.put("daterange.upperBound", "2024-12-31T23:59:59.000+05:30");
params.put("daterange.lowerOperation", ">=");
params.put("daterange.upperOperation", "<=");

// Sorting
params.put("orderby",       "@jcr:content/publishDate");
params.put("orderby.sort",  "desc");

// Pagination — ALWAYS set these
params.put("p.limit",       "10");
params.put("p.offset",      "0");

// Performance — NEVER omit on large repos
params.put("p.guessTotal",  "100");  // estimate, avoids full count traversal

Query query = queryBuilder.createQuery(
    PredicateGroup.create(params),
    resourceResolver.adaptTo(Session.class)
);
SearchResult result = query.getResult();
```

### p.guessTotal — Why It's Critical

Without `p.guessTotal`, QueryBuilder traverses **every matching node** to compute an exact total count. On a repo with 100,000 pages, this means 100,000 node reads for every search request.

With `p.guessTotal=100` (or any estimate), QueryBuilder stops counting after that number and returns an approximation. Use `result.getHits()` for actual results and `result.getTotalMatches()` for the (estimated) count.

### Explaining a Query — Debug Tool

Test queries in the Felix console at `/system/console/jmx` → `QueryStat`, or use:
```
/bin/querybuilder.json?type=cq:Page&path=/content&p.limit=10
```

### Common Interview Questions — QueryBuilder

**Q: What is the difference between `p.limit=-1` and a specific limit?**  
`p.limit=-1` returns all results with no pagination. **Never use this in production.** On a large repository it loads every matching node into memory and will cause an OutOfMemoryError. Always use explicit pagination.

**Q: What does `path.exact=true` do?**  
It restricts results to the exact path specified, not its descendants. Equivalent to querying for that single node. Rarely useful in practice; the default (`false`) searches all descendants.

**Q: How do you build an OR query in QueryBuilder?**  
Use a `PredicateGroup` with `p.or=true`:
```java
PredicateGroup orGroup = new PredicateGroup();
orGroup.setAllRequired(false); // this makes it OR
orGroup.add(new Predicate("property").set("property", "jcr:content/category").set("value", "tech"));
orGroup.add(new Predicate("property").set("property", "jcr:content/category").set("value", "sport"));
```

**Q: How do you use QueryBuilder in a unit test?**  
Mock `QueryBuilder` and `SearchResult` with Mockito, or use the `ResourceResolverMock` from wcm.io test helpers which includes a basic in-memory query engine.

### AEM Search & Indexing: Advanced Interview Preparation Guide

This guide covers the core mechanics of AEM search, Apache Oak indexing, Lucene scoring, and the architectural principles behind faceted search. It bridges the gap between high-level concepts (layman's terms) and deep-dive technical architecture.

#### 1. Lucene Relevance Scoring (TF-IDF)

When AEM executes a full-text search, the underlying engine (Lucene) uses a mathematical formula to rank the results. The most common algorithm is **TF-IDF**.

##### The Core Concept

A document is considered highly relevant if the search term appears in it *frequently*, but only if that term is relatively *rare* across the entire AEM repository.

* **TF (Term Frequency):** How many times the search term appears in a specific document. The higher the frequency, the higher the score.

* **IDF (Inverse Document Frequency):** How rare the term is across *all* indexed documents. Common words ("the", "page") are penalized; rare words ("Omnichannel") are heavily rewarded.

##### Additional Scoring Factors

* **Field Length Normalization (lengthNorm):** Lucene penalizes long fields. A match in a short `jcr:title` scores higher than a match in a massive `jcr:description`.

* **Index-Time Boosts:** Developers can manually boost specific properties in the Oak index (e.g., making a match in `jcr:title` worth 4x more than a match in the body text).

* **Coordination Factor (coord):** Rewards documents that contain *multiple* terms from a multi-word search query.

**Interview Tip:** To debug these scores in AEM, use the **Query Builder Debugger** (`/libs/cq/search/content/querydebug.html`), execute a query, and check "Extract explain plan" to see the exact math Lucene applied.

#### 2. Faceted Search (The "Smart Filters")

##### The Layman's Explanation

A facet is a **smart filter**. Unlike a regular filter, a facet does two things:

1. It categorizes the search results.

2. It mathematically counts the data to show you exactly how many items match that category *before* you click it.

*Analogy:* Buying a laptop online. You search "Laptops". The sidebar doesn't just say "Brands". It says "Apple (1,500), Asus (500)". If you click Asus, the "Color" filter instantly updates to remove colors Asus doesn't make, preventing you from ever hitting a "0 results found" dead end.

##### The Technical Handshake

For facets to work in AEM, there must be a strict handshake between the query and the database index.

1. **The Request (Query Builder):** The frontend asks AEM for facets by passing the property and a master switch:
   1_property=jcr:content/author
   p.facets=true

2. **The Permission (Oak Index):** AEM will only calculate this if the underlying index is configured for it. In the Lucene index rules for the `author` property, a developer must set:

* `propertyIndex = true`

* `facets = true`

##### JSON Response Structure

A faceted query returns two main blocks of data:

* **`hits`:** The actual array of search results (the pages).

* **`facets`:** A dictionary object containing the categories and their exact counts. The frontend uses this object to render the sidebar UI.

#### 3. Under the Hood: Dynamic Recalculation & Performance

##### How Facets Calculate Dynamically

AEM does **not** pre-calculate facet counts, nor does it read actual JCR nodes during a query to count them. It uses a two-step process against in-memory data structures:

1. **The Match:** The user searches for "Marketing" and clicks the filter "Author: Jane". A new query fires. Lucene finds the subset of matching documents (e.g., 400 pages).

2. **The Tally:** Lucene takes those 400 internal Document IDs and cross-references them against an ultra-fast, in-memory structure called **DocValues** (a columnar storage map). It tallies the counts for other facets (like `cq:tags`) instantly based *only* on those 400 IDs.

##### Security and ACLs (The Performance Bottleneck)

AEM cannot show a count for a node a user isn't allowed to see.

* Checking ACL permissions for every single node in a 50,000-result search would crash the server.

* **The Fix (AEM 6.5):** Set `secure=statistical` on the facet index definition. Oak checks permissions for a random sample (e.g., 1,000 nodes) and applies that permission ratio to the total index count, providing a highly accurate estimate without the performance hit.

* **The Fix (AEM as a Cloud Service):** Search is offloaded entirely to Elasticsearch, which handles these aggregations natively off the AEM JVM.

#### 4. The Oak Query Planner

The Query Planner is AEM's "Auctioneer." It ensures queries execute efficiently by avoiding repository traversal.

##### The Cost-Based Model

1. **Parsing:** The planner breaks the query into core restrictions (e.g., Node Type = `cq:Page`, Property = `jcr:title`).

2. **The Bid:** The planner asks all indexes under `/oak:index` how much it would "cost" them to execute the query.

* *Cost = The mathematical estimate of how many nodes the index has to read.*

3. **The Award:** The index with the lowest cost wins the execution rights.

##### The Traversal Nightmare

If you query an unindexed property, no custom index can bid on it. The planner is forced to use a basic Node Type index (or the root path), which bids a massive cost (e.g., 100,000). AEM must now manually traverse every node to check the property, causing a `TraversalWarning` and severe performance degradation.

#### 5. Advanced Indexing: Memory and Cardinality

You can create a custom index covering multiple properties with `facets=true`. However, you must deeply understand how this impacts server RAM.

##### Columnar Storage (DocValues)

When you set `facets=true`, Lucene builds a separate, in-memory "spreadsheet" for that property mapping the `Document ID` to the `Property Value`.

##### The Cardinality Trap & Dictionary Encoding

To save space, Lucene doesn't store heavy text strings directly in the spreadsheet. It uses **Dictionary Encoding** (Global Ordinals). It splits the data into two parts:

1. **The Dictionary:** A list of unique text values.

2. **The Pointer Array:** The "spreadsheet" mapping Document IDs to the integer IDs of the Dictionary.

**Why Cardinality (Number of Unique Values) Matters:**

* **Low Cardinality (e.g., `status` - Draft, Published):** There are only 2 unique values. The dictionary in RAM holds just 2 text strings. The pointer array holds 100,000 tiny integers. This is extremely fast and lightweight.

* **High Cardinality (e.g., `jcr:title`):** If you have 100,000 pages, you have 100,000 unique titles. The dictionary is forced to load 100,000 unique, heavy text strings directly into the JVM heap.

**Interview Takeaway:** Setting `facets=true` on high-cardinality, free-text properties will bloat the JVM heap and eventually cause `OutOfMemoryError` (OOM) crashes in AEM 6.5. Facets should strictly be used for categorical, low-cardinality data.
---

## 13. Dispatcher

### What the Dispatcher Does

The Dispatcher is an Apache httpd module that acts as AEM's caching layer and load balancer. It sits between the CDN (or browser) and AEM publish instances.

Its two jobs:
1. **Cache** — serve static HTML from disk without hitting AEM for every request
2. **Load balance** — distribute requests across multiple AEM publish nodes

### What Dispatcher Caches

By default, Dispatcher caches GET and HEAD requests that return HTTP 200. It does **not** cache:
- Requests with query strings (unless explicitly configured)
- Requests for paths in the `/bin/` or `/system/` namespace
- Responses with `Set-Cookie` headers (unless configured to ignore them)
- POST requests

### Dispatcher Cache Invalidation

When an author activates (publishes) a page:

1. AEM's replication agent sends a `DELETE` (flush) request to Dispatcher
2. Dispatcher deletes the cached `.html` file for that page
3. It also deletes all `.stat` files up the directory tree
4. Next request for the page is a cache miss → AEM renders it → Dispatcher caches again

**Statfile level:** The `.stat` file mechanism means publishing one page can invalidate cached files in parent directories. Configure `statfileslevel` in `dispatcher.any` to control how far up the invalidation propagates.

### Key Dispatcher Configuration

```apache
# dispatcher.any — example farm config (key settings)
/cache {
    /docroot "/var/www/html"          # where HTML files are cached on disk

    /rules {
        /0001 { /type "allow" /glob "*.html" }   # cache HTML
        /0002 { /type "deny"  /glob "/bin/*" }   # never cache /bin
        /0003 { /type "deny"  /glob "/system/*" }
    }

    /statfileslevel "3"   # invalidate up to 3 directory levels

    /invalidate {
        /0001 { /type "allow" /glob "*" }  # allow all invalidation requests
    }
}
```

### Security Headers at Dispatcher Level

```apache
# httpd.conf or vhost config — via mod_headers
<IfModule mod_headers.c>
    Header always set X-Frame-Options "SAMEORIGIN"
    Header always set X-Content-Type-Options "nosniff"
    Header always set Content-Security-Policy "default-src 'self'"
    Header always set Strict-Transport-Security "max-age=31536000; includeSubDomains"
</IfModule>
```

Setting these at Dispatcher (not in a Sling Filter) ensures they appear on **all** responses, including cached ones.

### Common Interview Questions — Dispatcher

**Q: What is the difference between Dispatcher flush and cache invalidation?**  
They are the same thing. AEM's replication agent sends a flush request (an HTTP GET with `CQ-Action: Delete` headers) to the Dispatcher. Dispatcher deletes the file from its cache. "Flush agent" and "invalidation" both refer to this process.

**Q: Why would a page not be getting cached by Dispatcher?**  
Common causes: the page URL has a query string; the response has a `Set-Cookie` header (session cookies prevent caching); the path is on the Dispatcher deny list; the response status is not 200; the `Cache-Control: no-cache` header is set.

**Q: How do you force Dispatcher to cache a page with query parameters?**  
Use `/ignoreUrlParams` in `dispatcher.any` to list parameters that should be ignored for cache key computation. Parameters in this list are stripped from the cache key, so `page.html?utm_source=x` and `page.html` are treated as the same cache entry.

**Q: What is the Dispatcher TTL and how does it work?**  
By default, Dispatcher does not use TTL — it caches indefinitely until a flush request arrives. You can enable TTL-based expiry with `/enableTTL "1"` in `dispatcher.any`, which then respects the `Cache-Control: max-age` header from AEM.

**Q: How does Dispatcher handle author vs publish?**  
Dispatcher is only deployed in front of **publish**. The author environment does not use Dispatcher — authors must see live, un-cached content. Author instances are accessed directly (or via a reverse proxy with no caching).

---

## 14. AEM Security — Service Users & Permissions

### Why Service Users?

In code that runs in the background (schedulers, jobs, event handlers, workflow steps), you need a JCR session to read or write content. You should never use admin credentials for this. Service users are system JCR users with minimal, least-privilege permissions.

### Creating a Service User — Three Steps

**Step 1: Create the system user in the JCR via repoinit script**

```
# ui.config/src/main/content/jcr_root/apps/mysite/osgiconfig/config/
# org.apache.sling.jcr.repoinit.RepositoryInitializer-mysite.cfg.json
{
    "scripts": [
        "create service user mysite-scheduler-service with path system/mysite",
        "create service user mysite-workflow-service with path system/mysite",
        "set ACL for mysite-scheduler-service",
        "    allow jcr:read on /content/mysite",
        "end"
    ]
}
```

**Step 2: Map the subservice name to the system user**

```json
// org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended-mysite.cfg.json
{
    "user.mapping": [
        "com.mysite.core:scheduler=mysite-scheduler-service",
        "com.mysite.core:workflow-service=mysite-workflow-service"
    ]
}
```

**Step 3: Use in code**

```java
@Reference
private ResourceResolverFactory resolverFactory;

private ResourceResolver getServiceResolver(String subService) throws LoginException {
    Map<String, Object> params = new HashMap<>();
    params.put(ResourceResolverFactory.SUBSERVICE, subService);
    return resolverFactory.getServiceResourceResolver(params);
}

// Usage — always close in finally
ResourceResolver resolver = null;
try {
    resolver = getServiceResolver("scheduler");
    // do JCR work
    resolver.commit();
} catch (Exception e) {
    log.error("Error", e);
} finally {
    if (resolver != null && resolver.isLive()) {
        resolver.close();
    }
}
```

### ACL Permissions Quick Reference

| Permission | Meaning |
|---|---|
| `jcr:read` | Read node properties and children |
| `jcr:write` | Add/modify/remove nodes and properties (includes jcr:addChildNodes, jcr:modifyProperties, jcr:removeNode) |
| `jcr:addChildNodes` | Create child nodes |
| `jcr:modifyProperties` | Set/change properties |
| `jcr:removeNode` | Delete this node |
| `jcr:removeChildNodes` | Delete child nodes |
| `rep:write` | Shortcut for write + add + remove |
| `crx:replicate` | Required to trigger replication |

### Common Interview Questions — Security

**Q: Why should you never use `ResourceResolverFactory.SUBSERVICE` with an admin session in production?**  
Admin sessions have full JCR access. If exploited (e.g. through a path traversal vulnerability in your code), an attacker can read or write any node in the repository, including user credentials under `/home`. Service users with least-privilege ACLs limit the blast radius.

**Q: What is `loginAdministrative()` and why is it blocked?**  
`ResourceResolverFactory.loginAdministrative()` returns a session with full admin rights. Adobe blocked it by default in AEM 6.2+. Any bundle using it must be whitelisted in the `LoginAdminWhitelist` OSGi config — which is itself a security red flag that interviewers will probe.

**Q: What is the difference between a service user and a system user?**  
In AEM, "service user" typically refers to a JCR user created under `/home/users/system` with `createServiceUser` in repoinit. Both terms are often used interchangeably. The distinction is that service users are created by repoinit and are mapped via `ServiceUserMapper`; system users can also be created manually in CRXDE.

**Q: What is the repoinit language?**  
Repository Initialisation (repoinit) is a domain-specific language processed by the `SlingRepositoryInitializer` on AEM startup. It creates users, groups, paths, and ACLs in a declarative, idempotent way. It is the standard approach for all user/permission setup in both AEM 6.5 and AEMaaCS.

---

## 15. Sling Context-Aware Configuration (CAConfig)

### What Is CAConfig?

CAConfig allows you to store and retrieve configuration values that vary per site, per language branch, or per content tree — without writing code that hard-codes environment checks. The configuration is stored in JCR under `/conf` and is resolved by walking up the content tree from the current resource.

### Resolution Order

Given a resource at `/content/mysite/en/about`:
1. Looks for config at `/conf/mysite` (mapped via `sling:configRef` on the page tree root)
2. Falls back to `/conf/global`
3. Falls back to defaults in the `@Configuration` annotation

### Defining a Configuration

```java
@Configuration(label = "Site Header Configuration")
public @interface HeaderConfig {
    String logoPath() default "";
    boolean enableSiteSearch() default true;
}

// Collection config — for repeating items (nav links, etc.)
@Configuration(label = "Header Nav Items", collection = true)
public @interface HeaderNavItemsConfig {
    String pageName() default "";
    String pagePath() default "";
}
```

### Reading CAConfig in a Sling Model

```java
@Model(adaptables = SlingHttpServletRequest.class)
public class HeaderModel {

    @Self
    private SlingHttpServletRequest request;

    private String logoPath;
    private List<HeaderNavItemsConfig> navItems;

    @PostConstruct
    protected void init() {
        ConfigurationBuilder cb = request.adaptTo(ConfigurationBuilder.class);
        if (cb != null) {
            HeaderConfig config = cb.as(HeaderConfig.class);
            logoPath = config.logoPath();

            navItems = cb.asCollection(HeaderNavItemsConfig.class)
                         .stream()
                         .collect(Collectors.toList());
        }
    }
}
```

### Storing a CAConfig value in JCR

CAConfig values are stored as JCR nodes under the mapped `/conf` path:

```
/conf/mysite/sling:configs/com.mysite.core.configs.HeaderConfig
    logoPath = "/content/dam/mysite/logo.svg"
    enableSiteSearch = true
```

Set via the `ConfigurationManager` API or directly in CRXDE.

### Common Interview Questions — CAConfig

**Q: How is CAConfig different from OSGi config?**  
OSGi config is per-environment (dev/stage/prod) and is set by operators. CAConfig is per-site/content-tree and can be set by developers or even authors. OSGi config is the right tool for environment-specific values (API keys, URLs). CAConfig is the right tool for site-specific design decisions (logo path, navigation structure, feature flags per locale).

**Q: What is `sling:configRef`?**  
A property set on a content root node (e.g. the language root `/content/mysite/en`) that points to a `/conf` path. It tells CAConfig's resolver which `/conf` bucket to use when resolving configuration for that content tree.

**Q: What happens if no CAConfig is found?**  
`ConfigurationBuilder.as(HeaderConfig.class)` never returns null — it returns a proxy object whose methods return the default values defined in the `@Configuration @interface`. This makes CAConfig null-safe by design.

---

## 16. AEM as a Cloud Service — Key Differences from 6.5

### Architecture Changes

| Aspect | AEM 6.5 | AEMaaCS |
|---|---|---|
| Deployment | On-premise / AMS (managed) | Cloud-native on Adobe I/O (Kubernetes) |
| Repository | TarMK or MongoMK | Oak Segment TAR on Azure Blob / AWS S3 |
| Scaling | Manual / semi-auto | Auto-scaling — new pods spin up in minutes |
| Upgrades | Major version upgrades every 2–3 years | Continuous delivery — updated weekly |
| Dispatcher | Apache httpd on a managed VM | CDN-integrated (Adobe CDN / Fastly) |
| Asset processing | DAM Update Asset workflow | Asset Microservices (cloud functions) |

### Mutable vs Immutable Content

A fundamental AEMaaCS constraint: the `/apps`, `/libs`, `/conf` tree is **immutable** at runtime. You cannot write to it via code. Only `/content`, `/conf` (authored), and `/var` are mutable.

| Path | Mutable? | Notes |
|---|---|---|
| `/apps` | **No** | Deployed via Cloud Manager pipeline only |
| `/libs` | **No** | Adobe-managed; never modify directly |
| `/content` | Yes | Authored content |
| `/conf` | Yes (authored part) | CAConfig, editable templates |
| `/var` | Yes | Jobs, workflows, indexes (runtime data) |
| `/home` | Yes | Users and groups |

### Things That Are Banned in AEMaaCS

| Pattern | Why banned | Alternative |
|---|---|---|
| `ResourceResolverFactory.loginAdministrative()` | Security | Service users via `getServiceResourceResolver()` |
| Writing to `/apps` or `/libs` at runtime | Immutable repo | Deploy via Cloud Manager |
| Custom Lucene index configurations that block reindexing | Performance | Use property indexes where possible |
| Mutable state in OSGi bundle classloader | Pod restarts lose it | Use JCR or external storage |
| Direct binary manipulation in workflows | Replaced by Asset Microservices | Use metadata triggers |

### Cloud Manager Pipeline

Deployments follow: Code Build → Unit Tests → Code Quality (SonarQube) → Functional Tests → Staging Deploy → Production Deploy. There is no manual FTP or package installation in production.

### AEMaaCS — Key APIs and Patterns

**Asset Compute SDK:** For custom rendition generation (replaces DAM workflow binary processing).

**Adobe I/O Events:** For event-driven integrations with external systems. Fire events from AEM, consume in external services — no long-running workflow instances waiting for API responses.

**Content Transfer Tool (CTT):** For migrating content from AEM 6.5 to AEMaaCS.

**Repository Modernization Tool:** Converts mutable packages to the immutable structure required by AEMaaCS.

### Common Interview Questions — AEMaaCS

**Q: What is the biggest architectural difference between AEM 6.5 and AEMaaCS?**  
Immutable repository structure. In 6.5 you could write anything anywhere at runtime. In AEMaaCS, `/apps` and `/libs` are read-only at runtime — all code must be deployed via the Cloud Manager pipeline. This fundamentally changes how you think about hotfixes, content migrations, and runtime configuration.

**Q: How do you debug an issue in AEMaaCS when you can't SSH into the server?**  
Use the **Developer Console** in Adobe Cloud Manager — it provides real-time log tailing, OSGI bundle status, Sling resource resolution tools, and JVM heap dumps. You can also use the `aio` CLI for log streaming.

**Q: Can you use `session.save()` in AEMaaCS?**  
Yes, it is not banned, but `resourceResolver.commit()` is preferred as it works with the Sling abstraction layer. Both work in AEMaaCS.

**Q: What is the RDE (Rapid Development Environment) in AEMaaCS?**  
RDE is a fast-feedback cloud environment where you can deploy individual bundles, content packages, or Dispatcher configs without a full Cloud Manager pipeline run. It is used for iterative development and debugging.

---

## 17. Unit Testing in AEM

### Testing Stack

| Library | Purpose |
|---|---|
| `io.wcm.testing.aem-mock-junit5` | Core AEM mock framework — provides `AemContext`, mock JCR, mock Sling |
| `org.mockito:mockito-core` | Mocking OSGi services and external dependencies |
| `org.junit.jupiter:junit-jupiter` | JUnit 5 test runner |
| `org.apache.sling:org.apache.sling.testing.sling-mock` | Underlying Sling mock |

### Testing a Sling Model

```java
@ExtendWith(AemContextExtension.class)
class AuthorImplTest {

    private final AemContext ctx = new AemContext(ResourceResolverType.JCR_MOCK);

    @BeforeEach
    void setUp() {
        ctx.addModelsForClasses(AuthorImpl.class);
        // Load test content from a JSON fixture
        ctx.load().json("/content/test-author.json", "/content/author");
        ctx.currentResource("/content/author");
    }

    @Test
    void testGetFirstName() {
        Author model = ctx.request().adaptTo(Author.class);
        assertNotNull(model, "Model should not be null");
        assertEquals("Sibi", model.getFirstName());
    }

    @Test
    void testDefaultValues() {
        // Resource with no properties set — test defaults
        ctx.create().resource("/content/empty", new HashMap<>());
        ctx.currentResource("/content/empty");
        Author model = ctx.request().adaptTo(Author.class);
        assertEquals("Sibi", model.getFirstName()); // default value
    }
}
```

### Test JSON Fixture (`/content/test-author.json`)

```json
{
  "jcr:primaryType": "nt:unstructured",
  "firstName": "Sibi",
  "lastName": "Sarvanan",
  "gender": "Male",
  "email": "sibi@example.com",
  "author:title": "Mr"
}
```

### Testing an OSGi Service with Mockito

```java
@ExtendWith(MockitoExtension.class)
class ExternalApiServiceImplTest {

    @Mock
    private CloseableHttpClient httpClient;

    @InjectMocks
    private ExternalApiServiceImpl service;

    @Test
    void testFetchProductData_success() throws Exception {
        // Arrange
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        HttpEntity entity = new StringEntity("{\"sku\":\"ABC123\"}");

        when(statusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getStatusLine()).thenReturn(statusLine);
        when(mockResponse.getEntity()).thenReturn(entity);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);

        // Act
        String result = service.fetchProductData("ABC123");

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("ABC123"));
    }
}
```

### Testing a Scheduler

```java
@Test
void testSchedulerRun() {
    // Schedulers are simple Runnables — just call run() directly
    SimpleScheduledTask task = new SimpleScheduledTask();
    // inject mocks via reflection or constructor
    assertDoesNotThrow(task::run);
}
```

### Common Interview Questions — Testing

**Q: What is AemContext and what does it provide?**  
`AemContext` is the central test fixture from wcm.io's aem-mock library. It provides a mock JCR repository, mock `ResourceResolver`, mock `SlingHttpServletRequest` and `Response`, a model factory, and helper methods to create resources and load JSON fixtures — all in-memory with no actual AEM instance needed.

**Q: What is the difference between `ResourceResolverType.JCR_MOCK` and `ResourceResolverType.JCR_OAK`?**  
`JCR_MOCK` is a lightweight in-memory mock — fast but doesn't support JCR queries. `JCR_OAK` spins up a real in-memory Oak repository — slower but supports full JCR query execution. Use `JCR_MOCK` for model and service tests; use `JCR_OAK` when you need to test QueryBuilder logic.

**Q: How do you test a component that uses `@OSGiService`?**  
Register a mock implementation with `ctx.registerService(MyService.class, mockImpl)` before calling `ctx.request().adaptTo(MyModel.class)`. The AemContext will inject the registered mock.

---

## 18. AEM Component Development & HTL

### HTL (Sightly) Essentials

HTL is AEM's server-side templating language. It replaces JSP and enforces XSS-safe output by default.

```html
<!-- Use statement — adapts a Sling Model -->
<sly data-sly-use.model="com.mysite.core.models.Author">
    <div class="author">
        <!-- Output is XSS-escaped automatically -->
        <h1>${model.firstName} ${model.lastName}</h1>

        <!-- Explicit context for HTML output (not escaped) -->
        <div>${model.richTextBody @ context='html'}</div>

        <!-- Attribute context -->
        <a href="${model.email @ context='uri'}">Email</a>

        <!-- Test / conditional -->
        <p data-sly-test="${model.featured}">Featured Author</p>

        <!-- List iteration -->
        <ul data-sly-list.item="${model.tags}">
            <li>${item}</li>
        </ul>

        <!-- Include another component -->
        <sly data-sly-include="header.html"/>

        <!-- Resource inclusion -->
        <sly data-sly-resource="${'footer' @ resourceType='mysite/components/footer'}"/>
    </div>
</sly>
```

### HTL Context Options (XSS)

| Context | Use for |
|---|---|
| (default) | HTML text — escapes `<`, `>`, `&`, `"` |
| `html` | Trust the value as raw HTML (only safe content) |
| `uri` | URL attribute values — encodes unsafe URL characters |
| `scriptString` | Inside JavaScript string literals |
| `styleContext` | CSS property values |
| `attribute` | HTML attribute name (dynamic attributes) |
| `text` | Explicit text context (same as default) |
| `unsafe` | No escaping at all — **never use for user input** |

### HTL Rules to Remember

| Rule                | Wrong                         | Right                                           |
|---------------------|-----------------------------|-------------------------------------------------|
| Arithmetic          | `${model.page + 1}`         | Add getter to model, use `${model.displayPage}` |
| Boolean attribute | `${x ? 'selected' : ''}`      | `data-sly-attribute.selected="${x}"` |
| String comparison   | `${'literal' == model.val}` | `${model.val == 'literal'}` |
| List contains check | Works as-is                 | `${model.list contains 'value'}` |
| XSS in href         | `href="${model.path}"`      | `href="${model.path @ context='uri'}"` |
| XSS in attribute    | `value="${model.val}"`      | `value="${model.val @ context='attribute'}"` |

### Component Dialog — Key Touch UI Resource Types

| Resource Type | Purpose |
|---|---|
| `granite/ui/components/coral/foundation/form/textfield` | Single-line text input |
| `granite/ui/components/coral/foundation/form/textarea` | Multi-line text |
| `granite/ui/components/coral/foundation/form/checkbox` | Boolean toggle |
| `granite/ui/components/coral/foundation/form/select` | Dropdown |
| `granite/ui/components/coral/foundation/form/pathfield` | Path picker (page/asset) |
| `granite/ui/components/coral/foundation/form/multifield` | Repeating field group |
| `granite/ui/components/coral/foundation/form/numberfield` | Numeric input |
| `cq/gui/components/authoring/dialog/richtext` | Rich text editor |
| `granite/ui/components/coral/foundation/include` | Include another dialog fragment |

### Editable Templates vs Static Templates

| | Static Templates | Editable Templates |
|---|---|---|
| Stored in | `/apps/mysite/templates/` | `/conf/mysite/settings/wcm/templates/` |
| Modifiable by authors? | No — developer only | Yes — template authors via Template Editor |
| Component allowed list | `allowedChildren` property on template | Configured per-container in Template Editor |
| Layout mode | N/A | Authors can set column widths in Layout Mode |
| Recommended? | Legacy — avoid | Yes — standard in all modern AEM projects |

### Common Interview Questions — Components & HTL

**Q: How do you prevent XSS in HTL?**  
HTL escapes output by default using HTML context. For URLs use `@ context='uri'`, for HTML use `@ context='html'` only with trusted content. Never use `@ context='unsafe'` with user-supplied data.

**Q: What is the difference between `data-sly-include` and `data-sly-resource`?**  
`data-sly-include` renders another HTL script in the same component context — the included script shares the same model and bindings. `data-sly-resource` renders a JCR resource (or virtual resource) as a new component — it starts a new Sling request dispatch cycle with its own model.

**Q: How do you make a component inherit from a Core Component?**  
Set `sling:resourceSuperType` on your component node to the Core Component path (e.g. `core/wcm/components/text/v2/text`). Your component inherits all HTL scripts and dialog fields, and you only override what you need.

**Q: What is a policy in editable templates?**  
A policy is a reusable set of component design settings stored in `/conf`. Multiple template pages can reference the same policy. For example, a "default text" policy might restrict the RTE toolbar to bold/italic/link only. Authors cannot override policy settings — they are design-time constraints.

---

## 19. TagManager & Taxonomy

### Core APIs

```java
@Reference
private TagManager tagManager; // com.day.cq.tagging.TagManager

// Or adapt from ResourceResolver:
TagManager tm = resourceResolver.adaptTo(TagManager.class);

// Resolve a tag by ID
Tag tag = tm.resolve("mysite:topic/aem-development");

// Get localised title in a specific locale
String title = tag.getTitle(Locale.ENGLISH);  // "AEM Development"
String localTitle = tag.getLocalizedTitle(Locale.FRENCH); // "Développement AEM"

// Create a tag
Tag newTag = tm.createTag(
    "mysite:events/conference-2025",
    "Conference 2025",
    "Annual AEM conference",
    true  // autoSave
);

// Get all tags on a resource (stored as cq:tags String[])
Tag[] tags = tm.getTagsForSubtree(resource, false);

// Find all tagged pages
RangeIterator<Resource> results = tm.find("/content/mysite", new String[]{"mysite:topic/aem"});
```

### Tag ID Structure

Tags follow a namespace:path structure: `namespace:category/subcategory`

Example: `mysite:topic/performance` means:
- Namespace: `mysite` (stored under `/content/cq:tags/mysite/`)
- Category: `topic`
- Tag: `performance`

### Common Interview Questions — Tags

**Q: How are tags stored on a page?**  
As a `String[]` property `cq:tags` on the `jcr:content` node. Each value is a tag ID string (e.g. `mysite:topic/aem`).

**Q: How do you get a human-readable tag title from a tag ID in a Sling Model?**  
```java
TagManager tm = resourceResolver.adaptTo(TagManager.class);
Tag tag = tm.resolve("mysite:topic/aem");
String title = tag != null ? tag.getTitle(request.getLocale()) : "mysite:topic/aem";
```

**Q: What is the difference between `getTitle()` and `getLocalizedTitle()`?**  
`getTitle(Locale)` returns the title in the given locale, falling back to the default title if no localisation is found. `getLocalizedTitle(Locale)` returns only the localised title or null — no fallback. Use `getTitle(Locale)` in production to avoid null titles.

---

## 20. Replication API

### Programmatic Page Activation

```java
@Reference
private Replicator replicator;

// Activate (publish) a page
public void publishPage(String pagePath, Session session) {
    try {
        replicator.replicate(session, ReplicationActionType.ACTIVATE, pagePath);
        log.info("Published: {}", pagePath);
    } catch (ReplicationException e) {
        log.error("Failed to publish {}: {}", pagePath, e.getMessage(), e);
    }
}

// Deactivate (unpublish) a page
public void unpublishPage(String pagePath, Session session) throws ReplicationException {
    replicator.replicate(session, ReplicationActionType.DEACTIVATE, pagePath);
}

// Delete from publish (when a page is deleted on author)
public void deleteFromPublish(String pagePath, Session session) throws ReplicationException {
    replicator.replicate(session, ReplicationActionType.DELETE, pagePath);
}
```

### Replication Action Types

| Type | What it does |
|---|---|
| `ACTIVATE` | Publish the content to publish instances |
| `DEACTIVATE` | Unpublish — removes the content from publish |
| `DELETE` | Deletes the page from publish instances (used when author page is deleted) |
| `TEST` | Sends a test ping to the replication agent |
| `REVERSE` | Pulls content from publish back to author (Reverse Replication) |

### ReplicationOptions — Batch Replication

```java
ReplicationOptions options = new ReplicationOptions();
options.setSynchronous(false);    // async — don't block the calling thread
options.setSuppressVersions(true); // don't create versions during replication
options.setFilter(agent -> agent.getId().equals("publish")); // target specific agent

replicator.replicate(session, ReplicationActionType.ACTIVATE, pagePath, options);
```

### Common Interview Questions — Replication

**Q: What is the difference between synchronous and asynchronous replication?**  
Synchronous replication blocks the calling thread until the publish instance confirms receipt. Asynchronous adds the replication action to a queue and returns immediately — the actual transfer happens in the background. For bulk activation (e.g. activating 1000 pages from a workflow), always use async to avoid thread exhaustion.

**Q: What is Reverse Replication?**  
The REVERSE action pulls content from publish back to author. Used for user-generated content (forms submissions, ratings) where content is created on publish and needs to be stored on author. Rarely used in modern AEM projects — most UGC is stored in external databases.

**Q: What permission does a service user need to trigger replication?**  
The service user must have `crx:replicate` permission on the content being replicated, plus `jcr:read`. Without `crx:replicate`, the `replicator.replicate()` call throws a `ReplicationException` with a permission error.

**Q: What is a replication agent and where is it configured?**  
A replication agent is an OSGi-managed transport queue stored at `/etc/replication/agents.author/`. The default agent (`publish`) points to the publish instance URL. Flush agents (for Dispatcher cache invalidation) are stored at `/etc/replication/agents.author/flush`.

---

## 21. Common Interview Questions — Senior Level

This section covers questions that appear frequently at the senior/lead AEM developer level, spanning multiple topics.

---

### Architecture & Design

**Q: How would you design a multi-site, multi-language AEM project?**

Key decisions:
- **Live Copy (MSM)** for sites that share content but need market-specific variations. The blueprint is the master; live copies inherit and can override.
- **Language Copy** for translation workflows. Create language roots under each country site (e.g. `/content/mysite/us/en`, `/content/mysite/fr/fr`).
- **CAConfig** per site root for site-specific configuration (logo, nav, features).
- **Shared component library** under a single app node (`/apps/mysite-components`) referenced by all sites.
- Single `ui.apps` package — avoid duplicate components per site.

**Q: How would you handle a situation where a page takes 10 seconds to render?**

Systematic approach:
1. Check AEM's **Request Performance Log** (`/system/console/requests`) for slow requests.
2. Enable **Developer Mode** in AEM to see per-component render times in the page source.
3. Profile with **YourKit** or heap dumps from `/system/console/jmx`.
4. Common culprits: QueryBuilder queries without `p.guessTotal` doing full traversal; N+1 queries in a list component; ResourceResolver not being reused; HTTP calls to external APIs on the render thread.
5. Fix: Add `p.guessTotal`, cache expensive computations in `@PostConstruct`, move API calls to Sling Jobs, use `SlingScheduler` to pre-warm caches.

**Q: What is the MSM (Multi-Site Manager) and how does it work?**

MSM lets you create Live Copies of a blueprint page tree. Rollout Configs define which properties and children are synced from blueprint to live copy. Live copy pages can have local overrides (breakpoints) that are preserved during rollout. Common rollout triggers: manual rollout, publish, page creation.

---

### Performance

**Q: How do you find slow queries in AEM?**

1. `/system/console/jmx` → `QueryStat` → `Slow Queries` — lists queries sorted by execution time.
2. Enable Oak query logging: set `org.apache.jackrabbit.oak.query` to DEBUG.
3. Use `/bin/querybuilder.json` with `p.debugTo` to get the SQL2 translation and then use `EXPLAIN SELECT ...` via the Oak JMX bean.

**Q: What causes `javax.jcr.query.InvalidQueryException: Traversal query`?**  
A query touches more than 100,000 nodes without using an index. The fix is always to add an indexed predicate — usually `type=cq:Page` + a property index on the filtered property.

**Q: How do you cache data in AEM at the Java level?**

Options in order of preference:
- **Guava Cache / Caffeine** — in-memory cache in the OSGi service with TTL. Fast, no persistence.
- **JCR node** — store computed data as a JCR property, retrieve in `@PostConstruct`. Persists across restarts. Good for expensive data that rarely changes.
- **Servlet response caching** — add `Cache-Control` headers and let Dispatcher cache the JSON output.
- **Sling Scheduler pre-warming** — a scheduler runs periodically, pre-computes data, stores in a field or JCR node.

---

### Debugging

**Q: A component works in author but not in publish. How do you debug?**

1. Check that the content has been **activated** (published) to the publish instance.
2. Check that the **OSGi bundle** is `Active` on publish — `/system/console/bundles` on the publish host.
3. Check that the **OSGi config** exists on publish — configs in `config.publish/` are publish-only; `config/` applies to all run modes.
4. Check **error.log** on publish for exceptions.
5. Check **Dispatcher allow/deny rules** — the request may be blocked before reaching AEM publish.
6. Check **user permissions** — publish uses anonymous or a specific user; author uses the logged-in CMS user.

**Q: How do you debug an OSGi component that isn't activating?**

1. `/system/console/bundles` — check if the bundle is `Active`. If `Resolved`, it has unmet package imports.
2. `/system/console/components` — find your component. If `Unsatisfied`, a `@Reference` is not satisfied (the required service is missing or not active).
3. Check `error.log` for `Cannot satisfy reference` messages.
4. Verify the required service is itself active.

**Q: What is CRXDE Lite and when should you NOT use it in production?**  
CRXDE Lite is a browser-based JCR repository browser and editor at `/crx/de`. Never use it to make persistent changes in production — changes made in CRXDE are not version-controlled, not repeatable, and will be overwritten by the next code deployment. Use it only for debugging and reading node properties. All changes must go through a deployable package or repoinit script.

---

### Data Migration & Content Operations

**Q: How do you bulk-update 50,000 JCR nodes efficiently?**

```java
// Use QueryBuilder to find nodes, then batch-process with periodic saves
int batchSize = 500;
int count = 0;

SearchResult result = query.getResult();
for (Hit hit : result.getHits()) {
    Resource resource = hit.getResource();
    ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);
    mvm.put("myProperty", "newValue");
    count++;

    if (count % batchSize == 0) {
        resourceResolver.commit(); // commit every 500 nodes to avoid OutOfMemory
        log.info("Committed {} nodes", count);
    }
}
resourceResolver.commit(); // final commit for remaining nodes
```

Key points:
- Always commit in batches — never hold all changes in the JCR session at once.
- Use a service user — never admin.
- Run as a Sling Job, not a scheduler, so it can be monitored and retried.
- Add `Thread.sleep(10)` between batches in production to avoid saturating the JCR write queue.

**Q: How do you create a page programmatically?**

```java
PageManager pm = resourceResolver.adaptTo(PageManager.class);
Page newPage = pm.create(
    "/content/mysite/en",       // parent path
    "my-new-page",              // page name (URL segment)
    "/conf/mysite/settings/wcm/templates/article",  // template path
    "My New Page Title",        // title
    true                        // auto-rename if name collision
);
resourceResolver.commit();
```

---

### Common Gotchas (High Interview Value)

**1. ResourceResolver leak**  
The most common production issue. Every `ResourceResolver` opened must be closed in a `finally` block. Not closing it leaks a JCR session — eventually the session pool exhausts and AEM stops responding.

**2. Sling Model returning null**  
`adaptTo()` returns `null` if adaptation fails. If `DefaultInjectionStrategy.REQUIRED` is used and any required field is missing, adaptation fails silently. Always null-check `adaptTo()` results and use `OPTIONAL` unless you truly require a field.

**3. QueryBuilder session leak**  
`query.getResult()` internally holds a JCR session. If you forget to close it (via `result.getHits()` iteration completing or calling `result.close()`), you leak a session. Use try-with-resources or call `((CloseableQuery)query).close()`.

**4. Thread safety in OSGi services**  
OSGi services are singletons. Any instance field in an OSGi service is shared across all threads. Never store request-specific data in instance fields. Use local variables or `ThreadLocal` for per-request state.

**5. Accessing JCR in a filter on every request**  
A Sling Filter that opens a `ResourceResolver` on every request will create and close a JCR session for every HTTP hit. Under load, this exhausts the session pool. Cache the data in a service-level field, pre-warmed by a scheduler.

**6. Missing `@Modified` handler**  
If a component reads config in `@Activate` but has no `@Modified`, changing the config in the Felix console triggers `@Activate` again — which may not clean up previous state (e.g. re-registers a scheduler without unregistering the old one). Always implement `@Modified` or ensure `@Activate` is idempotent.

**7. `context='html'` in HTL with user content**  
Using `@ context='html'` on a value that came from user input is an XSS vulnerability. Only use `html` context for values you control (e.g. from a trusted RTE field stored in JCR via the AEM author UI, which itself sanitizes input).

---

## 22. Product Catalog Scenario — Child Resource, Custom Serializer, Replication & Notification

These four patterns were identified as gaps in a code review and added together via one connected scenario: an e-commerce product page with auto-publish and an approval workflow.

### @ChildResource Pattern

```java
@ChildResource(name = "variants")
private List<Resource> variantResources;   // multifield child nodes

@PostConstruct
protected void init() {
    variants = variantResources.stream()
            .map(r -> r.adaptTo(ProductVariant.class))  // child model, adaptables=Resource.class
            .filter(Objects::nonNull)                    // adaptTo() can return null
            .collect(Collectors.toList());
}
```

**Interview Q: Why `adaptables = Resource.class` on the child model, not `SlingHttpServletRequest`?**  
Child nodes inside a multifield are not individually associated with an HTTP request — only the parent component resource is. There's no request context to adapt from at the child level.

### Custom Jackson Serializer Pattern

```java
@JsonSerialize(using = ProductVariantListSerializer.class)
private final List<ProductVariant> variants;
```

Lets you: rename fields in JSON output independent of getter names, exclude internal/derived fields from the API contract, and conditionally omit list entries based on computed state — none of which `@JsonIgnore`/`@JsonProperty` can do alone.

**Interview Q: When is a custom serializer required vs. simple annotations?**  
When the inclusion/exclusion or shape of the output depends on *computed* logic rather than a field simply being present or absent.

### Programmatic Replication Trigger (Event Listener)

```java
ReplicationOptions options = new ReplicationOptions();
options.setSynchronous(false);   // critical — never block the event thread
replicator.replicate(session, ReplicationActionType.ACTIVATE, pagePath, options);
```

**Interview Q: Why async, and what session do you use?**  
A `ResourceChangeListener` runs on a shared event thread; a synchronous replicate() call would block it on a network round-trip to the publish instance, backing up the whole observation queue. Use a service-user-derived JCR `Session` — never admin — obtained via `ResourceResolverFactory.getServiceResourceResolver()`.

### Workflow Replication + Notification Two-Step Pattern

```java
// Step 1: ProductPublishProcess — publish and record outcome
replicator.replicate(session, ReplicationActionType.ACTIVATE, payloadPath);
workItem.getWorkflowData().getMetaDataMap().put("publishStatus", "SUCCESS");

// Step 2: ApprovalNotificationProcess — read outcome, notify
String status = workItem.getWorkflowData().getMetaDataMap().get("publishStatus", "UNKNOWN");
MessageGateway<HtmlEmail> gateway = messageGatewayService.getGateway(HtmlEmail.class);
gateway.send(email);
```

**Interview Q: What is the correct AEM API for sending email, and what's a common wrong answer?**  
Correct: `com.day.cq.mailer.MessageGatewayService.getGateway(HtmlEmail.class).send(email)`. A common mistake is reaching for `org.apache.sling.commons.mail.MailService` with a `sendEmail(Email, String[])` signature — this is not the standard AEM mail API and won't compile against real AEM dependencies.

**Interview Q: Should a failed notification email fail the whole workflow?**  
No. If the actual business action (publishing) already succeeded, a notification failure is a secondary concern — log it and continue. Throwing `WorkflowException` here would incorrectly mark a successfully-published page as a failed workflow instance.

### Concepts Clarified Alongside This Scenario

| Concept | One-line definition |
|---|---|
| Serialization | Converting an in-memory object into another format (JSON/bytes/XML) for storage or transmission — `Serializable`/`serialVersionUID` is the unrelated binary-object variant used by `HttpServlet`. |
| Connection pooling | Reusing already-open TCP connections instead of paying handshake cost per request; `PoolingHttpClientConnectionManager` caps total and per-route connections. |
| `ConcurrentHashMap` | Thread-safe map allowing concurrent reads/writes without external `synchronized` blocks — required whenever a singleton OSGi service's field is touched by multiple concurrent request threads. |
| Circular reference (OSGi) | Two components each holding a runtime `@Reference` to a service the other provides, forming a dependency loop. A nested `@interface Config` inside a component is metadata read at build/activation time — it is not a service reference and cannot cause this. |

---

## 23. REST APIs & GraphQL in AEM

### REST — Multiple Layers, Not One API

| Layer | How it works | Code required? |
|---|---|---|
| Sling Default GET (`.json`, `.1.json`, `.infinity.json`, `.tidy.json`) | Any JCR resource is automatically renderable as JSON via extension/selector | None |
| Sling Model Exporter (`.model.json`) | `@Exporter(name="jackson", extensions="json")` on a Sling Model | Annotation only |
| Custom Sling Servlet | `@SlingServletResourceTypes` + `extensions="json"` | Full servlet |
| Sling POST Servlet | Writes to JCR via form params + `:operation` (create/copy/move/delete/checkin/checkout) | None — built in |
| QueryBuilder JSON | `/bin/querybuilder.json?...` | None — usually wrapped in a custom servlet for security |

```bash
# Sling POST Servlet — write to JCR with zero custom code
curl -u admin:admin -F"jcr:primaryType=nt:unstructured" -F"title=Hello" \
     http://localhost:4502/content/mysite/en/newnode

curl -u admin:admin -F":operation=delete" http://localhost:4502/content/mysite/en/oldnode
```

```java
// Sling Model Exporter — the most common "build a REST API" pattern
@Model(adaptables = Resource.class, adapters = Product.class, resourceType = "...")
@Exporter(name = "jackson", extensions = "json", selector = "model")
public class ProductImpl implements Product { ... }
// GET /content/.../products/shirt.model.json
```

**Key intricacies:**
- Selector + extension combo drives Sling's servlet resolution chain — `.model.json` and `.json` hit completely different code paths.
- `.infinity.json` can leak internal/ACL nodes — restrict via `DefaultGetServlet` config or Dispatcher rules in production.
- No built-in API versioning — must build your own convention (e.g. `/api/v1/...` resourceTypes).
- JSON endpoints are NOT cached by Dispatcher by default (often explicitly denied) — must opt in and consider cache-poisoning risk for personalised data.
- Cross-domain frontends need `org.apache.sling.cors.impl.CrossOriginFilter` configured, or browsers block the calls.

### GraphQL — Headless Content Fragment Delivery ONLY

**Critical distinction:** AEM's GraphQL API is scoped specifically to **Content Fragments**, not arbitrary pages/components — unlike REST, which can expose any resource.

**Workflow:** Content Fragment Model (schema, under `/conf/.../cfm/models`) → actual Content Fragments authored in DAM → AEM **auto-generates** the GraphQL schema (one type per model) → query via an endpoint configured in *Tools → General → GraphQL*.

```graphql
# Ad-hoc POST query — fine in dev, DISABLED by default in production/AEMaaCS (DoS risk)
{ articleModelList { items { title author { name } } } }
```

```
# Persisted Query (GET) — production-recommended, Dispatcher/CDN-cacheable
GET /content/_cq_graphql/mysite/endpoint.json/mysite/getArticleByPath;articlePath=/content/dam/mysite/articles/my-article
```

**Why persisted queries matter:** Ad-hoc POST queries let a client construct arbitrarily expensive/deep queries at runtime; persisted queries are invoked via deterministic GET URLs, which Dispatcher/CDN can actually cache — the single biggest practical reason teams adopt them.

| Fact | Detail |
|---|---|
| Read-only | No mutations — AEM author UI remains the only way to create/edit content |
| Nested references resolved in one call | A model field referencing another model is resolved server-side — the core advantage over REST's N+1 calls |
| Schema is derived, not independently versioned | Renaming a CF Model field renames the GraphQL field — no separate schema-versioning layer |
| Endpoint is scoped per `/conf` configuration | Not global to the instance |

**Interview Q: When would you choose REST over GraphQL in AEM, or vice versa?**  
REST for arbitrary resources, writes, or quick exposure of existing Sling Models. GraphQL specifically when delivering Content Fragments headlessly to SPAs/mobile apps where avoiding over-fetching and resolving nested references in one round-trip matters, and where persisted-query cacheability is valuable.

---

## 24. Oak & JCR Internals, TarMK, MongoMK

### JCR vs Oak

JCR (JSR-170/283) is a **specification only** — a contract for content-as-a-tree (nodes/properties, versioning, search, ACLs, observation). **Apache Jackrabbit Oak** is the actual engine implementing that contract, and what AEM 6.x/AEMaaCS runs on.

### Oak's Core Architecture — the NodeStore abstraction

```
JCR API → Oak Core (query engine, security, observation)
              → NodeStore (pluggable storage)
                  ├── SegmentNodeStore (= TarMK)
                  └── DocumentNodeStore (= MongoMK / RDB)
```

This separation is *why* the same AEM application code runs unmodified on completely different storage backends.

### MVCC — Concurrency Without Locking

Oak never locks for reads. Every commit produces a new **immutable NodeState** — like a Git commit: the new state mostly shares structure with the previous one (structural sharing); only changed nodes get new records. Readers always see one consistent point-in-time snapshot, even during concurrent writes elsewhere.

### TarMK (Segment Node Store)

- Default engine for most single-instance/on-prem deployments; conceptually the basis for AEMaaCS storage too.
- Data is written as **segments** (~256KB binary chunks: Node/Property/Template/Blob/List/Map records), packed append-only into `.tar` files.
- A **Journal** file points to the current head revision.
- **Git-like commits:** a save writes only new/changed records; the new root mostly points to unchanged old segments.
- **Compaction** (Online Revision Cleanup) reclaims disk by removing segments no longer referenced by any retained revision — necessary because old superseded data is never deleted at write time.

| Compaction type | Behaviour |
|---|---|
| Tail compaction (default) | Incremental, online, no maintenance window |
| Full compaction | Thorough rewrite; historically needed downtime, modern Oak versions handle more of this online |

- Performance depends heavily on memory-mapped file access plus in-process caches (Segment/String/Node Cache) — undersized caches are a common root cause of slow instances.
- **Clustering limitation:** TarMK is local to one JVM's filesystem. **Cold Standby** provides one master + continuously-synced standby instances for DR/read-scaling — active-passive, not true multi-master.

### MongoMK (Document Node Store)

- Solves concurrent multi-author-instance writes by storing each JCR node as one MongoDB document.
- Mongo's own replication/sharding lets multiple AEM JVMs (each a distinct `clusterId`) read/write the same backing store concurrently.
- **Cluster coordination:** each node writes periodic heartbeats/leases to a `clusterNodes` collection; a missed lease triggers **recovery** to repair half-committed changes from a crashed node.
- Concurrent conflicting writes from different nodes can surface as `OakState0001: Unable to merge changes` — application code must handle retries.
- Every cache miss is a network round-trip to Mongo — Oak layers a `NodeCache`/diff cache plus an optional local **Persistent Cache** (TarMK-format file) specifically to reduce Mongo calls.

> **Interview-critical fact:** AEM as a Cloud Service does **NOT** use MongoMK. AEMaaCS reverted to a Segment Store approach backed by cloud blob storage (Azure Blob/AWS S3) instead of local disk — Adobe judged a separate Mongo cluster too much operational overhead for a cloud-native, auto-scaling architecture. MongoMK is primarily relevant to **on-prem/AMS AEM 6.x author clustering** today.

### Oak Indexing

- **Property indexes**: updated synchronously, inside the same commit — always immediately consistent.
- **Lucene indexes** (full-text, sorting, complex queries): updated asynchronously on a background "async" lane (typically every few seconds) — why newly-created content can briefly not appear in search results.

### Common Interview Questions — Oak/JCR

**Q: Why can't TarMK support multiple AEM author instances writing concurrently, but MongoMK can?**  
TarMK's segment store is local to one JVM's filesystem with a single Journal head — there's no mechanism for two JVMs to coordinate writes to the same files. MongoMK delegates that coordination to MongoDB itself, which is designed for concurrent distributed access, plus Oak's own cluster lease/recovery mechanism.

**Q: Why doesn't a newly published page show up immediately in search?**  
Lucene indexes update asynchronously on a background lane, not within the triggering commit — there's a small, normal lag between content being saved and it being searchable.

**Q: What replaced MongoMK in AEM as a Cloud Service?**  
A Segment Store (TarMK-style) approach backed by cloud blob storage (Azure Blob/AWS S3), not local disk — chosen for lower operational overhead in a cloud-native, auto-scaling architecture.

**Q: What is structural sharing in the context of Oak's MVCC model?**  
When a new NodeState is created after a commit, it reuses references to all unchanged child nodes from the previous state and only creates new records for what actually changed — directly analogous to how a Git commit mostly points to unchanged blobs/trees rather than copying the whole repository.

### Layman terms Explanation

Think of it like a citywide records-keeping system.

JCR is just the rulebook — a standard saying "all content must be organized as a tree of folders and files, with version history, search, and permissions." It doesn't say which actual building stores anything.

Oak is the real records office that follows that rulebook — the staff and machinery that actually file things, look things up, and enforce the rules.

Behind Oak's front desk sits a swappable storage room — that's the NodeStore abstraction. The person at the front desk doesn't care whether the storage room is a back-office filing cabinet (TarMK) or a remote shared warehouse (MongoMK) — the experience at the counter is identical either way. This swappability is why the same AEM code runs unmodified on totally different storage backends.

MVCC (immutable NodeState) works like a smart photocopier: every time someone edits a page in a book, it doesn't scribble on the original — it photocopies a "new version" of the book, but cleverly reuses every unchanged page from before and only freshly prints the one page that actually changed. Anyone reading the book while someone else is mid-edit always sees a complete, consistent old copy — never a half-changed mess.

TarMK is that back-office filing cabinet: papers (segments) get stapled into folders (tar files) and slotted into the back of the drawer — nothing already filed ever gets edited in place, only added to. Over time, old superseded paper versions pile up uselessly — that's exactly why compaction exists: it's the cleanup crew that periodically clears out old, no-longer-needed copies so the cabinet doesn't grow forever.
Because that cabinet physically lives in one room, only one office can write into it at a time. Cold Standby is like a second back-office across town that gets a photocopy of every new folder the moment it's filed — ready to take over instantly if the first room floods — but it's not simultaneously taking its own walk-in clients.

MongoMK solves the "only one office" problem differently: instead of one back-room, imagine many branch offices across the city, all plugged into the same shared, already-distributed central filing service (MongoDB) that knows how to handle multiple branches reading and writing into it at once. Each branch occasionally calls in (heartbeat/lease) to say "still open, still working" — and if a branch goes dark mid-task without warning, headquarters sends a crew to tidy up whatever paperwork it left half-finished (recovery). If two branches grab for the same file at the same instant, the system just tells one "someone beat you to it, try again" (conflict retry). Because every single lookup means a phone call to the central service, branches keep a personal photocopy of frequently-needed files on a side desk (persistent cache) so they aren't calling headquarters for everything.

AEM as a Cloud Service went a different way: rather than running a whole city of Mongo branch offices (too much overhead to operate), it went back to the single-filing-cabinet style (TarMK), just relocated that cabinet into a shared off-site cloud storage unit (Azure Blob/S3) instead of a local back room — simpler to run, while still getting cloud scale.

Finally — a brand-new book on the shelf gets an instant index card filed the second it arrives (property index), but the big master search catalog (Lucene index) is only rebuilt every few seconds in the background — which is exactly why something you just published can take a few seconds to actually show up in search.

---

## 25. Sling Request Processing Pipeline

This is the internal request lifecycle inside AEM/Sling — distinct from the earlier Browser→CDN→Dispatcher flow, which is what happens before the request even reaches this pipeline.

```
HTTP request arrives at the Sling Engine (on Jetty/Felix HTTP Whiteboard)
│
▼
1. Authentication — SlingAuthenticator picks a handler
   (Form/Basic/Token/SSO) → resolves a Session/ResourceResolver
   (or falls back to anonymous)
   │
   ▼
2. Resource Resolution — ResourceResolver.resolve(path)
   Strips selectors/extension/suffix, walks UP the path until
   it finds an actual existing resource; remainder becomes
   the request "suffix". Vanity URLs / sling:redirect / /etc/map
   mappings are also applied at this stage.
   │
   ▼
3. Servlet/Script Resolution
   Resource's sling:resourceType (+ resourceSuperType chain)
   combined with selectors + extension + HTTP method →
   picks the MOST SPECIFIC matching script/servlet under
   /apps or /libs.
   │
   ▼
4. REQUEST-scope Filter Chain (pre-processing)
   Filters run in service.ranking order, each calling
   chain.doFilter() to proceed
   │
   ▼
5. Component Rendering (HTL/JSP/Servlet executes)
   Nested sling:include / sling:resource calls trigger
   steps 2-4 AGAIN recursively for each child component —
   page rendering is really a TREE of nested Sling requests
   │
   ▼
6. Filter post-processing (response unwinds back through
   the same filters, in reverse, after chain.doFilter() returns)
   │
   ▼
   Response sent to client
```

### Two design facts worth knowing well:
Resources, not JCR nodes, are the real abstraction. Sling doesn't hard-wire itself to JCR. It talks to a `ResourceProvider` SPI — JCR is just one provider (`JcrResourceProvider`). Other providers can mount virtual resources from anywhere (an external API, a config map, OSGi bundle resources) into the same resource tree at a chosen mount point. 
This is why the same Sling Model/HTL code can render content regardless of whether it actually came from JCR — the rest of the stack only ever talks to "Resources," never to raw JCR.
The /apps over /libs overlay mechanism (Resource Merger) is how customisations of Adobe's out-of-the-box components work without ever touching /libs directly: Sling's script/servlet resolution checks /apps first, falls back to /libs if nothing's there, and the Resource Merger can even merge node properties from both locations (not just whole resources) for partial overlay scenarios — this underlies the well-known "never modify /libs, always override in /apps" rule.

## 26. Custom Widget, Content Fragment & Adobe Launch — Applied Scenario

One connected scenario across all three previously-missing topics: extending a Property Listing component with (12) a custom star-rating dialog widget, (13) a Content Fragment reference, and (14) Adobe Launch tracking.

### Granite UI Custom Widget — Property Condition Rating

```xml
<!-- Component extends the base form field so it inherits label/description/validation slots -->
<jcr:root jcr:primaryType="cq:Component" jcr:title="Condition Star Rating Field"
    extends="granite/ui/components/coral/foundation/form/field"/>
```

```javascript
$stars.on("click", ".sibi-ConditionRating-star", function () {
    var value = $(this).data("value");
    $input.val(value).trigger("change"); // mandatory — dialog dirty-tracking listens for this
    render(value);
});
```

**Interview Q: Why must a custom Granite widget use a hidden `<input>` rather than a custom data attribute?**  
Coral's dialog submit logic serialises standard form elements (`input`/`select`/`textarea` with a `name`) inside the dialog form — it has no concept of arbitrary data attributes. The hidden input bridges a custom visual widget to AEM's unmodified save mechanism.

**Interview Q: What does `extends` do on a custom Granite component's `.content.xml`?**  
Inherits rendering behaviour from a base component — here, the base form field's label/description/validation-message slots — so a custom widget doesn't need to reimplement that scaffolding.

### Content Fragment API — Neighborhood Guide Reference

```java
ContentFragment fragment = resource.adaptTo(ContentFragment.class);
ContentElement element = fragment.getElement("walkabilityScore");
Integer score = element.getValue(Integer.class);  // structured types use getValue(Class)
String summary = fragment.getElement("summary").getContent(); // plain text uses getContent()
```

**Interview Q: Why does `NeighborhoodGuide` take a `Resource` via constructor instead of using `@Self`/field injection?**  
Because it's adapted from the FRAGMENT's own resource path, not the calling component's resource — the caller must resolve `neighborhoodFragmentPath` to a `Resource` first, then explicitly construct the model from it. Sling Models with non-default constructors are instantiated directly, not via `resource.adaptTo()`.

**Interview Q: Why model neighborhood data as a Content Fragment instead of plain text fields on the property page?**  
Reuse — the same neighborhood guide fragment is referenced by every property listing in that area; updating it once updates every page referencing it. A plain text field would require duplicating and separately maintaining that content on every property page.

### Adobe Launch / Client Context

```java
// Compose with the existing model — never re-read the same JCR properties twice
property = request.adaptTo(PropertyListing.class);
propertyId = resource.getName();
```

```html
<script>
    window.adobeDataLayer = window.adobeDataLayer || [];
    window.adobeDataLayer.push(${dataLayer.dataLayerJson @ context='unsafe'});
</script>
<button class="property-listing__rsvp-button" data-property-id="${dataLayer.propertyId}">RSVP</button>
```

```javascript
// Click event reads the ID from the attribute Java rendered — never invents it in JS
window.adobeDataLayer.push({ event: "openHouseRsvpClick",
    property: { propertyId: button.getAttribute("data-property-id") } });
```

**Interview Q: Why is `context='unsafe'` acceptable here when it's normally forbidden for user input?**  
The JSON is fully server-generated from typed Java fields (price, status, property type) — never raw unescaped author free-text. The rule is "never use unsafe on untrusted input," not "never use unsafe at all." A field that echoes free-text directly into this JSON would need sanitisation first.

**Interview Q: Why is the property ID rendered as a `data-*` attribute instead of being looked up again in JavaScript?**  
Single source of truth — the Java model is the only place that decides what the ID actually IS; JavaScript only decides WHEN to fire an event using that already-defined value. This avoids a markup/JS refactor silently breaking analytics with no compiler error to catch it (the same DOM-scraping fragility problem discussed earlier in Section 14's conceptual explanation).

---

## 26. CSRF Token Handling

### The attack in one sentence
A malicious website makes your browser silently POST to AEM using your session cookie — because browsers attach cookies automatically to every matching-domain request, AEM can't tell the forged request from a real one.

### The fix in one sentence
AEM generates a secret random token, puts it in a response BODY (not a cookie), and rejects any POST that doesn't include it — cross-origin scripts can never read response bodies (Same-Origin Policy), so the attacker can never get the token.

### What to use in practice

```javascript
// GET token from OOTB endpoint — never build your own
fetch("/libs/granite/csrf/token.json", { credentials: "same-origin" })
    .then(r => r.json()).then(d => { csrfToken = d.token; });

// Attach as header on every POST
headers: { "CSRF-Token": csrfToken }
// OR as a parameter named :cq_csrf_token
```

### What NOT to do

| Wrong | Right |
|---|---|
| Build a custom GET endpoint to issue tokens | Use `/libs/granite/csrf/token.json` |
| Call `csrfTokenManager.isValidToken()` in doPost | `CSRFFilter` already validated before doPost runs |
| Rely on CSRF protection for GET requests | GET requests are never CSRF-protected — never mutate state in a GET handler |

### Common interview Q&A

**Q: What HTTP status does CSRFFilter return on a bad token?**  
HTTP 403 — before your servlet's doPost() is ever invoked.

**Q: Does CSRF apply to server-to-server calls?**  
No — CSRF is a browser attack. Server-to-server calls don't carry session cookies automatically; exclude those paths from the filter or use service-user sessions which bypass the HTTP filter stack.

**Q: Why can't evil.com just call /libs/granite/csrf/token.json and read the token?**  
It can SEND the request but cannot READ the response — Same-Origin Policy blocks cross-origin response reads. The token is in the body, not in a cookie, so SOP locks it away from the attacker's script.

---

## 27. XSS Protection — XSSAPI

### Core concept: context-specific escaping

Using the wrong escaper for a context is as dangerous as no escaping at all.

| Context | Method | Escapes |
|---|---|---|
| HTML body text | `encodeForHTML(v)` | `< > & " '` |
| HTML attribute value | `encodeForHTMLAttr(v)` | `" '` and attribute-breakers |
| JS string literal | `encodeForJSString(v)` | `\ ' " newlines </script>` |
| URL (href/src) | `filterURLProtocols(v)` + `encodeForHTML(v)` | Blocks javascript:/vbscript: then HTML-encodes |
| Rich text / RTE | `filterHTML(v)` | Strips script/iframe/on* via allowlist |
| Redirect target | `getValidHref(v)` | Validates structure + protocol |

### The JS string context explained

```
Attack input:  '; alert(document.cookie); var x = '

Without encodeForJSString:
  <script>var q = ''; alert(document.cookie); var x = '';</script>
  → the ' closes the string → attacker's code executes

With encodeForJSString:
  <script>var q = '\'; alert(document.cookie); var x = \'';</script>
  → \' means "literal apostrophe inside the string" → nothing executes
```

HTML escaping (`&#39;`) does NOT work here — the JS engine doesn't decode HTML entities inside script blocks.

### The </script> injection corner case

```
Attack input:  </script><script>alert(1)</script>

Without encoding:
  <script>var q = '</script><script>alert(1)</script>';</script>
  → the HTML parser sees </script> and CLOSES the block → attack succeeds

encodeForJSString escapes / as \/ → </script> becomes <\/script>
  → HTML parser never sees a closing tag → attack fails
```

### URL context — two-step mandatory

```java
// Step 1: block dangerous schemes (javascript:, vbscript:, data:)
String filtered = xssAPI.filterURLProtocols(rawUrl);
// Step 2: encode remaining characters for HTML attribute context
String safe = xssAPI.encodeForHTML(filtered);
// Step 3 (open redirect prevention): only allow relative paths
if (safe.startsWith("/")) { /* use it */ }
```

### Common interview Q&A

**Q: When should you use filterHTML vs encodeForHTML?**  
`filterHTML` when the input IS expected to contain formatting HTML (e.g. RTE output) — it allows safe tags and strips dangerous ones. `encodeForHTML` when the input must be plain text — it encodes ALL tags as visible text, stripping no HTML, making it all literal characters.

**Q: Does HTL's default escaping protect you everywhere?**  
No — only in HTML body text and basic attribute contexts inside HTL templates. Java-built HTML strings (servlet responses), JS string literals in `<script>` blocks, and URL values all require explicit XSSAPI calls.

**Q: Can `@ context='html'` in HTL replace filterHTML?**  
Only for RTE fields authored through AEM's own dialog, which sanitises on save. For user-supplied input that arrives via HTTP request parameters, always use `xssAPI.filterHTML()` — never `@ context='html'` directly on untrusted input, as `context='html'` in HTL is equivalent to no escaping (it trusts the value is already safe HTML).

**Q: What's wrong with using encodeForHTML inside a href attribute?**  
It's incomplete — `encodeForHTML` doesn't block `javascript:` protocol. `filterURLProtocols()` is required as the first step to strip dangerous schemes before HTML-encoding the result.

---

## 28. Thread Dumps — Senior Interview Reference

### How to take one
```bash
jstack <PID> > dump1.txt; sleep 10; jstack <PID> > dump2.txt; sleep 10; jstack <PID> > dump3.txt
# Take 3, 10 seconds apart — patterns across all 3 reveal genuinely stuck threads
```

### Thread states cheat sheet
| State | What it means | Red flag? |
|---|---|---|
| `RUNNABLE` | Executing or ready to execute | No |
| `BLOCKED` | Waiting to acquire a monitor lock | **Yes — contention** |
| `TIMED_WAITING` | Sleeping with timeout (scheduler) | Usually no |
| `WAITING` | Waiting indefinitely (I/O, lock) | Possibly |

### Interview analysis checklist
```
1. How many http-nio-4502-exec-* threads? What state are most in?
2. If BLOCKED — what lock hex address? Search for "- locked <that hex>" to find holder.
3. What is the lock holder doing? Read its stack bottom-to-top.
4. Any "Found one Java-level deadlock" at the bottom of the dump?
5. Same RUNNABLE stack in all 3 dumps = runaway loop. Cross-ref with top -H -p <PID>.
```

**Q: AEM is unresponsive. Thread dump shows 47 request threads BLOCKED on the same hex, held by a scheduler thread inside `fetchProductData()`. What happened and how do you fix it?**  
The scheduler is holding a JVM monitor lock while blocked on a slow external HTTP call (or exhausted connection pool). All request threads queue behind it. Fix: remove `synchronized` from the method, or add a `connectionRequestTimeout` to the HTTP client so it fails fast instead of blocking indefinitely, releasing the lock.

**Q: How do you find which thread is causing 100% CPU?**  
`top -H -p <PID>` → note the OS thread ID using most CPU → convert to hex → search `nid=<hex>` in the thread dump → read that thread's call stack.

**Q: What's the difference between BLOCKED and WAITING?**  
`BLOCKED` = thread wants a monitor lock currently held by another thread — classic lock contention. `WAITING` = thread voluntarily released the CPU and is waiting for a signal or notification (e.g. `Object.wait()`, `LockSupport.park()`) — often normal for idle threads or threads waiting for async I/O.

---

## 29. Heap Dumps — Senior Interview Reference

### When to take one
- OutOfMemoryError (configure `-XX:+HeapDumpOnOutOfMemoryError` in advance)
- Sustained high memory usage (Old Gen > 80% after Full GC)
- Proactive: `jmap -dump:live,format=b,file=/tmp/heap.hprof <PID>`

### Eclipse MAT analysis flow
```
1. Open .hprof → Run "Leak Suspects" report first
2. Histogram → sort by Retained Heap → look for unexpected class counts
3. Right-click suspicious class → Path to GC Roots → Exclude weak refs
4. Root of that chain = your code holding the reference = the bug
5. OQL for targeted hunting: SELECT * FROM java.util.HashMap m WHERE m.size > 10000
```

### AEM-specific things to look for in Histogram

| Unexpected finding | Root cause |
|---|---|
| Thousands of `JcrResourceResolver` | ResourceResolver never closed — use try-with-resources |
| One huge `HashMap`/`ConcurrentHashMap` | Unbounded cache — replace with Guava Cache + maximumSize + TTL |
| Many `QueryResult` instances | QueryBuilder session leak — cast query to Closeable and close it |
| Large `String[]` or `char[]` | Large content being held in memory — check for huge RTE field reads |

**Q: What's the difference between Shallow Heap and Retained Heap in MAT?**  
Shallow Heap = memory used by the object itself (its fields). Retained Heap = memory that would be freed if this object AND everything it exclusively references were collected. Retained Heap is what matters for finding leaks — an object with 48 bytes shallow but 287MB retained is holding a huge graph of other objects alive.

**Q: What is "Path to GC Roots" in MAT and why do you use it?**  
It traces the reference chain from a suspicious object all the way back to a GC Root (a thread, a static field, a JNI reference) — the one thing keeping it alive. This tells you exactly which piece of your code is holding the object and preventing garbage collection.

**Q: Why use `jmap -dump:live` instead of plain `jmap -dump`?**  
`live` forces a Full GC first, then dumps only surviving (reachable) objects. This eliminates unreachable objects that haven't been collected yet, making the dump smaller and the analysis focused on genuine leaks rather than GC noise.

---

## 30. CIF GraphQL Caching

### Architecture
CIF caches GraphQL responses **in-process inside the AEM JVM** (Guava Cache), not in Dispatcher — because GraphQL POST requests have no stable URL to cache against at the HTTP layer.

```
Browser → AEM Publish
              ↓
    GraphqlClientImpl (Guava Cache — in JVM heap)
              ↓ cache miss only
    Adobe Commerce / Magento GraphQL API
```

### Cache key
SHA-256 hash of (query string + serialised variables). Same query + same variables = cache hit regardless of which user triggered it.

### OSGi config (factory — one per commerce endpoint)
```json
{
  "cacheEnabled": true,
  "cachingTime": 300,
  "cacheSize": 100,
  "httpMethod": "GET"
}
```
Factory config = one instance per site. Author can have short TTL (editors see fresh data), publish has longer TTL (performance).

### Viewing cache stats
```
/system/console/status-com.adobe.cq.commerce.graphql.client  ← hit rate, miss rate, evictions
/system/console/jmx → "GraphqlClient"                        ← live stats + invalidateAll operation
```

### Clearing the cache
| Method | How |
|---|---|
| TTL expiry | Automatic — wait `cachingTime` seconds |
| JMX | `/system/console/jmx` → GraphqlClient → `invalidateAll` |
| OSGi config save | Triggers `@Modified` → cache rebuilt |
| Bundle restart | Stop/Start bundle in `/system/console/bundles` |

**Q: Why doesn't CIF use Dispatcher to cache GraphQL responses?**  
Dispatcher caches HTTP responses keyed by URL. GraphQL queries are typically POST requests — POST has no stable URL and Dispatcher doesn't cache POST by design. CIF's JVM-level cache intercepts at the Java client layer before the HTTP response leaves AEM, making it framework-agnostic. Persisted queries (GET) CAN be Dispatcher-cached and is the recommended production approach.

**Q: What happens to the CIF GraphQL cache on AEM restart?**  
Lost entirely — it's in JVM heap memory only, not persisted. First requests after restart are all cache misses and call Magento directly. Design your `cachingTime` so this cold-start period is acceptable.

**Q: How do you handle stale product prices after a Magento price update?**  
Three options: (1) short `cachingTime` (60-120s) so stale data self-heals quickly, (2) Magento webhook on price change triggers AEM JMX `invalidateAll`, (3) Commerce Event Bus triggers a CIF cache invalidation listener. Most production teams use option 1 + option 2 together.

**Q: How is CIF's GraphQL cache different from the JVM cache you'd write yourself (e.g. InventoryServiceImpl)?**  
Functionally the same pattern — both are Guava Caches with `maximumSize` + `expireAfterWrite`. The difference is CIF's is built-in and configured via OSGi factory config per commerce endpoint, while your own cache is custom-written per service. The same "no ConcurrentHashMap without eviction" rule applies to both.

---

## 31. Lucene & Oak Search — Complete Reference

### Layman: What Lucene Is

AEM's repository has potentially millions of pages. Without a search index, finding pages matching a keyword means opening every single page and checking its content — like a librarian reading every book in a library. With Lucene, AEM pre-builds a **back-of-book card catalogue**: "beachfront appears in pages 1,234 and 45,678. Villa appears in pages 45,678, 89,123 and 234,567." A search for "beachfront villa" just looks up both words and returns the intersection — milliseconds, regardless of repository size.

This is called an **inverted index** — instead of "document → words it contains," it stores "word → documents that contain it."

---

### The Inverted Index Structure — In Detail

Lucene stores three things on disk:

**1. The Dictionary — every unique word, sorted alphabetically**
```
"apartment"
"available"
"beachfront"
"villa"
"waterfront"
```
Sorted so binary search finds any word in ~20 steps regardless of dictionary size (log₂ of 10,000,000 = 23). Like finding a name in a phone book by repeatedly opening to the middle — never reading it cover to cover.

**2. The Postings List — for each word, which document IDs contain it**
```
"beachfront" → [doc_45, doc_234, doc_567]
"villa"      → [doc_45, doc_123, doc_234]
```
Stored as compact delta-encoded integers (differences between consecutive IDs, not full IDs — small numbers compress better).

**3. The Document Store — stored field values per document**
```
doc_45  → { title: "Beachfront Villa Maldives", price: 850000, status: "available" }
```
Only fields marked `stored=true` go here — for returning values without re-reading JCR.

**A search query does this — effectively instant:**
```
Query: "beachfront" AND "villa"
Step 1: Binary search dictionary → "beachfront" → [doc_45, doc_234, doc_567]
Step 2: Binary search dictionary → "villa"      → [doc_45, doc_123, doc_234]
Step 3: Intersect both lists                    → [doc_45, doc_234]
Total: ~3 operations. Same speed for 10 or 10 million documents.
```

**Physical files in AEM:**
```
crx-quickstart/repository/index/
    lucene-1234/
        _0.cfs     ← compound file segment (actual index data)
        _0.si      ← segment info
        segments_N ← active segment list
        write.lock ← prevents concurrent writes
```

---

### The Indexing Pipeline — How Content Gets INTO the Index

Oak doesn't iterate through all nodes to build the index. It **reacts to change events** — the MVCC/NodeState diff system tracks exactly what changed between commits, so the indexer only ever processes what actually changed.

```
You click Save in AEM
    ↓
JCR saves the node (immediate)
    ↓
Oak writes a checkpoint: "node /content/.../my-property changed"
    ↓
(~5 seconds later) Async indexer wakes up, reads the checkpoint
    ↓
Checks which index definitions cover this node type/path
    ↓
For each matching index: reads the relevant properties
    ↓
Analyzer runs on text fields:
  "Beachfront Villa Maldives"
    → Tokenise  : ["Beachfront", "Villa", "Maldives"]
    → Lowercase  : ["beachfront", "villa", "maldives"]
    → Stop words : ["beachfront", "villa", "maldives"]  (none removed here)
    → Stem       : ["beachfront", "villa", "maldiv"]
    ↓
IndexWriter adds to postings:
  "beachfront" → doc_45
  "villa"      → doc_45
  "maldiv"     → doc_45
    ↓
Written to a new segment file on disk
    ↓
Next query for "beachfront villa" finds doc_45 instantly
```

**Key insight:** The indexer reacts to diffs, never scans the full repository. Oak knew exactly which node changed — the indexer only processes that delta.

---

### Asynchronous Indexing — The Most Important Operational Fact

```
/oak:index/@async = "async"            → runs every ~5 seconds
/oak:index/@async = "fulltext-async"   → separate lane, also ~5 seconds
```

A page saved right now will NOT appear in a Lucene search for up to 5 seconds. Property indexes update synchronously (within the same commit) — always immediately consistent.

Monitor async lag:
```
/system/console/jmx → "Async Indexing Statistics"
→ shows: LastIndexedTime, IndexingLag, FailedIndexCount
```

---

### Why Searching a Huge Index Isn't Slow — The Three Clarifications

**"Going through millions of dictionary terms must be slow"**
No — binary search. Finding "villa" in 10 million terms = 23 comparisons. Not 10 million.

**"Iterating 50,000 matching document IDs must be slow"**
No — integers in a compact array. A modern CPU processes ~1 billion integers/second. 50,000 takes 0.05 milliseconds. Merging two sorted lists (merge-join algorithm) runs in O(n+m), not O(n×m).

**"Oak must check every index definition for each query"**
No — the Oak **query planner** scores each index mathematically (using stored statistics about how many documents each index covers and how selective each predicate is) and picks exactly one winner. The entire evaluation is arithmetic, not data access — microseconds.

```
p.explain=true shows the decision:
GET /bin/querybuilder.json?type=cq:Page&...&p.explain=true
→ "plan": "[propertyListingIndex] cost: 2.3"   ← picked over traversal cost: 500,000
```

---

### Four Oak Index Types

| Type | How it works | Use for |
|---|---|---|
| `lucene` | Full Lucene inverted index | Full-text search, complex multi-property queries, sorting, facets |
| `property` | B-tree on a single property | Simple exact equality / range queries |
| `nodetype` | Index by jcr:primaryType / jcr:mixinTypes | Always used first to narrow by node type |
| `counter` | Counts nodes in subtree | Rarely used directly |

---

### OOTB Indexes

| Index | Covers |
|---|---|
| `lucene` | Default full-text — all node types, all text |
| `cqPageLucene` | cq:Page nodes — most page queries use this |
| `damAssetLucene` | DAM assets — asset search in Touch UI |
| `workflowDataLucene` | Workflow instances |

---

### When to Create a Custom Index

Create one when:
1. Your query triggers a Traversal Warning in `error.log`
2. You need to sort by a custom JCR property (`ordered=true` required)
3. You need full-text search scoped to specific node types (narrower = faster)
4. `/system/console/jmx → QueryStat → Slow Queries` shows your query at the top

**Traversal Warning — the trigger:**
```
*WARN* Traversal query with more than 100000 nodes:
/jcr:root/content/mysite//element(*,cq:Page)[jcr:content/@myCustomProp = 'value']
```
Oak is telling you: I had no index for this predicate, I read every node.

---

### Custom Index Definition — Full Example

```xml
<!-- /oak:index/propertyListingIndex/.content.xml -->
<jcr:root jcr:primaryType="oak:QueryIndexDefinition"
    type="lucene"
    async="async"
    compatVersion="{Long}2"
    evaluatePathRestrictions="{Boolean}true"
    includedPaths="[/content/sibi-aem-one]">

    <indexRules jcr:primaryType="nt:unstructured">
        <cq:Page jcr:primaryType="nt:unstructured">
            <properties jcr:primaryType="nt:unstructured">

                <!-- Full-text search on title -->
                <title jcr:primaryType="nt:unstructured"
                    name="jcr:content/jcr:title"
                    analyzed="{Boolean}true"
                    nodeScopeIndex="{Boolean}true"
                    boost="{Double}2.0"/>

                <!-- Exact filter -->
                <propertyType jcr:primaryType="nt:unstructured"
                    name="jcr:content/propertyType"
                    propertyIndex="{Boolean}true"
                    facets="{Boolean}true"/>

                <!-- Range + sort -->
                <price jcr:primaryType="nt:unstructured"
                    name="jcr:content/price"
                    propertyIndex="{Boolean}true"
                    ordered="{Boolean}true"
                    type="Double"/>

                <!-- Date range -->
                <availableFrom jcr:primaryType="nt:unstructured"
                    name="jcr:content/availableFrom"
                    propertyIndex="{Boolean}true"
                    ordered="{Boolean}true"
                    type="Date"/>

            </properties>
        </cq:Page>
    </indexRules>

    <analyzers jcr:primaryType="nt:unstructured">
        <default jcr:primaryType="nt:unstructured">
            <tokenizer jcr:primaryType="nt:unstructured" name="standard"/>
            <filters jcr:primaryType="nt:unstructured">
                <LowerCase jcr:primaryType="nt:unstructured" name="LowerCase"/>
                <Stop jcr:primaryType="nt:unstructured"
                    name="Stop" words="stopwords.txt" ignoreCase="{Boolean}true"/>
                <PorterStem jcr:primaryType="nt:unstructured" name="PorterStem"/>
            </filters>
        </default>
    </analyzers>

</jcr:root>
```

### Index property flags explained

| Flag | Effect |
|---|---|
| `analyzed=true` | Runs value through Analyzer — required for full-text search |
| `nodeScopeIndex=true` | Adds tokens to aggregate full-text — allows `fulltext` predicate to match |
| `propertyIndex=true` | Enables exact equality / range queries via `property` predicate |
| `ordered=true` | Enables sorting by this property — without it ORDER BY causes traversal |
| `type="Double"` | Numeric type for correct range queries and sorting |
| `facets=true` | Enables aggregate counts by field value in result set |
| `boost=2.0` | Matches in this field score twice as high for relevance ranking |
| `evaluatePathRestrictions=true` | Honours path restrictions — almost always set true |
| `includedPaths` | Only index nodes under these paths — smaller, faster index |
| `compatVersion=2` | Required for modern Oak Lucene features — always use 2 |

### After deploying: trigger reindexing

```
CRXDE: /oak:index/propertyListingIndex → add property reindex=Boolean(true) → Save
Oak reindexes existing content, sets reindex back to false when done.

Large repos: use oak-run.jar offline reindex to avoid JVM pause.
```

### Verify the index is being used

```
GET /bin/querybuilder.json?type=cq:Page&1_property=jcr:content/propertyType
    &1_property.value=villa&p.explain=true
→ "[propertyListingIndex] cost: 2.3"    ← your index was picked
→ "traversal cost: 500000"              ← this would mean no index matched
```

---

### Analyzers — How Text Is Processed

When `analyzed=true` is set, both the indexed text AND the search query run through the same Analyzer chain:

```
Input: "Beachfront Villas near the Ocean"
    ↓ Tokenizer (split on whitespace + punctuation)
    ["Beachfront", "Villas", "near", "the", "Ocean"]
    ↓ LowerCase Filter
    ["beachfront", "villas", "near", "the", "ocean"]
    ↓ Stop Words Filter
    ["beachfront", "villas", "ocean"]    ← "near" and "the" removed
    ↓ PorterStem Filter
    ["beachfront", "villa", "ocean"]     ← "villas" → "villa"
```

**Critical rule:** the SAME analyzer must process both index time AND query time. Oak applies the defined analyzer to both sides automatically — define it once in `analyzers/default`.

---

### Stemming — What It Is and Why It Matters

Stemming reduces words to their root form so plural/singular/verb-form variants all match the same index term.

| Original | Stemmed to |
|---|---|
| running, runs, ran | run |
| villas, villa | villa |
| properties, property | properti |
| swimming, swimmer | swim |
| availability, available | avail |

Note: stems are not always real words — "properti" is not a word, but it's the consistent root that both "property" and "properties" map to, enabling them to match each other in search.

**Without stemming:** search "villas" → only matches documents containing exactly "villas". Documents with "villa" NOT returned.

**With stemming:** "villas" → "villa" at both index time and query time → same term → match found.

---

### Partial Word Search (Wildcards)

Lucene stores terms as exact strings sorted in the dictionary. Wildcard search scans the sorted dictionary from the matching prefix point:

```
villa*   → matches: villa, villas, villagio    (efficient — scan from "villa" forward)
*front   → matches: beachfront, waterfront     (SLOW — must scan entire dictionary)
villa?   → ? = exactly one character           (moderate — bounded scan)
```

In QueryBuilder:
```java
params.put("fulltext", "villa*");
params.put("fulltext.relPath", "jcr:content");
```

In JCR-SQL2:
```sql
SELECT * FROM [cq:Page] WHERE CONTAINS([jcr:content/jcr:title], 'villa*')
```

**Wildcards and stemming conflict:** if stemming is active, "villas" was stored as "villa." Searching "villa*" matches "villa" and stemmed terms starting with "villa" — potentially unexpected results. For wildcard-heavy use cases, consider a separate index without stemming.

**Leading wildcards (`*word`) are always slow** — avoid in production on large repositories.

---

### Fuzzy Search

Matches words within an edit distance (Levenshtein — number of single-character edits):

```java
params.put("fulltext", "villa~");    // default distance 2
params.put("fulltext", "villa~1");   // distance 1: one character different
```

Practical use: user types "beachfrunt" (typo) → fuzzy~1 matches "beachfront" (one character different) → result returned instead of zero results.

---

### Relevance Scoring (TF-IDF)

When no explicit `orderby` is set, results rank by `@jcr:score` using TF-IDF:

**TF (Term Frequency):** how many times the search term appears in this document — more = higher score.

**IDF (Inverse Document Frequency):** how rare the term is across all documents — rarer terms score higher. "beachfront" in 5 pages scores higher than "the" in 500,000 pages.

**Field boost:** matches in boosted fields score higher:
```xml
<title name="jcr:content/jcr:title" analyzed="true" boost="2.0"/>
<!-- A title match is worth twice a body text match -->
```

---

### Faceted Search

Facets = aggregate counts by field value across the entire result set:

```
Search: "available villa" → 100 results
Facets: propertyType → { villa: 45, apartment: 32, house: 23 }
        status       → { available: 78, rented: 22 }
```

Enable in index definition:
```xml
<propertyType name="jcr:content/propertyType" propertyIndex="true" facets="true"/>
```

In QueryBuilder:
```java
params.put("facetextract.1_property", "jcr:content/propertyType");
params.put("facetextract.1_property.count", "10");
// result.getFacets() → {"villa":45, "apartment":32, "house":23}
```

---

### Full End-to-End Search Flow (How Everything Connects)

```
YOU SEARCH: "beachfront villa, propertyType=villa, price < 500000"
    ↓
Oak Query Planner evaluates all indexes by cost:
  default lucene:          cost=50,000   (covers everything, not selective)
  propertyListingIndex:    cost=2.3      (covers cq:Page + has propertyType)
  → PICKS propertyListingIndex
    ↓
Lucene dictionary binary search:
  "beachfront" → 23 comparisons → [doc_45, doc_234, doc_567]
  "villa"      → 23 comparisons → [doc_45, doc_123, doc_234]
  Merge-join intersect           → [doc_45, doc_234]
    ↓
Filter: propertyType B-tree lookup "villa" → [doc_45, doc_234, doc_89]
  Intersect with full-text:                 → [doc_45, doc_234]
    ↓
Filter: numeric range price < 500000
  doc_234 has price=1,200,000 → eliminated
  Remaining:                                → [doc_45]
    ↓
Sort by price (ordered Double field — pre-sorted in index)
    ↓
Return doc_45 → Oak reads JCR node from repository
    ↓
Result: /content/sibi-aem-one/en/properties/beachfront-villa-maldives

Total operations: ~100 comparisons. Under 1ms for any repository size.
Traversal alternative: 500,000 JCR node reads × 1ms each = 500 seconds.
```

---

### Summary Cheat Sheet

| Need | Solution |
|---|---|
| Exact match on custom property | Lucene index with `propertyIndex=true` |
| Full-text keyword search | `analyzed=true` + `nodeScopeIndex=true` |
| Partial word (`villa*`) | Lucene fulltext with wildcard |
| Typo-tolerant search | Lucene fulltext with `~` operator |
| Sort by custom property | `ordered=true` on that property |
| Faceted counts | `facets=true` on that property |
| Stemming | Add `PorterStem` filter to `analyzers/default` |
| Stop words | Add `Stop` filter to `analyzers/default` |
| Query using traversal | Add `p.explain=true`, check JMX slow queries, create targeted index |
| Newly saved content not searchable | Normal — async indexer ~5s lag; use property index for instant consistency |

---

### Common Interview Questions — Lucene & Search

**Q: What is the difference between `analyzed=true` and `propertyIndex=true`?**
`propertyIndex=true` enables exact equality and range queries on a property — it uses a B-tree structure, not Lucene's inverted index. `analyzed=true` runs the value through the Analyzer chain (tokenise, lowercase, stem) and adds it to the inverted index — required for full-text search where you want "villas" to match "villa."

**Q: Why does newly saved content not appear immediately in a Lucene search?**
Lucene indexes update asynchronously on a background lane running every ~5 seconds. This is by design — synchronous Lucene indexing inside a JCR commit would make every save dramatically slower. Property indexes update synchronously if you need immediate consistency.

**Q: What happens if you don't set `evaluatePathRestrictions=true`?**
The index returns results from the entire repository regardless of the `path` restriction in your query. Oak then post-filters — but this means the index returns far more results than needed, wasting memory and time.

**Q: How do you find which index a query is actually using?**
Add `p.explain=true` to your QueryBuilder URL. The response shows the chosen index and its estimated cost. If it shows "traversal" your query doesn't match any index.

**Q: Why is a leading wildcard search (`*front`) slow?**
Lucene finds a term like "villa*" by scanning the sorted dictionary forward from "villa" — an efficient bounded scan. A leading wildcard (`*front`) has no starting point — Lucene must compare the suffix against every single term in the dictionary. There is no shortcut.

**Q: What is the relationship between stemming at index time and query time?**
They must use the same Analyzer chain. If "villas" is stemmed to "villa" at index time, then the search query "villas" must also be stemmed to "villa" at query time — otherwise the query term "villas" won't match the index term "villa." Oak applies the same `analyzers/default` definition to both sides automatically.

**Q: How do you trigger reindexing after deploying a new index definition?**
Set `reindex=Boolean(true)` on the index node in CRXDE. Oak detects this, reindexes all existing content matching the index definition, then sets it back to `false`. For large repositories, use oak-run.jar's offline reindex to avoid pausing the JVM.


---------

## 32. AEM Maven Plugins Reference

**Purpose:** A senior-level reference for the Maven plugins found in a typical AEM multi-module project (`core`, `ui.apps`/`all`, `ui.content`, `ui.config`), covering what each plugin actually does, key configuration, and common misconfiguration symptoms — the kind of detail interviewers probe for beyond "I just add dependencies."

---

### 1. Concept — Dependencies vs Plugins

Before the individual plugins: it's worth being explicit about *why* this distinction matters, since it's the root of the "I only touch dependencies" gap.

- **Dependencies** (`<dependencies>`) are code your project *compiles against and/or bundles* — libraries your classes call.
- **Plugins** (`<build><plugins>`) are tools that run *during the build lifecycle itself* — they compile, test, package, transform, and deploy your project. They don't add code to your classpath at runtime; they control what happens when you type `mvn <phase>`.

Every `mvn` command (`compile`, `test`, `package`, `verify`, `install`, `deploy`) is a **phase** in Maven's build lifecycle, and each phase runs zero or more plugin **goals** bound to it. When you run `mvn clean install -PautoInstallPackage`, you are triggering a chain of plugin goals across many phases — the plugins are the actual machinery; the phase names are just labels.

---

### 2. maven-compiler-plugin

#### 2.1 What it does
Compiles your `.java` source files into `.class` bytecode. Bound to the `compile` phase (and `test-compile` for test sources).

#### 2.2 Key configuration

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <source>11</source>
    <target>11</target>
    <!-- or, modern equivalent: -->
    <release>11</release>
  </configuration>
</plugin>
```

#### 2.3 Technicalities
- `<release>` (Java 9+) is preferred over separate `<source>`/`<target>` — it also constrains which JDK *APIs* are available, not just language syntax, preventing accidental use of a newer API method against an older declared target.
- AEM as a Cloud Service currently targets Java 11 (with Java 17 support introduced more recently) — a mismatch between this plugin's configured version and the JDK actually used by your CI/Cloud Manager pipeline is a classic "works locally, fails in pipeline" cause.
- Compiler warnings-as-errors (`<compilerArgument>-Werror</compilerArgument>`) is a stricter option some teams enable — worth knowing whether your project has this on, since it changes how seriously to treat warnings during development.

#### 2.4 Common misconfig symptoms
| Symptom | Likely cause |
|---|---|
| `UnsupportedClassVersionError` at runtime on AEM | Compiled with a newer Java version than the AEM instance/Cloud Service runtime supports |
| Build passes locally, fails in Cloud Manager pipeline | Different JDK version between local machine and pipeline; `<release>` not pinned explicitly |

---

### 3. maven-bundle-plugin (or bnd-maven-plugin)

#### 3.1 What it does
This is the plugin that makes AEM development "OSGi development." It takes your compiled classes and generates the OSGi `MANIFEST.MF` — specifically `Bundle-SymbolicName`, `Import-Package`, `Export-Package`, and `Bundle-Version` — turning a plain JAR into a valid OSGi bundle that AEM's Felix container can load, resolve, and activate.

#### 3.2 Key configuration

```xml
<plugin>
  <groupId>org.apache.felix</groupId>
  <artifactId>maven-bundle-plugin</artifactId>
  <extensions>true</extensions> <!-- required: hooks into the jar packaging lifecycle -->
  <configuration>
    <instructions>
      <Bundle-SymbolicName>${project.artifactId}</Bundle-SymbolicName>
      <Export-Package>com.realestate.core.api.*</Export-Package>
      <Import-Package>*</Import-Package>
      <Sling-Model-Packages>com.realestate.core.models</Sling-Model-Packages>
    </instructions>
  </configuration>
</plugin>
```

Many current AEM archetypes instead use the newer **bnd-maven-plugin** (`biz.aQute.bnd:bnd-maven-plugin`) with a separate `bnd.bnd` file or inline `<bnd>` block — functionally the same purpose, different tooling generation. Know which one your project uses; the config *syntax* differs even though the *goal* (generate a correct OSGi manifest) is identical.

#### 3.3 Technicalities
- **`Import-Package` vs `Export-Package` is the single most important concept here.** `Export-Package` declares which of *your own* packages other bundles may use. `Import-Package` declares which *external* packages your bundle needs, and (critically) at what version range. Getting this wrong is the #1 cause of `"Unresolved constraint... package uses conflict"` errors when deploying to AEM.
- `Import-Package: *` (wildcard, auto-detected by BND analyzing your bytecode) is convenient but can silently import packages you didn't intend to depend on, or miss ones used only via reflection — reflection-based dependencies (Class.forName, dynamic proxies) are invisible to BND's static bytecode analysis and often need to be declared explicitly.
- `Sling-Model-Packages` tells Sling Models' bundle-scanning where to look for `@Model`-annotated classes at runtime — omitting a package here means your Sling Models simply never get registered, with no obvious error message pointing at this specific cause.
- **Package versioning ranges** — BND auto-generates version ranges for imported packages based on the exporting bundle's version at build time (e.g., `[1.2,2)`). If a dependency bundle is later upgraded on the AEM instance to a version outside that range, your bundle fails to resolve — a frequent "worked yesterday, broken today after a platform update" bug.

#### 3.4 Common misconfig symptoms
| Symptom | Likely cause |
|---|---|
| Bundle shows "Installed" but not "Active" in Felix Console | Unresolved package import — check the bundle's "Resolution failed" details |
| Sling Model never gets invoked, no error | `Sling-Model-Packages` doesn't include the model's package |
| `ClassNotFoundException` at runtime despite the class being in your JAR | Package wasn't exported, or another bundle can't see it due to import/export mismatch |
| Bundle fails to resolve after an unrelated platform/library upgrade | Auto-generated version range on an imported package no longer matches the new exporter's version |

---

### 4. filevault-package-maven-plugin (formerly content-package-maven-plugin)

#### 4.1 What it does
Builds the content package `.zip` (for `ui.apps`, `ui.content`, `all`) from your `jcr_root` directory structure and `filter.xml` — this is what actually installs pages, components, configs, and design content into the JCR repository when deployed, as distinct from the bundle plugin's job of installing *code*.

#### 4.2 Key configuration

```xml
<plugin>
  <groupId>org.apache.jackrabbit</groupId>
  <artifactId>filevault-package-maven-plugin</artifactId>
  <extensions>true</extensions>
  <configuration>
    <group>com.realestate</group>
    <name>realestate.ui.apps</name>
    <packageType>application</packageType>
    <filterSource>src/main/content/META-INF/vault/filter.xml</filterSource>
  </configuration>
</plugin>
```

#### 4.3 Technicalities
- **`filter.xml` defines the "workspace filter" — the set of JCR paths this package owns and will overwrite on install.** This is the single highest-stakes piece of AEM Maven config: a filter root that's too broad (e.g., `/content` instead of `/content/properties`) can wipe out unrelated content on deploy; a filter root missing an intended path means your content silently doesn't deploy at all.
- `<packageType>` matters for AEM as a Cloud Service specifically — Cloud Manager's pipeline validates that `application` packages (code, OSGi configs, `ui.apps`) and `content` packages (`ui.content`) are correctly separated; mixing them (e.g., putting mutable author content inside an `application`-typed package) is flagged and can fail the Cloud Manager readiness check.
- Filter rules support `mode="merge"`/`mode="replace"` (and `include`/`exclude` sub-rules) — `replace` (the default) wipes and replaces everything under that root; `merge` only adds/updates what's in the package without deleting siblings not present in the package. Using `replace` on a shared root that other packages or authors also write to is a common way to accidentally delete author-created content on the next deploy.
- The `all` package typically embeds `core` (the bundle) and `ui.apps`/`ui.content` as sub-packages via `<embeddeds>`/dependency-based packaging — this is why deploying `all` installs everything in one shot, and why a broken filter in a sub-package still breaks the aggregate `all` deployment.

#### 4.4 Common misconfig symptoms
| Symptom | Likely cause |
|---|---|
| Author-created content disappears after a code deploy | Overly broad filter root using `replace` mode covering author-editable content |
| New component/template doesn't appear after deploy | Missing filter root for that specific path in `filter.xml` |
| Cloud Manager pipeline fails at "content package validation" | `application` package contains paths that should be in a `content` package, or vice versa |

---

### 5. maven-resources-plugin

#### 5.1 What it does
Copies non-Java resource files (`.content.xml`, properties files, OSGi config JSON/CFM files under `src/main/resources`) into the build output directory, so they end up correctly placed in the packaged bundle.

#### 5.2 Technicalities
- Runs in the `process-resources` phase, **before** `compile` — resource filtering (variable substitution like `${project.version}` inside a resource file) happens here if `<filtering>true</filtering>` is configured on the resource directory.
- A resource "not showing up" after build is very often not a bug in this plugin but a wrong `<resource>`/`<directory>` path in the `<build><resources>` block, or the file simply not being under a recognized resource root.

#### 5.3 Common misconfig symptoms
| Symptom | Likely cause |
|---|---|
| OSGi config file present in source but missing from deployed bundle | Not under a configured `<resources>` directory, or excluded by a resource filter pattern |
| `${project.version}` literally appears unsubstituted in a deployed file | `<filtering>` not enabled on that resource directory |

---

### 6. maven-jar-plugin

#### 6.1 What it does
Packages compiled classes + resources into a plain `.jar`, which the bundle plugin (Section 3) then post-processes to inject the OSGi manifest. Bound to the `package` phase.

#### 6.2 Technicalities
- Usually invisible/default-configured in AEM projects — you'd only touch this directly to customize manifest entries not covered by the bundle plugin, or to exclude specific files from the final JAR (`<excludes>`).
- If you ever see manifest entries that look like plain-JAR defaults (no `Import-Package`/`Export-Package`) rather than OSGi-flavored ones, it usually means the bundle plugin isn't correctly configured with `<extensions>true</extensions>`, so this plugin's plain output is what actually got deployed.

---

### 7. maven-surefire-plugin

#### 7.1 What it does
Runs unit tests (anything matching `**/*Test.java` by default) during the `test` phase — this is the plugin actually executing your JUnit/Mockito/AemContext test suite from Phases 6–9.

#### 7.2 Key configuration

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <argLine>-Xmx1024m</argLine>
    <includes>
      <include>**/*Test.java</include>
    </includes>
  </configuration>
</plugin>
```

#### 7.3 Technicalities
- `mvn package`/`mvn install` runs tests by default (via the `test` phase preceding `package` in the lifecycle) — `-DskipTests` skips execution but still compiles test classes; `-Dmaven.test.skip=true` skips both compiling and running, which is a meaningfully different (faster, but riskier) skip.
- Surefire and JaCoCo interact through the `argLine` property — JaCoCo's `prepare-agent` goal injects a `-javaagent` flag into the same `argLine` surefire uses to launch the test JVM; if your `pom.xml` manually overrides `<argLine>` elsewhere without including `@{argLine}` (the JaCoCo-populated placeholder), you silently lose coverage instrumentation with no error — a subtle, easy-to-miss interaction.
- Test JVM forking (`forkCount`) affects both speed and isolation — a shared/reused fork across many test classes can leak static state (relevant to the `MockedStatic` cleanup discipline from Phase 9.3) between test classes if not closed properly, though this is much rarer than same-class leakage.

#### 7.4 Common misconfig symptoms
| Symptom | Likely cause |
|---|---|
| JaCoCo report shows 0% coverage despite tests passing | Custom `<argLine>` override doesn't include `@{argLine}`, dropping the JaCoCo agent |
| Tests silently don't run at all | Test class naming doesn't match `<includes>` pattern (e.g., named `*Tests.java` instead of `*Test.java`) |
| OutOfMemoryError during `mvn test` on a large suite | Default heap too small for the test JVM; needs `<argLine>-Xmx...` increase |

---

### 8. maven-failsafe-plugin

#### 8.1 What it does
Runs *integration* tests (conventionally `**/*IT.java`), bound to the `integration-test`/`verify` phases — distinct from surefire's unit tests, which run earlier in the `test` phase. The separation exists because integration tests often need a running environment (a deployed AEM instance, external service) that shouldn't block a fast local `mvn test`.

#### 8.2 Technicalities
- Failsafe deliberately runs `integration-test` (execute) and `verify` (check results) as **two separate goal bindings**, so that post-integration-test cleanup (tearing down a test environment) can run even if a test failed — this is why failsafe exists as a distinct plugin from surefire rather than surefire simply also handling `*IT.java` files.
- Most AEM projects don't have integration tests configured by default from the archetype — if your project has none, this plugin may not even be present in your `pom.xml`, which is normal, not a gap.

---

### 9. sling-maven-plugin

#### 9.1 What it does
Installs/deploys the built package or bundle directly to a running AEM instance — this is the plugin actually invoked by the common `-PautoInstallPackage` / `-PautoInstallBundle` Maven profiles used during local development.

#### 9.2 Key configuration (typically inside a profile, not the default build)

```xml
<profile>
  <id>autoInstallPackage</id>
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.sling</groupId>
        <artifactId>sling-maven-plugin</artifactId>
        <configuration>
          <slingUrl>http://localhost:4502/system/console</slingUrl>
        </configuration>
        <executions>
          <execution>
            <goals>
              <goal>install</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</profile>
```

#### 9.3 Technicalities
- The `-P` flag activates a Maven **profile** — `-PautoInstallPackage` and `-PautoInstallBundle` are conventionally defined profiles in AEM archetypes that add this plugin's execution to the build only when explicitly requested, which is why a plain `mvn clean install` (no profile flag) doesn't attempt to touch a running AEM instance.
- `autoInstallBundle` deploys just the OSGi bundle (fast, for quick backend iteration); `autoInstallPackage` deploys the full content package (slower, includes content/config changes) — knowing which one to reach for materially affects local dev iteration speed.
- This plugin is irrelevant to Cloud Manager pipeline deployments — Cloud Manager uses its own deployment orchestration reading the built artifacts, not `sling-maven-plugin` against a live URL; this plugin is strictly a local/on-prem development convenience.

#### 9.4 Common misconfig symptoms
| Symptom | Likely cause |
|---|---|
| `mvn clean install -PautoInstallPackage` succeeds but nothing changes on the instance | Wrong `slingUrl` port/host, or instance not actually running |
| Deploy "succeeds" but bundle doesn't update | Used `autoInstallPackage` when only bundle code changed and `autoInstallBundle` would have been faster/more direct — usually not a failure, just an efficiency note |

---

### 10. jacoco-maven-plugin

Covered in full detail in Phase 10 of the JUnit testing guide — summarized here for completeness:

- `prepare-agent` goal must run before `surefire`/`failsafe` execution to attach the coverage-collecting Java agent.
- `report` goal generates the human-readable HTML (`target/site/jacoco/index.html`) and machine-readable XML (`jacoco.xml`, consumed by SonarQube/Cloud Manager quality gates).
- `check` goal is the actual build-failing enforcement mechanism — `report` alone does not block a bad build.
- Threshold values are fractional (0.0–1.0), not percentages — `<minimum>80</minimum>` is a common, silently-impossible-to-meet misconfiguration.

---

### 11. Full Lifecycle Walkthrough — What `mvn clean install -PautoInstallPackage` Actually Does

Useful as a single mental model tying every plugin above together, in the order things actually happen:

1. **`clean`** — deletes `target/`, removing stale compiled classes, old JaCoCo `.exec` data, and old packaged artifacts.
2. **`validate`/`initialize`** — Maven reads the POM, resolves the effective configuration (including any active profiles like `autoInstallPackage`).
3. **`process-resources`** — `maven-resources-plugin` copies non-Java resources into `target/classes`.
4. **`compile`** — `maven-compiler-plugin` compiles `.java` sources.
5. **`process-test-resources` / `test-compile`** — same as above, for test sources.
6. **`test`** — `maven-surefire-plugin` runs unit tests; if `jacoco-maven-plugin`'s `prepare-agent` ran earlier, coverage data is collected here.
7. **`package`** — `maven-jar-plugin` builds a plain JAR, then `maven-bundle-plugin`/`bnd-maven-plugin` post-processes it into an OSGi bundle (for `core`); `filevault-package-maven-plugin` builds the content package `.zip` (for `ui.apps`/`ui.content`/`all`).
8. **`integration-test` / `verify`** — `maven-failsafe-plugin` runs `*IT.java` tests if present; `jacoco-maven-plugin`'s `check` goal enforces coverage thresholds here, potentially failing the build.
9. **`install`** — the built artifact (bundle JAR or content package ZIP) is copied into your local `~/.m2` repository.
10. **(profile-only) `sling:install`** — because `-PautoInstallPackage` was passed, `sling-maven-plugin`'s bound execution now also pushes the freshly built artifact to the running AEM instance at the configured `slingUrl`.

---

### 12. Quick Reference Table

| Plugin | Phase it binds to | What breaks without it | Local-dev only? |
|---|---|---|---|
| maven-compiler-plugin | compile | Nothing compiles | No |
| maven-bundle-plugin / bnd-maven-plugin | package | No OSGi manifest — bundle won't be recognized as a bundle | No |
| filevault-package-maven-plugin | package | No deployable content package | No |
| maven-resources-plugin | process-resources | Non-Java files missing from the build | No |
| maven-jar-plugin | package | No JAR to turn into a bundle | No |
| maven-surefire-plugin | test | Unit tests never run | No |
| maven-failsafe-plugin | integration-test/verify | Integration tests never run | No |
| sling-maven-plugin | install (profile-bound) | No direct deploy to a running local instance | Yes |
| jacoco-maven-plugin | test/verify | No coverage report, no coverage gate enforcement | No |

---

**Interview framing tip:** if asked "walk me through what happens when you build an AEM project," Section 11 above is essentially the answer — being able to name the phases in order and which plugin does what at each step is what separates "I run the Maven command" from genuinely understanding the build.
