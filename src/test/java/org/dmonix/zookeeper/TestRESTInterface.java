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

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
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
//	private static ResourceConfig config = ResourceConfig.forApplicationClass(RestfulZooKeeperPropertiesApp.class);
    private static Server server = new Server(9998);

	private final Client client = ClientBuilder.newClient();

	@BeforeClass
	public static void startServer() throws TimeoutException, Throwable {
		
		
		instance.start().result(Duration.ofSeconds(5));
		try (CloseableZooKeeper zk = instance.connect().get()) {
			zk.create("/etc", new byte[0], OPEN_ACL_UNSAFE, PERSISTENT);
		}
		
	       // Create a basic jetty server object that will listen on port 8080.
        // Note that if you set this to port 0 then a randomly available port
        // will be assigned that you can either look in the logs for the port,
        // or programmatically obtain it for use in test cases.
 
        // The ServletHandler is a dead simple way to create a context handler
        // that is backed by an instance of a Servlet.
        // This handler then needs to be registered with the Server object.
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
 
        // Passing in the class for the Servlet allows jetty to instantiate an
        // instance of that Servlet and mount it on a given context path.
 
        // IMPORTANT:
        // This is a raw Servlet, not a Servlet that has been configured
        // through a web.xml @WebServlet annotation, or anything similar.
        handler.addServletWithMapping(PropertyServiceServlet.class, "/*");
 
        // Start things up!
		server.start();
	}

	@AfterClass
	public static void stopServer() throws Exception {
		server.stop();
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
	public void setPropertySet() {
		WebTarget target = client.target(HTTP_URL).path("/properties/setPropertySet");
		Response response = target.request().put(Entity.json("{\"port\":\"6969\",\"host\":\"127.0.0.1\"}"));
		assertEquals(201, response.getStatus());
		
		assertPropertySetExists("setPropertySet");
	}

	@Test
	public void deletePropertySet_nonExistingSet() {
		WebTarget target = client.target(HTTP_URL).path("/properties/no-such-set");
		Response response = target.request().delete();
		assertEquals(200, response.getStatus());
	}

	@Test
	public void deletePropertySet() {
		setPropertySet();
		WebTarget target = client.target(HTTP_URL).path("/properties/setPropertySet");
		Response response = target.request().delete();
		assertEquals(200, response.getStatus());
		
		assertPropertySetNotExists("setPropertySet");
	}
	
	@Test
	public void listProperties_nonExistingSet() {
		WebTarget target = client.target(HTTP_URL).path("/properties/no-such-set");
		Response response = target.request(MediaType.APPLICATION_JSON_TYPE).get();
		assertEquals(404, response.getStatus());
	}
	
	@Test
	public void listProperties() {
		setPropertySet();
		WebTarget target = client.target(HTTP_URL).path("/properties/setPropertySet");
		Response response = target.request(MediaType.APPLICATION_JSON_TYPE).get();
		assertEquals(200, response.getStatus());
	}

	private void assertPropertySetExists(String name) {
		WebTarget target = client.target(HTTP_URL).path("/properties/"+name);
		Response response = target.request(MediaType.APPLICATION_JSON_TYPE).get();
		assertEquals(200, response.getStatus());
	}

	private void assertPropertySetNotExists(String name) {
		WebTarget target = client.target(HTTP_URL).path("/properties/"+name);
		Response response = target.request(MediaType.APPLICATION_JSON_TYPE).get();
		assertEquals(404, response.getStatus());
	}
}
