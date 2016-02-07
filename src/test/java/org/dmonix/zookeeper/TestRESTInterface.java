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

import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.apache.zookeeper.CreateMode.PERSISTENT;
import static org.apache.zookeeper.ZooDefs.Ids.OPEN_ACL_UNSAFE;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import zookeeperjunit.CloseableZooKeeper;
import zookeeperjunit.ZKFactory;
import zookeeperjunit.ZKInstance;
import zookeeperjunit.ZooKeeperAssert;

/**
 * Starts a ZooKeper instance and registers the {@link PropertyServiceServlet} in a Jetty instance. <br>
 * Then runs proper HTTP operations towards the entire thing to test that the servlet will behave correctly
 * 
 * @author Peter Nerg
 */
public class TestRESTInterface extends BaseAssert implements ZooKeeperAssert {

	private static final int HTTP_PORT = 9998;
	private static final String HTTP_URL = "http://localhost:" + HTTP_PORT;

	private static ZKInstance instance = ZKFactory.apply().create();
	private static Server server = new Server(9998);

	private final Client client = ClientBuilder.newClient();

	@BeforeClass
	public static void startServer() throws TimeoutException, Throwable {
		instance.start().result(Duration.ofSeconds(5));

		// configure and register the servlet
		ServletHolder servletHolder = new ServletHolder(PropertyServiceServlet.class);
		servletHolder.setInitParameter("connectString", instance.connectString().get());
		servletHolder.setInitParameter("rootPath", "/etc/properties");
		ServletHandler handler = new ServletHandler();
		handler.addServletWithMapping(servletHolder, "/properties/*");

		// start the HTTP server referring to the servlet
		server.setHandler(handler);
		server.start();
	}

	@AfterClass
	public static void stopServer() throws Exception {
		instance.destroy().ready(Duration.ofSeconds(5));
		server.stop();
	}

	@Before
	public void createZkPaths() throws TimeoutException, Throwable {
		try (CloseableZooKeeper zk = instance.connect().get()) {
			zk.create("/etc", new byte[0], OPEN_ACL_UNSAFE, PERSISTENT);
			zk.create("/etc/properties", new byte[0], OPEN_ACL_UNSAFE, PERSISTENT);
		}
	}
	
	@After
	public void cleanZooKeeper() throws TimeoutException, Throwable {
		try (CloseableZooKeeper zk = instance.connect().get()) {
			zk.deleteRecursively("/etc");
		}
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
	public void listPropertySets_empty() {
		WebTarget target = client.target(HTTP_URL).path("/properties");
		Response response = target.request(APPLICATION_JSON_TYPE).get();
		assertEquals(200, response.getStatus());
		assertEquals("[]", response.readEntity(String.class));
	}

	@Test
	public void listPropertySets() {
		setPropertySet();
		WebTarget target = client.target(HTTP_URL).path("/properties");
		Response response = target.request(APPLICATION_JSON_TYPE).get();
		assertEquals(200, response.getStatus());
		assertEquals("[\"setPropertySet\"]", response.readEntity(String.class));
	}

	@Test
	public void setPropertySet() {
		WebTarget target = client.target(HTTP_URL).path("/properties/setPropertySet");
		Response response = target.request().put(Entity.json("{port:\"6969\",\"host\":\"127.0.0.1\"}"));
		assertEquals(SC_CREATED, response.getStatus());

		assertPropertySetExists("setPropertySet");
	}

	@Test
	public void deletePropertySet_nonExistingSet() {
		WebTarget target = client.target(HTTP_URL).path("/properties/no-such-set");
		Response response = target.request().delete();
		assertEquals(SC_OK, response.getStatus());
	}

	@Test
	public void deletePropertySet() {
		setPropertySet();
		WebTarget target = client.target(HTTP_URL).path("/properties/setPropertySet");
		Response response = target.request().delete();
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());

		assertPropertySetNotExists("setPropertySet");
	}

	@Test
	public void listProperties_nonExistingSet() {
		WebTarget target = client.target(HTTP_URL).path("/properties/no-such-set");
		Response response = target.request(APPLICATION_JSON_TYPE).get();
		assertEquals(SC_NOT_FOUND, response.getStatus());
	}

	@Test
	public void listProperties() {
		setPropertySet();
		WebTarget target = client.target(HTTP_URL).path("/properties/setPropertySet");
		Response response = target.request(APPLICATION_JSON_TYPE).get();
		assertEquals(SC_OK, response.getStatus());
	}

	private void assertPropertySetExists(String name) {
		WebTarget target = client.target(HTTP_URL).path("/properties/" + name);
		Response response = target.request(APPLICATION_JSON_TYPE).get();
		assertEquals(SC_OK, response.getStatus());
	}

	private void assertPropertySetNotExists(String name) {
		WebTarget target = client.target(HTTP_URL).path("/properties/" + name);
		Response response = target.request(APPLICATION_JSON_TYPE).get();
		assertEquals(SC_NOT_FOUND, response.getStatus());
	}
}
