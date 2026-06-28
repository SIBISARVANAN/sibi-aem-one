package com.sibi.aem.one.core.models.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.sibi.aem.one.core.models.ProductVariant;

import java.io.IOException;
import java.util.List;

/**
 * Custom Jackson {@link JsonSerializer} for {@code List<ProductVariant>}.
 *
 * <h3>Why a custom serializer is needed</h3>
 * <p>Default Jackson reflection would serialise every public getter on
 * {@code ProductVariant}, including internal derived fields like
 * {@code availabilityLabel} that shouldn't be part of a stable API contract.
 * A custom serializer gives full control: which fields are output, and what
 * they're named — independent of the Java getter names.</p>
 *
 * <h3>Where it's applied</h3>
 * <p>Scoped to a single field via {@code @JsonSerialize(using=...)} on
 * {@link com.sibi.aem.one.core.models.ProductSummary#variants} —
 * not registered globally, so {@code ProductVariant} serialises normally
 * anywhere else it might be used.</p>
 */
public class ProductVariantListSerializer extends JsonSerializer<List<ProductVariant>> {

    @Override
    public void serialize(List<ProductVariant> variants, JsonGenerator gen,
                          SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        if (variants != null) {
            for (ProductVariant variant : variants) {
                // PATTERN: conditional omission — e.g. "if (!variant.isInStock()) continue;"
                // is possible here but impossible with plain @JsonInclude annotations,
                // because the condition depends on a computed field, not a null check.
                gen.writeStartObject();
                gen.writeStringField("sku",    safe(variant.getVariantSku())); // renamed from variantSku
                gen.writeStringField("size",   safe(variant.getSize()));
                gen.writeStringField("colour", safe(variant.getColour()));
                gen.writeBooleanField("available", variant.isInStock());
                // Deliberately excluded: getStock() (raw count), getAvailabilityLabel() (UI string)
                gen.writeEndObject();
            }
        }
        gen.writeEndArray();
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}
