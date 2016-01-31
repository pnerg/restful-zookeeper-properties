/**
 *  Copyright 2016 Peter Nerg
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.dmonix.zookeeper;

import static org.apache.zookeeper.CreateMode.PERSISTENT;
import static org.apache.zookeeper.ZooDefs.Ids.OPEN_ACL_UNSAFE;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import zookeeperjunit.CloseableZooKeeper;
import zookeeperjunit.ZKFactory;
import zookeeperjunit.ZKInstance;
import zookeeperjunit.ZooKeeperAssert;

/**
 * @author Peter Nerg
 *
 */
public class TestRESTInterface extends BaseAssert implements ZooKeeperAssert {

	private static final int HTTP_PORT = 9998;
	private static final String HTTP_URL = "http://localhost:"+HTTP_PORT;
	
	private static ZKInstance instance = ZKFactory.apply().withPort(6969).create();
	private static URI baseUri = UriBuilder.fromUri("http://localhost/").port(HTTP_PORT).build();
	private static ResourceConfig config = ResourceConfig.forApplicationClass(RestfulZooKeeperPropertiesApp.class);
	private static HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, config);

	private final Client client = ClientBuilder.newClient();

	@BeforeClass
	public static void startServer() throws TimeoutException, Throwable {
		instance.start().result(Duration.ofSeconds(5));
		try (CloseableZooKeeper zk = instance.connect().get()) {
			zk.create("/etc", new byte[0], OPEN_ACL_UNSAFE, PERSISTENT);
		}
		server.start();
	}

	@AfterClass
	public static void stopServer() throws TimeoutException, InterruptedException {
		server.shutdownNow();
		instance.destroy().ready(Duration.ofSeconds(5));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see zookeeperjunit.ZooKeeperAssert#instance()
	 */
	@Override
	public ZKInstance instance() {
		return instance;
	}

	@Test
	public void listPropertySets() {
		WebTarget target = client.target(HTTP_URL).path("/properties");
		Response response = target.request(MediaType.APPLICATION_JSON_TYPE).get();
		assertEquals(200, response.getStatus());
	}

	@Test
	public void listProperties_nonExistingSet() {
		WebTarget target = client.target(HTTP_URL).path("/properties/no-such-set");
		Response response = target.request(MediaType.APPLICATION_JSON_TYPE).get();
		assertEquals(404, response.getStatus());
	}
	
	@Test
	public void setPropertySet() {
		WebTarget target = client.target(HTTP_URL).path("/properties/setPropertySet");
		Response response = target.request().put(Entity.json("{\"port\":\"6969\",\"host\":\"127.0.0.1\"}"));
		assertEquals(201, response.getStatus());
	}

}
