package com.sibi.aem.one.core.models.impl;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMMode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.sibi.aem.one.core.models.Author;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.models.annotations.*;
import org.apache.sling.models.annotations.injectorspecific.*;
import org.osgi.resource.Resource;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

@Model(adaptables = {Resource.class, SlingHttpServletRequest.class}, adapters = Author.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL, resourceType = AuthorImpl.RESOURCE_TYPE)
@Exporter(name = "jackson", selector = "model", extensions = "json", options = {@ExporterOption(name = "SerializationFeature.WRAP_ROOT_VALUE", value = "true"), @ExporterOption(name = "MapperFeature.SORT_PROPERTIES_ALPHABETICALLY", value = "true")})
@JsonRootName("AuthorDetails")
public class AuthorImpl implements Author {

    static final String RESOURCE_TYPE = "sibi-aem-one/components/content/author";

    @Inject
    @Default(values = "Sibi")
    @JsonProperty("firstName")
    String firstName;

    @Inject
    @Default(values = "Sarvanan")
    @JsonProperty("lastName")
    String lastName;

    @Inject
    @Default(values = "Mr")
    @Named("author:title")
    @JsonProperty("authorTitle")
    String title;

    @ValueMapValue
    @Default(values = "Male")
    @JsonProperty("gender")
    String gender;

    @ValueMapValue
    @Default(values = "sibi.sarvanan@yopmail.com")
    @JsonProperty("email")
    String email;

    @ValueMapValue
    @Named("jcr:lastModified")
    @JsonProperty("lastModified")
    String lastModified;

    @RequestAttribute(name = "reqAttr")
    String reqAttr;

    @ResourcePath(path = "/content/sibi-aem-one/any-page-path")
    Resource anyPageResource;

    @Self
    private Resource resource;

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    private ResourceResolver resourceResolver;

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    private WCMMode wcmMode;

    @SlingObject
    private ResourceResolverFactory resourceResolverFactory;

    @SlingObject
    private SlingHttpServletResponse response;

    @PostConstruct
    protected void init() {
        // type any code to be executed once after all injections are over
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getGender() {
        return gender;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getTitle() {
        return title;
    }
}
