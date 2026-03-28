
# Sling Models

Interface + Implementation class - Best practice

`@Model(adaptables = {Resource.class, SlingHttpServletRequest.class}, adapters = Author.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL, resourceType = AuthorImpl.RESOURCE_TYPE)
@Exporter(name = "jackson", selector = "model", extensions = "json", options = {@ExporterOption(name = "SerializationFeature.WRAP_ROOT_VALUE", value = "true"), @ExporterOption(name = "MapperFeature.SORT_PROPERTIES_ALPHABETICALLY", value = "true")})
@JsonRootName("AuthorDetails")
public class AuthorImpl implements Author {}`

## Adaptables 
What types of objects this model can adapt from.
Specifies the source object types that can be adapted into this sling model.
SlingHttpServletRequest.class -> when adapting from sling request.
Resource.class -> when adapting from JCR resource.

## Adapters
What types of objects this model can adapt to.
Specifies the target that this model can adapt to.
Usually the model itself is the adapter, but you can expose it as an interface.

## Annotations used

## TL;DR
Adaptables - Where the model comes from (input)
Adapters - What the model can be seen as (output)

# OSGI Configuration Registry

## Container-managed lifecycle
**core/src/main/java/com/sibi/aem/one/core/services/impl/v1/GoogleRecaptchaConfigServiceImpl.java**
This is container-managed lifecycle. - OSGi Service Tracker Pattern (DS-based)
(Bind/Unbind / Service Tracker pattern)
Here you tell OSGi:
@Reference(cardinality = MULTIPLE, policy = DYNAMIC)
Now the container (OSGi runtime):
watches the service registry
decides when a service appears/disappears
calls your bind() / unbind() methods
enforces ordering, ranking, thread safety
You are just reacting to container events.
That’s why it’s container-managed.

## Application-managed lifecycle
**core/src/main/java/com/sibi/aem/one/core/services/impl/v2/GoogleRecaptchaConfigServiceImpl.java**
This is application managed lifecycle - Self-Registering Service Pattern
Static registry is application-managed because you own the lifecycle.
Your application code decides:
when to register
where to store
how to update
how to remove
@Activate  → REGISTRY.put(this)
@Deactivate → REGISTRY.remove(this)
The container only:
creates the object
calls @Activate / @Deactivate
Everything else is your responsibility. That’s why it’s application-managed.

## TL;DR
Static registry is application-managed because you own the lifecycle.
Bind/unbind is container-managed because OSGi owns the lifecycle.

| Concept                         | Static Registry | Bind/Unbind          |
| ------------------------------- | --------------- | -------------------- |
| Who tracks instances?           | Your code       | OSGi runtime         |
| Who decides when to add/remove? | You             | Container            |
| Failure handling                | You write it    | Container handles it |
| Thread safety                   | You manage      | OSGi ensures         |
| Dynamic hot swap                | Hard            | Built-in             |

# OSGI Service Registry

## How to handle multiple implementations of a single Service?

By default, this is decided based on the ServiceID with which these implementations are registered in system/console.
Whichever implementation has lesser ServiceID gets picked and the methods in that gets executed.

## How to force another service or model to always pick one implementation over another?

Lets say you have ImplA & ImplB and you want your component to always use ImplB, then handle it using @ServiceRanking as well as the service name.
Whichever Implementation class has higher ServiceRanking will get picked by default.

### Using Service Ranking

`@Component(service = MyService.class)
@ServiceRanking(1001)
public class MyServiceImplA implements MyService{
}

@Component(service = MyService.class)
@ServiceRanking(1002)
public class MyServiceImplB implements MyService{
}`

With this setup, wherever you call MyService, ImplB will get picked since that has highest Service Ranking.

In Sling Model, use below.

`@OsgiService
private MyService service;`

In another OSGI component, use below.

`@Reference
private MyService service.`

### Using Service Name

`@Component(service = MyService.class, name="impla")
public class MyServiceImplA implements MyService{
}

@Component(service = MyService.class, name="implb")
public class MyServiceImplB implements MyService{
}`

