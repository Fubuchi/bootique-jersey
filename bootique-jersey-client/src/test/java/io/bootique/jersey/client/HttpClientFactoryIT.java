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

package io.bootique.jersey.client;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jersey.JerseyModule;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.logback.LogbackModuleProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class HttpClientFactoryIT {

    @BQApp
    static final BQRuntime server = Bootique.app("--server")
            .modules(JettyModule.class, JerseyModule.class)
            .moduleProvider(new LogbackModuleProvider())
            .module(b -> JerseyModule.extend(b).addResource(Resource.class))
            .module(JettyTester.moduleReplacingConnectors())
            .createRuntime();

    @RegisterExtension
    public BQTestFactory clientFactory = new BQTestFactory();

    @Test
    public void testNewClient() {
        HttpClientFactory factory =
                clientFactory.app()
                        .moduleProvider(new JerseyClientModuleProvider())
                        .moduleProvider(new LogbackModuleProvider())
                        .createRuntime()
                        .getInstance(HttpClientFactory.class);

        Client client = factory.newClient();

        Response r1 = client.target(JettyTester.getServerUrl(server)).path("get").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("got", r1.readEntity(String.class));
    }

    @Test
    public void testNewClientAuth() {
        HttpClientFactory factory =
                clientFactory.app()
                        .moduleProvider(new JerseyClientModuleProvider())
                        .moduleProvider(new LogbackModuleProvider())
                        .property("bq.jerseyclient.auth.auth1.type", "basic")
                        .property("bq.jerseyclient.auth.auth1.username", "u")
                        .property("bq.jerseyclient.auth.auth1.password", "p")
                        .createRuntime()
                        .getInstance(HttpClientFactory.class);

        Client client = factory.newBuilder().auth("auth1").build();

        Response r1 = client.target(JettyTester.getServerUrl(server)).path("get_auth").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("got_Basic dTpw", r1.readEntity(String.class));
    }

    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public static class Resource {

        @GET
        @Path("get")
        public String get() {
            return "got";
        }

        @GET
        @Path("get_auth")
        public String getAuth(@HeaderParam("Authorization") String auth) {
            return "got_" + auth;
        }
    }
}
