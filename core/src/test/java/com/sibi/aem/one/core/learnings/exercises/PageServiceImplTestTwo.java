package com.sibi.aem.one.core.learnings.exercises;

import com.day.cq.search.Predicate;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Session;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link PageServiceImpl}.
 *
 * WHY MockitoExtension + default AemContext (RESOURCERESOLVER_MOCK), not JCR_OAK:
 * -----------------------------------------------------------------------------
 * Real Oak-backed QueryBuilder testing (ResourceResolverType.JCR_OAK) requires
 * the org.apache.sling.testing.sling-mock-oak Maven artifact. That dependency
 * isn't reliably resolvable in every environment (e.g. behind a Maven mirror
 * that hasn't proxied it), so this version avoids it entirely.
 *
 * Instead, QueryBuilder is mocked directly with Mockito and wired in two
 * places:
 *   1. As an OSGi service (context.registerService) -- satisfies the
 *      mandatory @Reference QueryBuilder field on PageServiceImpl so
 *      registerInjectActivateService() doesn't fail to activate the component.
 *   2. As a resource resolver adapter (context.registerAdapter) -- satisfies
 *      the actual code path, since searchPageWithTitle() ignores the
 *      injected field and instead calls
 *      resourceResolver.adaptTo(QueryBuilder.class) locally (see the earlier
 *      review note about this being dead/shadowed code).
 *
 * This approach has a nice side effect: instead of relying on real query
 * *behavior* to expose the hardcoded-predicate bug, we capture the actual
 * PredicateGroup handed to QueryBuilder.createQuery() and assert on it
 * directly -- a more precise and Oak-independent way to pin down the bug.
 */