With this setup, you get the flexibility to call whichever implementation you want.

In Sling Model, use below to call ImplA.

`@OsgiService(filter="(component.name=impla"))
private MyService service;`

In another OSGI component, use below to call ImplB.

`@Reference(target=="(component.name=implb)")
private MyService service.`

# Sling Servlets

## Version control ID for Java Serialization

`private static final long serialVersionUID = 1L;
`

It is a version control ID for a class that implements Serializable.
HttpServlet implements Serializable, so your servlet inherits that requirement.

### What problem it solves
When a Java object is serialized (converted to bytes) and later deserialized, Java must ensure:
> “Is this the same class definition that created this object?”

It checks that by comparing the serialVersionUID.
If the ID is different, Java throws: java.io.InvalidClassException

## Servlet Registry

Sling servlets can be registed in 2 ways - Path and ResourceType.
ResourceType registration is preferred and is the best practices while Path registration is very old and has its own security issues.

### ResourceType registration
**core/src/main/java/com/sibi/aem/one/core/servlets/ResourceTypeRegistrationServlet.java**

Registering the servlet based on a resource type and then create a node in JCR with this resource type.
ACLs of that node in JCR will be applied to this servlet as well.

### Path registration
**core/src/main/java/com/sibi/aem/one/core/servlets/PathRegistrationServlet.java**

Registering the servlet on a path, old way. By default, all servlets registered using paths require authentication.
You can open up servlet paths to public by adding the servlet path to SlingAuthenticator configuration like below.

**ui.config/src/main/content/jcr_root/apps/sibi-aem-one/osgiconfig/config/org.apache.sling.engine.impl.auth.SlingAuthenticator.cfg.json**

# Sling Jobs

## Multi timezone job registration

# AEM Scheduled Sling Job — End to End Flow

---

## Step 1 — OSGi config is loaded

AEM reads your `.cfg.json` file on startup or bundle deploy.
```json
{
  "enabled": true,
  "timezone1.cron": "0 0 5 * * ?",
  "timezone1.id": "America/New_York"
}
```

Each key in the JSON maps to a method in your `@interface Config` using the underscore-to-dot naming rule (`timezone1_cron()` → `"timezone1.cron"`).

---

## Step 2 — Config is injected into your component

`@Designate(ocd = Config.class)` on your `@Component` tells OSGi which `@interface` to use. OSGi calls `@Activate` and passes in the populated `Config` object.
```
.cfg.json  →  @interface Config  →  @Activate(Config config)
```

If the config is changed later in the Felix console, `@Modified` fires — the old jobs are unscheduled and new ones are registered with the updated values.

---

## Step 3 — Registrar calls JobManager

Inside `@Activate`, your `JobRegistrar` uses the `@Reference`-injected `JobManager` to register the scheduled job.
```java
jobManager
  .createJob(TOPIC)
  .properties(props)       // e.g. timezoneId, schedulerName
  .schedule()
  .cron("0 0 5 * * ?")
  .add();
```

Before calling `.add()`, always call `getScheduledJobs()` to check for duplicates — otherwise you'll register the same job multiple times on every restart.

---

## Step 4 — Job is persisted in JCR

Unlike a plain `Sling Scheduler`, a scheduled Sling Job is **written to the JCR** at:
```
/var/eventing/scheduled-jobs/
```

This means it **survives a server restart**. AEM going down and coming back up does not lose the schedule.

---

## Step 5 — Cron fires, job instance is created

When the cron expression matches the current server time, the Sling Eventing framework automatically creates a **job instance** and places it in the queue at:
```
/var/eventing/jobs/
```

The properties you attached (like `timezoneId`, `schedulerName`) are carried into this job instance.

---

## Step 6 — JobConsumer picks it up

Sling finds a registered `JobConsumer` whose `PROPERTY_TOPICS` matches the job's topic. It calls `process(job)` on it.
```java
@Component(
  service = JobConsumer.class,
  property = { JobConsumer.PROPERTY_TOPICS + "=com/example/myapp/topic" }
)
public class MyConsumer implements JobConsumer {
    public JobResult process(Job job) {
        // do work
        return JobResult.OK;
    }
}
```

