/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.bootique.jersey.jackson;

import io.bootique.BQCoreModule;
import io.bootique.di.Binder;

/**
 * @since 2.0
 */
public class JerseyJacksonModuleExtender {

    private Binder binder;

    public JerseyJacksonModuleExtender(Binder binder) {
        this.binder = binder;
    }

    /**
     * Configures global JSON serialization strategy that would skip properties with null values in responses.
     *
     * @return this extender instance
     */
    public JerseyJacksonModuleExtender skipNullProperties() {
        BQCoreModule.extend(binder).setProperty("bq.jerseyjackson.skipNullProperties", "true");
        return this;
    }
}
