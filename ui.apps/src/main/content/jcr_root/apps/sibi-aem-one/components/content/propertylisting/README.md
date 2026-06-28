# Property Listing Component — Feature Map

Real-world scenario: a real-estate **Property Listing** component. Every requested
Touch UI / Coral UI 3 field type and layout is demonstrated somewhere in this
dialog — this README maps each one to its exact location.

## Feature → Location Map

| Requested Feature | Where it is | Notes |
|---|---|---|
| Multifield | `gallery` field, Tab 3 | Composite multifield: image + caption per row |
| Nested multifield | `rooms` → `features`, Tab 3 | `Room.java` adapts its own `@ChildResource("features")` → `RoomFeature.java`. **Caveat:** nested composite multifields have had authoring-UI quirks on some AEM versions (drag-reorder, add/remove inside inner rows) — test on your target version before relying on this in production. |
| Dropdown showhide | `status` select, Tab 1 | `granite:class="cq-dialog-dropdown-showhide"` + per-option `showhidetargetvalue` |
| Checkbox showhide | `featured` checkbox, Tab 1 | `granite:class="cq-dialog-checkbox-showhide"` — both are **built-in** AEM clientlibs (`cq.authoring.dialog`), no custom JS needed |
| Heading | `heading`/`heading2` etc. | `foundation/heading` |
| Text (static) | `introText` | `foundation/text` |
| Separator | `separator1`–`separator5` | `foundation/separator` |
| Textfield | `title`, `subtitle`, `roomName`, etc. | `foundation/form/textfield` |
| Textarea | `shortDescription` | `foundation/form/textarea` |
| RTE (advanced) | `fullDescription`, Tab 2 | Table, paraformat with custom H2/H3/blockquote, anchors, source-edit, fullscreen toolbar, paste rules |
| Pathfield | `locationPage` | Generic, rootPath only |
| Pathbrowser variant | `agentPage` | Constrained rootPath + filter/predicate |
| Image drag & drop | `gallery` → `image` | `foundation/form/fileupload` with `dropZone="true"` |
| Checkbox | `featured`, `published` | `foundation/form/checkbox` |
| Radio buttons | `contactPreference` | `foundation/form/radiogroup` + `radio` |
| Select (static) | `roomType` | `foundation/form/select` with static `<items>` |
| Select — custom servlet datasource | `propertyType` | Points to `sibi-aem-one/datasources/propertytype` → `PropertyTypeDataSourceServlet.java` |
| Select — ACS Commons generic list datasource | `currency` | See **ACS Commons caveat** below |
| Colorfield | `badgeColor` | Plain `colorfield`, `variant="swatch"` |
| Color swatch — custom palette | `themeColor` | Custom `<items>` child nodes define a curated brand palette, `showDefaultColors="false"` |
| Datepicker | `soldDate`, `availableFrom` | `foundation/form/datepicker` |
| Datepicker "range" | `openHouseStart` / `openHouseEnd` | Paired fields — see **Date range caveat** below |
| Tagpicker | `amenities` | `cq/gui/components/coral/foundation/form/tagfield` |
| Tabs (root layout) | Dialog root | `foundation/layouts/tabs` |
| Fixedcolumns | Tab 1 | `foundation/fixedcolumns`, 2 columns |
| Accordion | Tab 5 | `foundation/accordion`, 2 sections |
| Popover | Tab 4, next to Price | Help button triggers `foundation/popover` via `granite:data target` |
| Wizard | Not in main dialog (alternate root) | See snippet below |
| Carousel | Not in main dialog (alternate root) | See snippet below |

## Why Wizard & Carousel aren't in the main dialog

A Granite UI dialog can only have **one root layout container**. This dialog
uses **Tabs** as the root because it's the most common real-world choice for
a content-heavy component. Wizard and Carousel are genuine alternate root
layouts — shown here as standalone snippets so you can see the syntax without
conflicting with the Tabs root above.

### Wizard (alternate root — step-by-step dialog)

```xml
<content jcr:primaryType="nt:unstructured"
    sling:resourceType="granite/ui/components/coral/foundation/container">
  <items jcr:primaryType="nt:unstructured">
    <wizard jcr:primaryType="nt:unstructured"
        sling:resourceType="granite/ui/components/coral/foundation/layouts/wizard">
      <items jcr:primaryType="nt:unstructured">
        <step1 jcr:primaryType="nt:unstructured" jcr:title="Basic Info"
            sling:resourceType="granite/ui/components/coral/foundation/container">
          <!-- fields for step 1 -->
        </step1>
        <step2 jcr:primaryType="nt:unstructured" jcr:title="Pricing"
            sling:resourceType="granite/ui/components/coral/foundation/container">
          <!-- fields for step 2 -->
        </step2>
      </items>
    </wizard>
  </items>
</content>
```

Use Wizard when the dialog represents a genuine sequential process (e.g. a
multi-step onboarding form) where step N shouldn't be visible until step N-1
is complete. It's rare in standard component dialogs — most projects use Tabs.

### Carousel (alternate root — swipeable panels)

```xml
<content jcr:primaryType="nt:unstructured"
    sling:resourceType="granite/ui/components/coral/foundation/container">
  <items jcr:primaryType="nt:unstructured">
    <carousel jcr:primaryType="nt:unstructured"
        sling:resourceType="granite/ui/components/coral/foundation/layouts/carousel">
      <items jcr:primaryType="nt:unstructured">
        <panel1 jcr:primaryType="nt:unstructured" jcr:title="Slide 1"
            sling:resourceType="granite/ui/components/coral/foundation/container">
          <!-- fields -->
        </panel1>
      </items>
    </carousel>
  </items>
</content>
```

Carousel is functionally very similar to Tabs but renders as swipeable
panels with dot-indicators instead of a tab strip — mostly seen in
mobile-first authoring contexts. Genuinely rare in production dialogs.

## ACS Commons Generic List caveat

The `currency` field's datasource resourceType
(`acs-commons/components/utilities/genericlist/datasource`) and the
`listPath` attribute name are correct for recent ACS Commons releases, but
**this has changed across ACS Commons versions** (the storage location moved
from `/etc/acs-commons/lists/` to `/conf/...` in some releases, and the exact
resourceType string has had minor revisions). Before using this in a real
project: check the ACS Commons version pinned in your `pom.xml` and confirm
against that version's documentation. You'll also need to actually author
the generic list content page (e.g. list the currency codes/names) under
the path you reference — the datasource doesn't create it for you.

## Date range caveat

AEM has **no native single "date range" widget**. The real-world pattern —
used here for the Open House window — is two separate `datepicker` fields
(start/end), with the relationship between them validated in your Sling
Model (`PropertyListingImpl.validateOpenHouseRange()`) and/or with a small
custom client-side validator registered via `Granite.UI.Foundation.Validator`
if you want the author to see an inline error before saving. This project
demonstrates the server-side half only, since that's the half every project
needs regardless of whether you add client-side validation too.

## Files in this package

```
ui.apps/.../propertylisting/.content.xml          component definition
ui.apps/.../propertylisting/_cq_dialog/.content.xml   the full dialog (main deliverable)
ui.apps/.../propertylisting/propertylisting.html  HTL render script
core/.../models/propertylisting/PropertyListing.java       interface
core/.../models/propertylisting/PropertyListingImpl.java   implementation
core/.../models/propertylisting/GalleryImage.java          flat multifield child model
core/.../models/propertylisting/Room.java                  nested multifield mid-level model
core/.../models/propertylisting/RoomFeature.java            nested multifield leaf model
core/.../datasources/PropertyTypeDataSourceServlet.java     custom servlet datasource
```