In a **clustered AEM/AMS environment**, this runs on exactly one node — Sling handles the coordination automatically.

---

## Step 7 — JobResult decides what happens next

| Return | Meaning | Job removed? | Retried? |
|---|---|---|---|
| `OK` | Success | Yes | No |
| `FAILED` | Error, try again | No | Yes (with backoff) |
| `CANCEL` | Intentional abort | Yes | No |

If `FAILED` is returned, Sling retries the job automatically. The retry count and backoff delay are configurable in the OSGi job queue config at `/system/console/configMgr`.

---

## Full flow in one view
```
.cfg.json
   ↓ OSGi reads and maps keys
@interface Config
   ↓ @Designate + @Activate
JobRegistrar
   ↓ @Reference + createJob().schedule().cron().add()
JobManager
   ↓ persists to JCR
/var/eventing/scheduled-jobs   ← survives restart
   ↓ cron fires
/var/eventing/jobs             ← job instance created
   ↓ topic matched
JobConsumer.process(job)
   ↓ returns
OK → done | FAILED → retry | CANCEL → abort
```

# Sling Event Handlers

## Common Event Topics 
### OSGi `EventHandler` topics

| Event | Topic constant |
|---|---|
| Page activated/deactivated | `ReplicationAction.EVENT_TOPIC` |
| Page created/modified/deleted | `PageEvent.EVENT_TOPIC` |
| DAM asset events | `DamEvent.EVENT_TOPIC` |
| Workflow completed | `WorkflowEvent.TOPIC` |
| Bundle started/stopped | `BundleEvent.TOPIC` |

### `ResourceChangeListener` change types

| Constant | Meaning |
|---|---|
| `ADDED` | Node/resource created |
| `CHANGED` | Node/resource modified |
| `REMOVED` | Node/resource deleted |

---

## The Golden Rule — `handleEvent()` must be fast

Since the event handler service might receive a lot of events even concurrently, it is advised to set `immediate = true` on the component. Otherwise the event handler would be created and destroyed with every event coming in. 

Never do heavy work inside `handleEvent()` or `onChange()`. Always delegate to a Sling Job:
```
EventHandler.handleEvent()
└── jobManager.addJob(topic, props)   ← fire and return immediately
│
▼
JobConsumer.process(job)              ← heavy work happens here, with retry
```

|   | OSGi EventHandler  | ResourceChangeListener  |
|---|---|---|
| Use for  | System events (replication, DAM, workflow)  | JCR content/resource changes  |
| Package  | org.osgi.service.event  | org.apache.sling.api.resource.observation  |
| Status  | Current  | Current (replacement for EventHandler + resource topics)  |
| immediate=true?  |  Always |  Always |
| Heavy work inside?  | Never — delegate to Sling Job  |  Never — delegate to Sling Job |
| Deprecated alternative  | —  | EventHandler with SlingConstants resource topics  |

# Cluster-Aware Listeners

---

## What Problem Does It Solve?

In AEM AMS, author runs as a cluster (typically 2 nodes). When node 1 makes a
change, a plain `ResourceChangeListener` on node 2 will NOT receive that event.
It silently misses it. Cluster-aware listeners fix this.

---

## The Two Interfaces

`ResourceChangeListener` — listens to changes on the current node only.

`ExternalResourceChangeListener` — marker interface. Implement it alongside
`ResourceChangeListener` to also receive changes that originated on other
cluster nodes.

Both are in the package `org.apache.sling.api.resource.observation`.

---

## The One Key Method

`change.isExternal()` — tells you where the change came from.

- Returns `false` — change happened on THIS node (local)
- Returns `true`  — change happened on ANOTHER node in the cluster (external)

This is the only way to distinguish local from external events inside `onChange()`.

---

## When to Use Each Approach

**Process all changes regardless of origin** — cache invalidation, search index
updates, anything where every node must react to every change no matter where
it came from.

