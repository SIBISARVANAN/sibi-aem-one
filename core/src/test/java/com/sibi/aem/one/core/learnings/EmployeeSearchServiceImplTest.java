package com.sibi.aem.one.core.learnings;

import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import org.apache.commons.collections.ListUtils;
import org.apache.sling.api.resource.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EmployeeSearchServiceImplTest {

    @Mock
    ResourceResolverFactory resourceResolverFactory;

    @Mock
    ResourceResolver resourceResolver;

    @Mock
    Session session;

    @Mock
    QueryBuilder queryBuilder;

    @Mock
    Query query;

    @Mock
    SearchResult searchResult;

    @Mock
    Hit hit1;

    @Mock
    Hit hit2;

    @Mock
    Hit hit3;

    @Mock
    Resource resource1;

    @Mock
    Resource resource2;

    @Mock
    Resource resource3;

    @Mock
    ValueMap valueMap1;

    @Mock
    ValueMap valueMap2;

    @Mock
    ValueMap valueMap3;

    @InjectMocks
    EmployeeSearchServiceImpl employeeSearchServiceImpl;

    @BeforeEach
    void setupCommonMocks() throws LoginException {
        lenient().when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenReturn(resourceResolver);
        lenient().when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        lenient().when(queryBuilder.createQuery(any(), eq(session))).thenReturn(query);
        lenient().when(query.getResult()).thenReturn(searchResult);
    }

    @Test
    void getEmployeeNames() throws LoginException, RepositoryException {
        when(searchResult.getHits()).thenReturn(List.of(hit1, hit2, hit3));
        when(hit1.getResource()).thenReturn(resource1);
        when(resource1.getValueMap()).thenReturn(valueMap1);
        when(valueMap1.get("name", String.class)).thenReturn("Sibi");
        when(hit2.getResource()).thenReturn(resource2);
        when(resource2.getValueMap()).thenReturn(valueMap2);
        when(valueMap2.get("name", String.class)).thenReturn("John");
        when(hit3.getResource()).thenReturn(resource3);
        when(resource3.getValueMap()).thenReturn(valueMap3);
        when(valueMap3.get("name", String.class)).thenReturn("Alice");
        List<String> names = employeeSearchServiceImpl.getEmployeeNames();
        assertEquals(List.of("Sibi", "John", "Alice"), names);
    }

    @Test
    void getEmptySearchResults() throws LoginException {
        List<Hit>  hits = new ArrayList<>();
        when(searchResult.getHits()).thenReturn(hits);
        List<String> names = employeeSearchServiceImpl.getEmployeeNames();
        assertTrue(names.isEmpty());
    }

    @Test
    void getEmptyResultsFromException() throws LoginException {
        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenThrow(new LoginException());
        List<String> names = employeeSearchServiceImpl.getEmployeeNames();
        assertTrue(names.isEmpty());
    }

    @Test
    void getEmployeeNamesFiltered() throws LoginException, RepositoryException {
        when(searchResult.getHits()).thenReturn(List.of(hit1, hit2, hit3));
        when(hit1.getResource()).thenReturn(resource1);
        when(resource1.getValueMap()).thenReturn(valueMap1);
        when(valueMap1.get("name", String.class)).thenReturn("Sibi");
        when(hit2.getResource()).thenReturn(resource2);
        when(resource2.getValueMap()).thenReturn(valueMap2);
        //when(valueMap2.get("name", String.class)).thenReturn("John");
        when(hit3.getResource()).thenReturn(resource3);
        when(resource3.getValueMap()).thenReturn(valueMap3);
        when(valueMap3.get("name", String.class)).thenReturn("Alice");
        List<String> names = employeeSearchServiceImpl.getEmployeeNames();
        assertEquals(List.of("Sibi", "Alice"), names);
        assertEquals(2, names.size());
    }


}
