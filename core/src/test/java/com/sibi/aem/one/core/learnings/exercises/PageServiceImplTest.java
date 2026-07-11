//package com.sibi.aem.one.core.learnings.exercises;
//
//import com.day.cq.search.PredicateGroup;
//import com.day.cq.search.Query;
//import com.day.cq.search.QueryBuilder;
//import com.day.cq.search.result.Hit;
//import com.day.cq.search.result.SearchResult;
//import io.wcm.testing.mock.aem.junit5.AemContext;
//import io.wcm.testing.mock.aem.junit5.AemContextExtension;
//import org.apache.sling.api.resource.Resource;
//import org.apache.sling.api.resource.ResourceResolver;
//import org.apache.sling.testing.mock.sling.ResourceResolverType;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//
//import javax.jcr.Session;
//import java.util.Collections;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(AemContextExtension.class)
//class PageServiceImplTest {
//
//    // Using JCR_MOCK to ensure proper JCR node structures (required for Pages/Assets)
//    private final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);
//    private PageServiceImpl pageService;
//
//    @BeforeEach
//    void setUp() {
//        // 1. Create a page
//        context.create().page("/content/test-page", "default-template", "Test Page Title");
//
//        // 2. Create an asset (using the shorthand for a dummy image)
//        context.create().asset("/content/dam/test-image.jpg", 100, 100, "image/jpeg");
//        context.create().resource("/content/dam/test-image.jpg/jcr:content/metadata",
//                "dc:title", "TEST-IMAGE");
//
//        // 3. Create a page that points to the asset
//        context.create().page("/content/page-with-thumb", "default-template", "Thumb Page");
//        context.create().resource("/content/page-with-thumb/jcr:content",
//                "thumbnailPath", "/content/dam/test-image.jpg");
//
//        // 4. Register the service
//        pageService = context.registerInjectActivateService(new PageServiceImpl());
//    }
//
//    @Test
//    void testGetTitle() {
//        assertEquals("Test Page Title", pageService.getTitle("/content/test-page"));
//    }
//
//    @Test
//    void testGetThumbnail() {
//        assertEquals("/content/dam/test-image.jpg", pageService.getThumbnail("/content/page-with-thumb"));
//    }
//
//    @Test
//    void testGetThumbnailTitle() {
//        assertEquals("TEST-IMAGE", pageService.getThumbnailTitle("/content/page-with-thumb"));
//    }
//
//    @Test
//    void testSearchPageWithTitle() throws Exception {
//        // 1. Setup the Page and Title in the AemContext (as you did)
//        // Ensure the resource exists so PageManager can actually find it
//        Resource pageResource = context.resourceResolver().getResource("/content/test-page");
//
//        // 2. Mock the QueryBuilder
//        QueryBuilder mockQueryBuilder = mock(QueryBuilder.class);
//        Query mockQuery = mock(Query.class);
//        SearchResult mockResult = mock(SearchResult.class);
//        Hit mockHit = mock(Hit.class);
//
//        // 3. Define behavior
//        // Mock the chain: QueryBuilder -> Query -> SearchResult -> Hit -> Resource
//        when(mockQueryBuilder.createQuery(any(PredicateGroup.class), any(Session.class)))
//                .thenReturn(mockQuery);
//        when(mockQuery.getResult()).thenReturn(mockResult);
//        when(mockResult.getHits()).thenReturn(Collections.singletonList(mockHit));
//        when(mockHit.getResource()).thenReturn(pageResource);
//
//        // 4. IMPORTANT: Register the Mocked QueryBuilder AND a Mock Session
//        context.registerService(QueryBuilder.class, mockQueryBuilder);
//        context.registerAdapter(ResourceResolver.class, Session.class, mock(Session.class));
//
//        // 5. Register/Re-activate the service
//        pageService = context.registerInjectActivateService(new PageServiceImpl());
//
//        // 6. Run the test
//        String path = pageService.searchPageWithTitle("Test Page Title");
//        System.out.println("Result from service: " + path);
//        // 7. Verify
//        assertEquals("/content/test-page", path);
//    }
//}