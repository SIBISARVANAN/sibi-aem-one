# Sibi AEM One

A sample AEM (Adobe Experience Manager) project built to demonstrate hands-on, production-grade AEM backend development — built on the standard AEM project archetype, covering the patterns and internals a Senior AEM Developer is expected to know beyond day-to-day project scope.

Built by [Sibi Sarvanan](https://github.com/SIBISARVANAN), Senior AEM Developer (~8 years, backend/APIs focus).

> Looking for the conceptual deep-dives and interview reference notes instead of code? See [sibi-aem-playbook](https://github.com/SIBISARVANAN/sibi-aem-playbook).

## What this project demonstrates

| Area | Where |
| --- | --- |
| OSGi service patterns — container-managed (bind/unbind) vs application-managed (self-registering) lifecycle | `core/src/main/java/com/sibi/aem/one/core/services/impl/v1`, `v2` |
| Servlet registration — resourceType vs path-based | `core/src/main/java/com/sibi/aem/one/core/servlets` |
| Sling Jobs — cron scheduling, JCR persistence, retry/backoff | `core` |
| OSGi Configuration & Sling Authentication handling | `ui.config` |
| Front-end build & AEM ClientLib integration | `ui.frontend`, `ui.apps` |
| JUnit testing — OSGi service mocking, ResourceResolver/QueryBuilder mocking, Mockito patterns | `core/src/test` |

## Modules

- **core** — OSGi services, listeners, schedulers, servlets, request filters
- **it.tests** — Java-based integration tests (AEM Testing Clients)
- **ui.apps** — `/apps` content: clientlibs, components, templates
- **ui.content** — sample content built on the `ui.apps` components
- **ui.config** — runmode-specific OSGi configs
- **ui.frontend** — dedicated front-end build (Webpack-based)
- **ui.tests** — Cypress-based UI tests
- **all** — single package embedding all compiled modules
- **analyse** — AEMaaCS deployment validation

## How to build

```bash
mvn clean install
```

Deploy to a local author instance:

```bash
mvn clean install -PautoInstallSinglePackage
```

Deploy to a local publish instance:

```bash
mvn clean install -PautoInstallSinglePackagePublish
```

Deploy only the bundle to author:

```bash
mvn clean install -PautoInstallBundle
```

## Testing

```bash
mvn clean test          # unit tests
mvn clean verify -Plocal  # integration tests against a running AEM instance
```

## Related

- [sibi-aem-playbook](https://github.com/SIBISARVANAN/sibi-aem-playbook) — deep-dive conceptual notes on OSGi internals, Sling request handling, Oak/JCR, and AEM testing patterns, built alongside this project.

---

*This repository is shared publicly for portfolio and interview purposes. Please do not copy or redistribute without permission.*