**Skip external changes** — background processing jobs, data sync tasks where
only one node should do the work. The node that made the change processes it;
other nodes ignore it to avoid duplicate job execution.

**Handle local and external differently** — primary data store updates on the
originating node, secondary data store sync on all other nodes.

---

## The Duplicate Job Problem

This is the most common mistake with cluster-aware listeners.

If you implement `ExternalResourceChangeListener` and fire a Sling Job in
`onChange()` without checking `isExternal()`, every node in the cluster fires
its own job for the same change. In a 2-node cluster that means 2 jobs. In a
4-node cluster, 4 jobs. All doing the same work.

Fix — check `isExternal()` before firing a job, and only fire on the node
where the change originated. Or use a Sling Job (which is cluster-aware by
nature and will run on exactly one node) and let that handle deduplication.

---

## Author Cluster vs Publish Farm

Author cluster — nodes are peers. Changes on node 1 replicate to node 2 via
Oak/Jackrabbit. `ExternalResourceChangeListener` handles this.

Publish farm — nodes are independent. They do NOT share a JCR. A change on
publish node 1 is NOT visible to publish node 2 via `ResourceChangeListener`
at all. Replication from author is the only way publish nodes get content.
`ExternalResourceChangeListener` does NOT help across publish nodes.

---

## Gotchas

**REMOVED events may be for a parent** — if `/content/mysite` is deleted, you
will not get individual REMOVED events for every child. You get one event for
the parent. Your handler must account for this by checking if the removed path
is an ancestor of your registered path, not just an exact match.

**`onChange()` must be fast** — it runs on the event thread. Any slow or
blocking operation inside it will back up the event queue for the whole
instance. Always delegate work to a Sling Job and return immediately.

**External events may arrive slightly delayed** — do not assume real-time
consistency across cluster nodes. Oak replication has a small lag. Design
your handler to be tolerant of out-of-order or slightly delayed events.

**Do not open a ResourceResolver inside onChange()** — the event thread does
not have a session. You must use a service ResourceResolver from
ResourceResolverFactory, and always close it in a finally block.

**`immediate = true` is mandatory** — without it the OSGi framework may
create and destroy the component for every event, causing missed events
during instantiation.

---

## OSGi Config Properties That Matter

`ResourceChangeListener.PATHS` — one or more JCR paths or glob patterns to
watch. Use `glob:` prefix for pattern matching. Use `.` to watch everything
(use with caution in production).

`ResourceChangeListener.CHANGES` — which change types to subscribe to. Values
are ADDED, CHANGED, REMOVED, PROVIDER_ADDED, PROVIDER_REMOVED. Only subscribe
to what you actually need.

---

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

# Sling Filters

## What Is a Sling Filter

A Sling Filter is a Java class that intercepts HTTP requests and responses in
AEM before they reach a servlet or component, and after the response is
generated. It is the AEM equivalent of a standard Java Servlet Filter and
follows the same javax.servlet.Filter contract.

## When to Use a Filter

Use a filter when you need to apply logic that cuts across many requests
without modifying individual components or servlets. Common use cases are
authentication and token validation, adding or modifying response headers,
logging and monitoring, redirects, response content modification, and
error handling.

## Old Way vs New Way

The old way uses string-based OSGi component properties inside the @Component
annotation to configure the filter. It works but is error-prone because
property names and values are plain strings with no type safety.

The new way uses the @SlingServletFilter annotation alongside @Component.
It is type-safe, cleaner to read, and is the currently recommended approach
for all new AEM development.

## The Five Filter Scopes

Scope is the most important configuration decision. It controls at what point
in the request lifecycle your filter fires.

REQUEST fires on every incoming HTTP request from a client. This is the most
commonly used scope and covers the majority of use cases including auth checks,
logging, header injection, and response modification.

INCLUDE fires when RequestDispatcher.include() is called. This happens when
one component includes another, such as a parsys including its child
components.

FORWARD fires when RequestDispatcher.forward() is called. This is less common
in AEM but can occur in error handling flows or custom routing logic.

