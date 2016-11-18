/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.orchestrate.client;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import lombok.NonNull;

/**
 * A utility object to configure a Jackson JSON {@code ObjectMapper}.
 *
 * <p>Usage:
 * <pre>
 * {@code
 * JacksonMapper mapper = new JacksonMapper();
 * }
 * </pre>
 *
 * @see Builder
 */
public final class JacksonMapper {

    /** The builder for this instance of the mapper. */
    private final Builder builder;

    /**
     * Create a new {@code JacksonMapper} with default settings.
     */
    public JacksonMapper() {
        this(builder());
    }

    private JacksonMapper(final Builder builder) {
        assert (builder != null);

        this.builder = builder;
    }

    ObjectMapper getMapper() {
        return builder.objectMapper;
    }

    /**
     * A new builder to create a {@code JacksonMapper} with default settings.
     *
     * @return A new {@code Builder} with default settings.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A new builder to create a {@code JacksonMapper} from the specified
     * {@code ObjectMapper}.
     *
     * @param objectMapper A Jackson JSON {@code ObjectMapper}.
     * @return A new {@code Builder} with the specified {@code ObjectMapper}.
     */
    public static Builder builder(final @NonNull ObjectMapper objectMapper) {
        return new Builder(objectMapper);
    }

    /**
     * Builder used to create {@code JacksonMapper} instances.
     *
     * <p>Usage:
     * <pre>
     * {@code
     * JacksonMapper mapper = JacksonMapper.builder()
     *         .registerModule(new JodaModule())
     *         .enable(JsonGenerator.Feature.ESCAPE_NON_ASCII)
     *         .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
     *         .build();
     * }
     * </pre>
     */
    public static final class Builder {

        /** The configurable JSON mapper. */
        private final ObjectMapper objectMapper;

        private Builder() {
            this(new ObjectMapper());
        }

        private Builder(final ObjectMapper objectMapper) {
            assert (objectMapper != null);

            this.objectMapper = objectMapper;
        }

        /**
         * Register a new {@code Module} with the mapper.
         *
         * @param module The {@code Module} to register.
         * @return This builder.
         */
        public Builder registerModule(final @NonNull Module module) {
            objectMapper.registerModule(module);
            return this;
        }

        /**
         * Enable the {@code DeserializationFeature}(s) in the mapper.
         *
         * @param features The feature to enable in the mapper.
         * @return This builder.
         */
        public Builder enable(final @NonNull DeserializationFeature... features) {
            if (features.length < 1) {
                throw new IllegalArgumentException("'features' cannot be empty.");
            }

            for (final DeserializationFeature feature : features) {
                objectMapper.enable(feature);
            }
            return this;
        }

        /**
         * Enable the {@code MapperFeature}(s) in the mapper.
         *
         * @param features The feature to enable in the mapper.
         * @return This builder.
         */
        public Builder enable(final @NonNull MapperFeature... features) {
            if (features.length < 1) {
                throw new IllegalArgumentException("'features' cannot be empty.");
            }

            objectMapper.enable(features);
            return this;
        }

        /**
         * Enable the {@code SerializationFeature}(s) in the mapper.
         *
         * @param features The feature to enable in the mapper.
         * @return This builder.
         */
        public Builder enable(final @NonNull SerializationFeature... features) {
            if (features.length < 1) {
                throw new IllegalArgumentException("'features' cannot be empty.");
            }

            for (final SerializationFeature feature : features) {
                objectMapper.enable(feature);
            }
            return this;
        }

        /**
         * Enable the {@code JsonGenerator.Feature}(s) in the mapper.
         *
         * @param features The features to enable in the mapper.
         * @return This builder.
         */
        public Builder enable(final @NonNull JsonGenerator.Feature... features) {
            if (features.length < 1) {
                throw new IllegalArgumentException("'features' cannot be empty.");
            }

            for (final JsonGenerator.Feature feature : features) {
                objectMapper.getFactory().enable(feature);
            }
            return this;
        }

        /**
         * Enable the {@code JsonParser.Feature}(s) in the mapper.
         *
         * @param features The features to enable in the mapper.
         * @return This builder.
         */
        public Builder enable(final @NonNull JsonParser.Feature... features) {
            if (features.length < 1) {
                throw new IllegalArgumentException("'features' cannot be empty.");
            }

            for (final JsonParser.Feature feature : features) {
                objectMapper.getFactory().enable(feature);
            }
            return this;
        }

        /**
         * Disable the {@code DeserializationFeature}(s) in the mapper.
         *
         * @param features The features to disable in the mapper.
         * @return This builder.
         */
        public Builder disable(final @NonNull DeserializationFeature... features) {
            if (features.length < 1) {
                throw new IllegalArgumentException("'features' cannot be empty.");
            }

            for (final DeserializationFeature feature : features) {
                objectMapper.disable(feature);
            }
            return this;
        }

        /**
         * Disable the {@code MapperFeature}(s) in the mapper.
         *
         * @param features The features to disable in the mapper.
         * @return This builder.
         */
        public Builder disable(final @NonNull MapperFeature... features) {
            if (features.length < 1) {
                throw new IllegalArgumentException("'features' cannot be empty.");
            }

            objectMapper.disable(features);
            return this;
        }

        /**
         * Disable the {@code SerializationFeature}(s) in the mapper.
         *
         * @param features The features to disable in the mapper.
         * @return This builder.
         */
        public Builder disable(final @NonNull SerializationFeature... features) {
            if (features.length < 1) {
                throw new IllegalArgumentException("'features' cannot be empty.");
            }

            for (final SerializationFeature feature : features) {
                objectMapper.disable(feature);
            }
            return this;
        }

        /**
         * Disable the {@code JsonGenerator.Feature}(s) in the mapper.
         *
         * @param features The features to disable in the mapper.
         * @return This builder.
         */
        public Builder disable(final @NonNull JsonGenerator.Feature... features) {
            if (features.length < 1) {
                throw new IllegalArgumentException("'features' cannot be empty.");
            }

            for (final JsonGenerator.Feature feature : features) {
                objectMapper.getFactory().disable(feature);
            }
            return this;
        }

        /**
         * Disable the {@code JsonParser.Feature}(s) in the mapper.
         *
         * @param features The features to disable in the mapper.
         * @return This builder.
         */
        public Builder disable(final @NonNull JsonParser.Feature... features) {
            if (features.length < 1) {
                throw new IllegalArgumentException("'features' cannot be empty.");
            }

            for (final JsonParser.Feature feature : features) {
                objectMapper.getFactory().disable(feature);
            }
            return this;
        }

        /**
         * Creates a new {@code JacksonMapper}.
         *
         * @return A new {@link JacksonMapper}.
         */
        public JacksonMapper build() {
            return new JacksonMapper(this);
        }

    }

}