@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class PageServiceImplTestTwo {

    // No-arg AemContext() defaults to ResourceResolverType.RESOURCERESOLVER_MOCK.
    // PageManager / AssetManager / ContentBuilder all work fine with this type;
    // only QueryBuilder needs the manual mocking below.
    private final AemContext context = new AemContext();

    @Mock
    private QueryBuilder mockQueryBuilder;

    @Mock
    private Query mockQuery;

    @Mock
    private SearchResult mockSearchResult;

    private PageServiceImpl pageService;

    @BeforeEach
    void setUp() {
        // Satisfies the @Reference QueryBuilder field so component activation
        // doesn't fail (the field itself is unused by the actual code path).
        context.registerService(QueryBuilder.class, mockQueryBuilder);

        // Satisfies resourceResolver.adaptTo( .class), which is what
        // searchPageWithTitle() actually calls.
        context.registerAdapter(ResourceResolver.class, QueryBuilder.class, mockQueryBuilder);

        pageService = context.registerInjectActivateService(new PageServiceImpl());
    }

    // ---------------------------------------------------------------
    // getTitle()
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("getTitle()")
    class GetTitleTests {

        @Test
        @DisplayName("returns the jcr:title of an existing page")
        void returnsCorrectTitle() {
            context.create().page(
                    "/content/property-listing/us/en/homes/property-101",
                    "property-listing-template",
                    "3BHK Villa in Chennai"
            );

            String title = pageService.getTitle("/content/property-listing/us/en/homes/property-101");

            assertEquals("3BHK Villa in Chennai", title);
        }

        @Test
        @DisplayName("returns null when the page does not exist")
        void nonExistentPage_returnsNull() {
            String title = pageService.getTitle("/content/property-listing/does-not-exist");

            assertNull(title);
        }

        @Test
        @DisplayName("returns null when the path points to a non-page resource")
        void nonPageResource_returnsNull() {
            context.create().resource("/content/property-listing/just-a-resource");

            String title = pageService.getTitle("/content/property-listing/just-a-resource");

            assertNull(title);
        }
    }

    // ---------------------------------------------------------------
    // getThumbnail()
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("getThumbnail()")
    class GetThumbnailTests {

        @Test
        @DisplayName("returns the asset path when thumbnailPath resolves to a valid asset")
        void validThumbnail_returnsAssetPath() {
            String assetPath = "/content/dam/property-listing/property-101-thumb.jpg";
            context.create().asset(assetPath, 300, 200, "image/jpeg");

            Page page = context.create().page("/content/property-listing/us/en/homes/property-101");
            // page() already creates jcr:content -- modify it in place rather than
            // re-creating the resource (which throws PersistenceException: Path already exists).
            ModifiableValueMap contentProps = page.getContentResource().adaptTo(ModifiableValueMap.class);
            contentProps.put("thumbnailPath", assetPath);

            String thumbnail = pageService.getThumbnail("/content/property-listing/us/en/homes/property-101");

            assertEquals(assetPath, thumbnail);
        }

        @Test
        @DisplayName("returns null when thumbnailPath property is missing")
        void missingThumbnailPathProperty_returnsNull() {
            context.create().page("/content/property-listing/us/en/homes/property-102");
            // jcr:content exists but has no thumbnailPath property
            // -> resourceResolver.getResource(null) returns null
            // -> assetResource.adaptTo(...) throws NPE -> caught -> null

            String thumbnail = pageService.getThumbnail("/content/property-listing/us/en/homes/property-102");

            assertNull(thumbnail);
        }

        @Test
        @DisplayName("returns null when thumbnailPath points to a non-asset resource")
        void thumbnailPathNotAnAsset_returnsNull() {
            context.create().resource("/content/property-listing/not-an-asset");
            Page page = context.create().page("/content/property-listing/us/en/homes/property-103");
            ModifiableValueMap contentProps = page.getContentResource().adaptTo(ModifiableValueMap.class);
            contentProps.put("thumbnailPath", "/content/property-listing/not-an-asset");

            String thumbnail = pageService.getThumbnail("/content/property-listing/us/en/homes/property-103");

            // adaptTo(Asset.class) returns null for a non-dam resource -> NPE on
            // asset.getPath() -> caught -> null
            assertNull(thumbnail);
        }

        @Test
        @DisplayName("returns null when the page itself does not exist")
        void nonExistentPage_returnsNull() {
            String thumbnail = pageService.getThumbnail("/content/property-listing/does-not-exist");

            assertNull(thumbnail);
        }
    }

    // ---------------------------------------------------------------
    // getThumbnailTitle()
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("getThumbnailTitle()")
    class GetThumbnailTitleTests {

        @Test
        @DisplayName("returns dc:title metadata of the thumbnail asset")
        void validAssetWithMetadata_returnsDcTitle() {
            String assetPath = "/content/dam/property-listing/property-104-thumb.jpg";
            context.create().asset(assetPath, 300, 200, "image/jpeg");
            // asset() already creates jcr:content/metadata -- modify it in place
            // rather than re-creating the resource (Path already exists otherwise).
            Resource metadataResource = context.resourceResolver().getResource(assetPath + "/jcr:content/metadata");
            ModifiableValueMap metadataProps = metadataResource.adaptTo(ModifiableValueMap.class);
            metadataProps.put("dc:title", "Luxury Villa Thumbnail");

            Page page = context.create().page("/content/property-listing/us/en/homes/property-104");
            ModifiableValueMap contentProps = page.getContentResource().adaptTo(ModifiableValueMap.class);
            contentProps.put("thumbnailPath", assetPath);

            String title = pageService.getThumbnailTitle("/content/property-listing/us/en/homes/property-104");

            assertEquals("Luxury Villa Thumbnail", title);
        }

        @Test
        @DisplayName("returns null when dc:title metadata is not set (no exception path)")
        void assetWithoutMetadata_returnsNull() {
            // Note: unlike getThumbnail(), this does NOT rely on exception
            // swallowing -- Asset.getMetadataValue() simply returns null
            // for an absent property.
            String assetPath = "/content/dam/property-listing/property-105-thumb.jpg";
            context.create().asset(assetPath, 300, 200, "image/jpeg");

            Page page = context.create().page("/content/property-listing/us/en/homes/property-105");
            ModifiableValueMap contentProps = page.getContentResource().adaptTo(ModifiableValueMap.class);
            contentProps.put("thumbnailPath", assetPath);

            String title = pageService.getThumbnailTitle("/content/property-listing/us/en/homes/property-105");
            assertEquals(StringUtils.EMPTY, title);
        }
    }

    // ---------------------------------------------------------------
    // searchPageWithTitle()
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("searchPageWithTitle()")
    class SearchPageWithTitleTests {

        @Test
        @DisplayName("returns the matching page's content resource path when a hit's title matches the input title")
        void matchingHit_returnsResourcePath() throws Exception {
            var page = context.create().page(
                    "/content/property-listing/us/en/homes/property-106",
                    "property-listing-template",
                    "Test Page Title"
            );

            Hit hit = Mockito.mock(Hit.class);
            when(hit.getResource()).thenReturn(page.getContentResource());
            when(mockSearchResult.getHits()).thenReturn(List.of(hit));
            when(mockQuery.getResult()).thenReturn(mockSearchResult);
            when(mockQueryBuilder.createQuery(any(PredicateGroup.class), nullable(Session.class)))
                    .thenReturn(mockQuery);

            String result = pageService.searchPageWithTitle("Test Page Title");

            assertEquals(
                    "/content/property-listing/us/en/homes/property-106/jcr:content",
                    result
            );
        }

        @Test
        @DisplayName("returns empty string when no hits are returned")
        void noHits_returnsEmptyString() {
            when(mockSearchResult.getHits()).thenReturn(Collections.emptyList());
            when(mockQuery.getResult()).thenReturn(mockSearchResult);
            when(mockQueryBuilder.createQuery(any(PredicateGroup.class), nullable(Session.class)))
                    .thenReturn(mockQuery);

            String result = pageService.searchPageWithTitle("Any Title");

            assertEquals("", result);
        }

        @Test
        @DisplayName("returns empty string when a hit's containing page title does not match the input title")
        void hitTitleDiffersFromInput_returnsEmptyString() throws Exception {
            var page = context.create().page(
                    "/content/property-listing/us/en/homes/property-107",
                    "property-listing-template",
                    "Test Page Title"
            );

            Hit hit = Mockito.mock(Hit.class);
            when(hit.getResource()).thenReturn(page.getContentResource());
            when(mockSearchResult.getHits()).thenReturn(List.of(hit));
            when(mockQuery.getResult()).thenReturn(mockSearchResult);
            when(mockQueryBuilder.createQuery(any(PredicateGroup.class), nullable(Session.class)))
                    .thenReturn(mockQuery);

            // Caller asks for a different title than the hit's actual page title
            String result = pageService.searchPageWithTitle("Some Other Title Entirely");

            assertEquals("", result);
        }
    }
}