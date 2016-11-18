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

/**
 * An abstract adapter class for receiving client responses.
 *
 * <p>The methods in this class are empty. This class exists as convenience for
 * creating listener objects.
 *
 * @param <T> The deserializable type for the value of this response.
 */
public abstract class ResponseAdapter<T> implements ResponseListener<T> {

    /** {@inheritDoc} */
    @Override
    public void onFailure(final Throwable error) {
    }

    /** {@inheritDoc} */
    @Override
    public void onSuccess(final T object) {
    }

}
