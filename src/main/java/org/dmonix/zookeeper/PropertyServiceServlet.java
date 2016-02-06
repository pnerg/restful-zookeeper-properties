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
import static javascalautils.TryCompanion.Try;

import java.io.IOException;
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

	private static final Unit Unit = new Unit();

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	}

	// /*
	// * (non-Javadoc)
	// *
	// * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	// */
	// @Override
	// protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	// String path = getPathInfo(req);
	//
	// // list all property set names
	// if (path.isEmpty()) {
	// Try<List<String>> result = propertiesStorageFactory.create().flatMap(storage -> storage.propertySets());
	// result.map(names -> writeJSonResponse(resp, gson.toJson(names))).recover(t -> writeInternalErrorResponse(resp, t));
	// } else {
	// Try<Option<PropertySet>> result = propertiesStorageFactory.create().flatMap(storage -> storage.get(path));
	// // make a response
	// result.map(properties -> writeProperties(resp, properties)).recover(t -> writeInternalErrorResponse(resp, t));
	// }
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = getPathInfo(req);
		
		// list all property set names
		Try<Response> response = null;
		if (path.isEmpty()) {
			Try<List<String>> result = propertiesStorageFactory.create().flatMap(storage -> storage.propertySets());
			response = result.map(list -> ListResponse(list)).recover(t -> ErrorResponse(t));
		}
		else {
			Try<Option<PropertySet>> result = propertiesStorageFactory.create().flatMap(storage -> storage.get(path));
			response = result.map(p -> PropertySetResponse(p)).recover(t -> ErrorResponse(t));
		}
		response.forEach(r -> writeResponse(resp, r));
	}

	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = getPathInfo(req);
//		if (path.isEmpty()) {
//			writeErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing property set name");
//			return;
//		}
	}

	private static String getPathInfo(HttpServletRequest req) {
		return Option(req.getPathInfo()).map(p -> p.substring(1)).getOrElse(() -> "");
	}

	private static void writeResponse(HttpServletResponse resp, Response response) {
		resp.setStatus(response.responseCode);
		Try(() -> resp.getWriter().write(response.message));
	}
	
	private static class Response {
		private final int responseCode;
		private final String message;

		private Response(int responseCode, String message) {
			this.responseCode = responseCode;
			this.message = message;
		}

	}

	static Response StringResponse(int responseCode, String message) {
		return new Response(responseCode, message);
	}

	private static Response ListResponse(List<String> list) {
		return new Response(200, gson.toJson(list));
	}

	private static Response PropertySetResponse(PropertySet propertySet) {
		final Map<String, String> map = new HashMap<>();
		for (String name : propertySet.properties()) {
			propertySet.property(name).forEach(value -> {
				map.put(name, value);
			});
		}
		return new Response(200, gson.toJson(map));
	}

	private static Response PropertySetResponse(Option<PropertySet> propertySet) {
		return propertySet.map(p -> PropertySetResponse(p)).getOrElse(() -> ErrorResponse(HttpServletResponse.SC_NOT_FOUND, "No such property set"));
	}
	
	private static Response ErrorResponse(Throwable t) {
		return new Response(HttpServletResponse.SC_BAD_REQUEST, t.getMessage());
	}

	private static Response ErrorResponse(int responseCode, String message) {
		return new Response(responseCode, message);
	}
	
}
