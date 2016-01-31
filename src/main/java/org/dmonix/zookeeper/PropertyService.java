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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import javascalautils.Option;
import javascalautils.Try;

/**
 * @author Peter Nerg
 * @since 1.0
 */
@Singleton
@Path("/properties")
public final class PropertyService {

	private final PropertiesStorageFactory propertiesStorageFactory;

	PropertyService(PropertiesStorageFactory propertiesStorageFactory) {
		this.propertiesStorageFactory = propertiesStorageFactory;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response listPropertySets() {
		Try<List<String>> result = propertiesStorageFactory.create().flatMap(storage -> storage.propertySets());
		return result.map(list -> successResponse(list)).getOrElse(() -> failureResponse());
	}

	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPropertySet(@PathParam("id") String id) {
		//attempt to get the property set from storage
		Try<Option<PropertySet>> result = propertiesStorageFactory.create().flatMap(storage -> storage.get(id));
		
		//make a response
		Try<Response> response = result.map(option -> option.map(set -> {
			 Map<String, String> properties = new HashMap<>();
			 set.properties().forEach(name -> properties.put(name, set.property(name).orNull()));
			 return successResponse(properties);
		}).getOrElse(() -> customReponse(Status.NOT_FOUND)));

		return response.getOrElse(() -> failureResponse());
	}

	@PUT
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response setPropertySet(@PathParam("id") String id, Map<String, String> properties) {
		// map.put(id, properties);
		return customReponse(Status.CREATED);
	}

	private static Response customReponse(Status status) {
		return Response.status(status).build();
	}
	
	private static Response successResponse(Object response) {
		return Response.status(Status.OK).entity(response).build();
	}
	
	private static Response failureResponse() {
		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}
}
