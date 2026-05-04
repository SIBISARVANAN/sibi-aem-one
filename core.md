# AEM Core Concepts — Developer Reference

> A structured technical reference for all core AEM/OSGi/Sling patterns implemented in this project.

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
