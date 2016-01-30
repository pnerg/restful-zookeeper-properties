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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Peter Nerg
 * @since 1.0
 */
@Path("/properties")
public class PropertyService {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> listPropertySets() {
		List<String> ls = new ArrayList<String>();
		ls.add("System");
		ls.add("Module-1");

		return ls;
	}

	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, String> getPropertySet(@PathParam("id") String id) {
		PropertySet propertySet = PropertySet.apply(id);
		propertySet.set("host", "localhost");
		propertySet.set("port", "6969");
		Map<String, String> properties = new HashMap<>();
		propertySet.properties().forEach(name -> properties.put(name, propertySet.property(name).orNull()));
		return properties;
	}
	
	@PUT
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response setPropertySet(@PathParam("id") String id, Map<String, String> properties) {
		properties.keySet().forEach(name -> System.out.println(name+":"+properties.get(name)));
		return Response.status(201).build();
	}
}
