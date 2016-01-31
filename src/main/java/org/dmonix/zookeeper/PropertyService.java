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

import static javascalautils.OptionCompanion.Option;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;
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
@Singleton
@Path("/properties")
public class PropertyService {

	private Map<String, Map<String, String>> map = new HashMap<>();
	private final PropertiesStorageFactory propertiesStorageFactory;
	
	PropertyService(PropertiesStorageFactory propertiesStorageFactory)  {
		this.propertiesStorageFactory = propertiesStorageFactory;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Set<String> listPropertySets() {
		return map.keySet();
	}

	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPropertySet(@PathParam("id") String id) {
		
		return Option(map.get(id)).map(map -> {
			return Response.status(200).entity(map).build();
		}).getOrElse(() -> Response.status(404).build());
		
//		PropertySet propertySet = PropertySet.apply(id);
//		propertySet.set("host", "localhost");
//		propertySet.set("port", "6969");
//		Map<String, String> properties = new HashMap<>();
//		propertySet.properties().forEach(name -> properties.put(name, propertySet.property(name).orNull()));
//		Response.status(200).
//		return properties;
	}
	
	@PUT
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response setPropertySet(@PathParam("id") String id, Map<String, String> properties) {
		map.put(id, properties);
		return Response.status(201).build();
	}
}
