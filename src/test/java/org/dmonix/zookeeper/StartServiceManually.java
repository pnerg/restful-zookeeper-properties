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

import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import zookeeperjunit.CloseableZooKeeper;
import zookeeperjunit.ZKFactory;
import zookeeperjunit.ZKInstance;

/**
 * This class is used to manually start both a ZooKeeper server and a web server exposing the RESTful ZooKeepoer interface. <br>
 * Once started it will print the URL you can use to connect to.
 * @author Peter Nerg
 */
public class StartServiceManually {
	private static final int HTTP_PORT = 9998;
	
	private static ZKInstance instance = ZKFactory.apply().withPort(6969).create();
	private static URI baseUri = UriBuilder.fromUri("http://localhost/").port(HTTP_PORT).build();
	private static ResourceConfig config = ResourceConfig.forApplicationClass(RestfulZooKeeperPropertiesApp.class);
	private static HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, config);

	public static void main(String[] args) throws Throwable {
		//starts a in-memory ZooKeeper server
		instance.start().result(Duration.ofSeconds(5));
		
		//creates the root path in ZooKeeper
		try (CloseableZooKeeper zk = instance.connect().get()) {
			zk.create("/etc", new byte[0], OPEN_ACL_UNSAFE, PERSISTENT);
		}
		
		//starts the HTTP server
		server.start();
		
		System.out.println("Started service");
		System.out.println("http://localhost:"+HTTP_PORT+"/properties");
	}
}
