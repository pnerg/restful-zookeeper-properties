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

import static javascalautils.OptionCompanion.None;
import static javascalautils.OptionCompanion.Option;
import static javascalautils.OptionCompanion.Some;
import static javascalautils.TryCompanion.Try;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import javascalautils.Option;
import javascalautils.Try;
import javascalautils.Unit;



/**
 * @author Peter Nerg
 */
@WebServlet(name = "PropertyService", displayName = "RESTful ZooKeeper Properties", description = "RESTful interface for managing properties stored in ZooKeeper", urlPatterns = {
		"/properties/*" }, loadOnStartup = 1, initParams = { @WebInitParam(name = "connectString", value = "localhost:6181"), @WebInitParam(name = "rootPath", value = "/etc/properties") })
public class PropertyServiceServlet extends HttpServlet {

	/**
	 * A {@code String} constant representing {@value #APPLICATION_JSON} media type.
	 */
	private final static String APPLICATION_JSON = "application/json";
	private static final long serialVersionUID = -5954664255975640068L;

	private PropertiesStorageFactory propertiesStorageFactory;
	private static final Gson gson = new Gson();

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		propertiesStorageFactory = PropertiesStorageFactory.apply(config.getInitParameter("connectString"));
		Option(config.getInitParameter("rootPath")).forEach(value -> propertiesStorageFactory.withRootPath(value));

	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPut(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Response result = getPathInfo(req).map(name -> {
			Try<PropertySet> propSet = Try(() -> {
				PropertySet set = PropertySet.apply(name);
				Map<String, String> map = (Map<String, String>)gson.fromJson(new InputStreamReader(req.getInputStream()), Map.class);
				map.forEach((k,v) -> set.set(k, v));
				return set;
			});
			
			Try<Unit> createResult = propSet.flatMap(set -> propertiesStorageFactory.create().flatMap(storage -> storage.store(set)));
			
			//orNull will never happen as we installed a recover function 
			return createResult.map(u -> EmptyResponse(SC_CREATED)).recover(t -> ErrorResponse(t)).orNull();
		}).getOrElse(() -> ErrorResponse(SC_BAD_REQUEST, "Missing property set name"));
		
		writeResponse(resp, result);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = getPathInfo(req).getOrElse(() -> "");

		// list all property set names
		Try<Response> response = null;
		if (path.isEmpty()) {
			Try<List<String>> result = propertiesStorageFactory.create().flatMap(storage -> storage.propertySets());
			response = result.map(list -> ListResponse(list)).recover(t -> ErrorResponse(t));
		} else {
			Try<Option<PropertySet>> result = propertiesStorageFactory.create().flatMap(storage -> storage.get(path));
			response = result.map(p -> PropertySetResponse(p)).recover(t -> ErrorResponse(t));
		}
		response.forEach(r -> writeResponse(resp, r));
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Response response = getPathInfo(req).map(path -> {
			Try<Unit> result = propertiesStorageFactory.create().flatMap(storage -> storage.delete(path));
			return result.map(r -> EmptyResponse(SC_OK)).recover(t -> ErrorResponse(t)).orNull();
		}).getOrElse(() -> ErrorResponse(SC_BAD_REQUEST, "Missing property set name"));
		writeResponse(resp, response);
	}

	private static Option<String> getPathInfo(HttpServletRequest req) {
		return Option(req.getPathInfo()).map(p -> p.substring(1));
	}

	private static void writeResponse(HttpServletResponse resp, Response response) {
		resp.setStatus(response.responseCode);
		response.mediaType.forEach(mt -> resp.setContentType(mt));
		Try(() -> resp.getWriter().write(response.message));
	}

	private static class Response {
		private final int responseCode;
		private final String message;
		private final Option<String> mediaType;
		
		private Response(int responseCode, String message) {
			this(responseCode, message, None());
		}

		private Response(int responseCode, String message, Option<String> mediaType) {
			this.responseCode = responseCode;
			this.message = message;
			this.mediaType = mediaType;
		}
		
	}


	private static Response EmptyResponse(int responseCode) {
		return new Response(responseCode, "");
	}

	private static Response ListResponse(List<String> list) {
		return new Response(SC_OK, gson.toJson(list), Some(APPLICATION_JSON));
	}

	private static Response PropertySetResponse(PropertySet propertySet) {
		final Map<String, String> map = new HashMap<>();
		for (String name : propertySet.properties()) {
			propertySet.property(name).forEach(value -> {
				map.put(name, value);
			});
		}
		return new Response(SC_OK, gson.toJson(map), Some(APPLICATION_JSON));
	}

	private static Response PropertySetResponse(Option<PropertySet> propertySet) {
		return propertySet.map(p -> PropertySetResponse(p)).getOrElse(() -> ErrorResponse(SC_NOT_FOUND, "No such property set"));
	}

	private static Response ErrorResponse(Throwable t) {
		return new Response(SC_BAD_REQUEST, t.getMessage());
	}

	private static Response ErrorResponse(int responseCode, String message) {
		return new Response(responseCode, message);
	}

}