ERROR fires when HttpServletResponse.sendError() is called or when an uncaught
Throwable propagates up from the servlet. Use this for global error handling
and graceful fallback pages.

COMPONENT is a legacy scope kept for backwards compatibility. It fires across
REQUEST, INCLUDE, and FORWARD. Avoid it in new code.

## Filter Properties

Pattern is a regex that the request path must match for the filter to fire.
If not specified, the filter applies to all paths which is almost never
what you want in production.

Extensions restricts the filter to specific file extensions such as html,
json, or xml. A filter registered for html will not fire on API calls
returning json.

Methods restricts the filter to specific HTTP methods such as GET, POST,
or HEAD. Always restrict to only the methods you actually need.

Resource types restricts the filter to requests where the resolved resource
has a specific sling:resourceType. Useful when you want a filter to fire
only for a specific component type.

Selectors restricts the filter to requests containing specific Sling
selectors in the URL.

All properties except scope are optional. Every property you add narrows
the set of requests that trigger your filter. Only requests matching all
specified conditions will fire doFilter().

## Service Ranking

Service ranking controls the order in which multiple filters execute on the
same request. It is set as an integer on the @Component annotation.

A more negative value means higher priority — the filter runs earlier in
the chain. A less negative or positive value means lower priority — the
filter runs later.

As a general guideline, authentication and security filters should run
earliest and carry the most negative ranking. Logging and monitoring filters
run in the middle range. Response modification filters run last and carry
rankings closer to zero.

If two filters have the same ranking the execution order is not guaranteed.
Always assign explicit rankings when order matters.

| Ranking  | Use for  |
|---|---|
| -100 to -500  | Auth checks, security headers  |
| -500 to -800  | Logging, monitoring  |
| -800 to -1000  | Response modification, caching  |


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

If you do not call chain.doFilter() the request is blocked entirely and
nothing further executes. The client receives only what your filter writes
to the response. This is intentional for auth filters that must return
a 401 or 403.

## Response Wrapping

If you need to read or modify the response body, you cannot do it directly
because by the time your post-processing code runs the response has already
been written. You must wrap the response before calling chain.doFilter().

A response wrapper intercepts the output that the servlet writes and buffers
it in memory. After chain.doFilter() returns you read the buffer, modify it,
and write the modified content to the real response.

Response wrapping is expensive because the full response body is held in
memory. Only use it when you genuinely need to modify the output. Never use
it on high-traffic paths without measuring the memory impact first.

## Disabling a Filter at Runtime

You can disable any filter without redeploying code by pushing an OSGi config
that sets the sling.filter.scope property to an invalid value such as
the string disabled. The Sling framework ignores filters with an unrecognised
scope and the filter stops executing immediately.

This is useful in production when a filter causes issues and you need to
turn it off quickly without a full deployment.

## Common Mistakes

Not calling chain.doFilter() by accident — if you forget to call it inside
a conditional block the request is silently blocked for matching conditions.
Always trace every code path to confirm chain.doFilter() is called when
the request should proceed.

Doing heavy work inside doFilter() — the filter runs on the request thread.
Any slow database call, HTTP call, or content traversal will block that
thread and slow down every request. Delegate heavy work to a Sling Job if
needed.

Not restricting scope with pattern and methods — a filter with no pattern
fires on every single request in AEM including internal Sling requests,
clientlib requests, and system calls. Always scope your filter as tightly
as possible.

Modifying response headers after the response is committed — once the
response is committed headers cannot be changed. Always set headers before
calling chain.doFilter() or immediately after while the response is still
open.

Not handling encoding correctly in response wrapping — always use the
response character encoding when converting bytes to strings and back.
Never hardcode UTF-8.

## Key Differences From Event Handlers and Schedulers

Filters are synchronous and run on the request thread. They must complete
quickly or they degrade request performance directly.

Event handlers and schedulers are asynchronous and run on background threads.
They are suitable for heavier work.

A filter is the right tool when you need to inspect or modify an HTTP
request or response. It is the wrong tool for background processing,
scheduled work, or reacting to JCR changes.

