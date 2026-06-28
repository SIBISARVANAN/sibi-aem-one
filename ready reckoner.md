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
