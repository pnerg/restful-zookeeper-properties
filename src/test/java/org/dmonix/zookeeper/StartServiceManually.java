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

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import zookeeperjunit.CloseableZooKeeper;
import zookeeperjunit.ZKFactory;
import zookeeperjunit.ZKInstance;

/**
 * This class is used to manually start both a ZooKeeper server and a web server exposing the RESTful ZooKeepoer interface. <br>
 * Once started it will print the URL you can use to connect to.
 * 
 * @author Peter Nerg
 */
public class StartServiceManually {
	private static final int HTTP_PORT = 9998;

	private static ZKInstance instance = ZKFactory.apply().create();
	private static Server server = new Server(9998);

	public static void main(String[] args) throws Throwable {
		// starts a in-memory ZooKeeper server
		instance.start().result(Duration.ofSeconds(5));

		// creates the root path in ZooKeeper
		try (CloseableZooKeeper zk = instance.connect().get()) {
			zk.create("/etc", new byte[0], OPEN_ACL_UNSAFE, PERSISTENT);
			zk.create("/etc/properties", new byte[0], OPEN_ACL_UNSAFE, PERSISTENT);
			zk.create("/etc/properties/example-set", new byte[0], OPEN_ACL_UNSAFE, PERSISTENT);
			zk.create("/etc/properties/example-set/host", "localhost".getBytes(), OPEN_ACL_UNSAFE, PERSISTENT);
			zk.create("/etc/properties/example-set/port", "6969".getBytes(), OPEN_ACL_UNSAFE, PERSISTENT);
		}

		//configure and register the servlet
		ServletHolder servletHolder = new ServletHolder(PropertyServiceServlet.class);
		servletHolder.setInitParameter("connectString", instance.connectString().get());
		servletHolder.setInitParameter("rootPath", "/etc/properties");
		ServletHandler handler = new ServletHandler();
		handler.addServletWithMapping(servletHolder, "/properties/*");

		//start the HTTP server referring to the servlet
		server.setHandler(handler);
		server.start();

		System.out.println("Started services");
		System.out.println(instance.connectString().get()); //the ZooKeeper connect string
		System.out.println("http://localhost:" + HTTP_PORT + "/properties"); //The URL to direct your browser to
	}

}