# Request Flow — Browser to AEM with CDN, Dispatcher and Filters

## The Full Flow

Browser sends a request for a page.

Request hits the CDN first. If the CDN has a cached copy of that page it
returns it immediately and the request never goes further. AEM, Dispatcher,
and your filters never see it.

If the CDN does not have a cached copy it forwards the request to the
Dispatcher.

Dispatcher checks its own cache on the filesystem. If it has a cached HTML
file for that URL it returns it immediately. Again, AEM and your filters
never see it.

If the Dispatcher cache does not have the page, or the cache was invalidated,
Dispatcher forwards the request to AEM publish.

The request enters AEM publish. This is where your Sling Filters fire.

AEM resolves the request path to a resource and a component, renders the
page, and sends the HTML response back to Dispatcher.

Dispatcher caches the rendered HTML on its filesystem for future requests
and returns the response to the CDN.

CDN caches the response and returns it to the browser.

Browser renders the page.

## Where Exactly Inside AEM Does the Filter Fire

Once the request enters AEM publish the processing order is as follows.

First the request goes through the Sling Authentication layer which handles
login and session resolution.

Then your REQUEST scope Sling Filters fire in service ranking order. This
is the pre-processing phase. Your filter code before chain.doFilter() runs
here.

Then Sling resolves the resource and selects the appropriate servlet or
component script to handle the request.

Then if the component includes other components via RequestDispatcher.include,
your INCLUDE scope filters fire for each inclusion.

Then the component renders the HTML and writes it to the response.

Then your REQUEST scope filter post-processing runs. Your filter code after
chain.doFilter() runs here. This is where you can add response headers or
modify the response body.

Then the response leaves AEM and goes back to Dispatcher.

## Simple Visual Flow
```
Browser
   |
   | HTTP request
   v
CDN
   |--- cache hit? --> return cached response to browser (filters never fire)
   |
   | cache miss
   v
Dispatcher
   |--- cache hit? --> return cached HTML to CDN (filters never fire)
   |
   | cache miss or invalidated
   v
AEM Publish
   |
   v
Sling Authentication
   |
   v
REQUEST Filter pre-processing   <-- your filter fires here (before chain.doFilter)
   |
   v
Sling Resource Resolution
   |
   v
Servlet / Component renders HTML
   |
   v
REQUEST Filter post-processing  <-- your filter fires here (after chain.doFilter)
   |
   v
Response leaves AEM
   |
   v
Dispatcher caches and returns to CDN
   |
   v
CDN caches and returns to Browser
   |
   v
Browser renders page
```

## What This Means Practically

Your filter only fires when the request reaches AEM. If CDN or Dispatcher
serves a cached response your filter never executes for that request.

This means you cannot use a Sling Filter to intercept every single user
request. A user who gets a CDN-cached page bypasses your filter entirely.

For logic that must run on every user request regardless of caching — such
as analytics tracking or personalisation — that logic belongs in the browser
via JavaScript, not in a Sling Filter.

For logic that only needs to run when AEM actually renders a page — such as
adding security headers to the AEM response, validating service tokens on
API calls, or modifying rendered HTML — a Sling Filter is the right place.

## Security Headers — A Special Note

If you add security headers like X-Frame-Options or Content-Security-Policy
in a Sling Filter they will only be present on responses that AEM renders.
Cached responses served by Dispatcher or CDN will not have those headers
unless you also configure Dispatcher and the CDN to add them independently.

The recommended approach in AEM AMS is to configure security headers at
the Dispatcher level using the mod_headers Apache directive so they are
present on all responses including cached ones.

## Dispatcher Cache Invalidation and Filters

When an author publishes a page, AEM sends a cache invalidation request to
Dispatcher. Dispatcher marks the cached file as invalid. The next request
for that page misses the Dispatcher cache, reaches AEM, your filter fires,
AEM renders the fresh page, and Dispatcher caches it again.

Your filter therefore fires on the first request after every publish event
and then not again until the cache is invalidated next time.
