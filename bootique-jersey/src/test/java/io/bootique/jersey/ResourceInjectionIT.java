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

package io.bootique.jersey;

import io.bootique.test.junit.BQTestFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class ResourceInjectionIT {

    private static final String TEST_PROPERTY = "bq.test.label";

    @ClassRule
    public static BQTestFactory TEST_FACTORY = new BQTestFactory().autoLoadModules();

    private static InjectedService SERVICE;
    private static final WebTarget target = ClientBuilder.newClient().target("http://127.0.0.1:8080");

    @BeforeClass
    public static void startJetty() {

        SERVICE = new InjectedService();

        TEST_FACTORY.app("-s")
                .module(binder -> {
                    binder.bind(InjectedService.class).toInstance(SERVICE);
                    JerseyModule.extend(binder)
                            .addFeature(ctx -> {
                                ctx.property(TEST_PROPERTY, "x");
                                return false;
                            })
                            .addResource(FieldInjectedResource.class)
                            .addResource(ConstructorInjectedResource.class)
                            .addResource(UnInjectedResource.class);

                    binder.bind(UnInjectedResource.class).toProviderInstance(() -> new UnInjectedResource(SERVICE));

                })
                .run();
    }

    @Before
    public void before() {
        SERVICE.reset();
    }

    @Test
    public void testFieldInjected() {

        Response r1 = target.path("f").request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("f_1_x", r1.readEntity(String.class));
        r1.close();

        Response r2 = target.path("f").request().get();
        assertEquals(Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("f_2_x", r2.readEntity(String.class));
        r2.close();
    }

    @Test
    public void testConstructorInjected() {

        Response r1 = target.path("c").request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("c_1_x", r1.readEntity(String.class));
        r1.close();

        Response r2 = target.path("c").request().get();
        assertEquals(Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("c_2_x", r2.readEntity(String.class));
        r2.close();
    }

    @Test
    public void testProviderForResource() {

        Response r1 = target.path("u").request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("u_1_x", r1.readEntity(String.class));
        r1.close();

        Response r2 = target.path("u").request().get();
        assertEquals(Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("u_2_x", r2.readEntity(String.class));
        r2.close();
    }

    @Path("/f")
    @Produces(MediaType.TEXT_PLAIN)
    public static class FieldInjectedResource {

        @Inject
        private InjectedService service;

        @Context
        private Configuration config;

        @GET
        public String get() {
            return "f_" + service.getNext() + "_" + config.getProperty(TEST_PROPERTY);
        }
    }

    @Path("/c")
    @Produces(MediaType.TEXT_PLAIN)
    public static class ConstructorInjectedResource {

        private InjectedService service;

        @Context
        private Configuration config;

        @Inject
        public ConstructorInjectedResource(InjectedService service) {
            this.service = service;
        }

        @GET
        public String get() {
            return "c_" + service.getNext() + "_" + config.getProperty(TEST_PROPERTY);
        }
    }

    @Path("/u")
    @Produces(MediaType.TEXT_PLAIN)
    public static class UnInjectedResource {

        private InjectedService service;

        @Context
        private Configuration config;

        public UnInjectedResource(InjectedService service) {
            this.service = service;
        }

        @GET
        public String get() {
            return "u_" + service.getNext() + "_" + config.getProperty(TEST_PROPERTY);
        }
    }

    public static class InjectedService {

        private AtomicInteger atomicInt = new AtomicInteger();

        public void reset() {
            atomicInt.set(0);
        }

        public int getNext() {
            return atomicInt.incrementAndGet();
        }
    }
}
