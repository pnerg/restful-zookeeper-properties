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
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javascalautils.Option;
import javascalautils.Try;
import javascalautils.Unit;

/**
 * @author Peter Nerg
 */
@WebServlet(name = "PropertyService", displayName = "RESTful ZooKeeper Properties", description = "RESTful interface for managing properties stored in ZooKeeper", urlPatterns = {
		"/properties/*" }, loadOnStartup = 1, initParams = { @WebInitParam(name = "connectString", value = "localhost:6969"), @WebInitParam(name = "rootPath", value = "/etc/properties") })
public class PropertyServiceServlet extends HttpServlet {

	private static final Unit Unit = new Unit();

	/**
	 * A {@code String} constant representing {@value #APPLICATION_JSON} media type.
	 */
	private final static String APPLICATION_JSON = "application/json";
	/** Double quote " */
	private final static String DQC = "\"";
	private static final long serialVersionUID = -5954664255975640068L;

	private PropertiesStorageFactory propertiesStorageFactory;

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = Option(req.getPathInfo()).map(p -> p.substring(1)).getOrElse(() -> "");

		// list all property set names
		if (path.isEmpty()) {
			Try<List<String>> result = propertiesStorageFactory.create().flatMap(storage -> storage.propertySets());
			result.map(names -> writeJSonResponse(resp, toString(names))).recover(t -> writeInternalErrorResponse(resp, t));
		} else {
			Try<Option<PropertySet>> result = propertiesStorageFactory.create().flatMap(storage -> storage.get(path));
			// make a response
			result.map(properties -> writeProperties(resp, properties)).recover(t -> writeInternalErrorResponse(resp, t));
		}
	}

	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

	}

	private static String toString(List<String> list) {
		// this is sooo ugly, so imperative and non-functional.
		// a simple reduce would be nicer but how to get rid of the last ','
		int size = list.size();
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < list.size(); i++) {
			sb.append(DQC).append(list.get(i)).append(DQC);
			if (i < size - 1)
				sb.append(",");
		}
		sb.append("]");
		return sb.toString();
	}

	private static String toString(PropertySet propertySet) {
		// this is sooo ugly, so imperative and non-functional.
		// a simple reduce would be nicer but how to get rid of the last ','
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		int counter = 0;
		int size = propertySet.properties().size();
		for (String name : propertySet.properties()) {
			propertySet.property(name).forEach(value -> {
				sb.append(DQC).append(name).append(DQC).append(":").append(DQC).append(value).append(DQC);
			});
			counter++;
			if (counter < size)
				sb.append(",");
		}
		sb.append("}");
		return sb.toString();
	}

	private static Unit writeProperties(HttpServletResponse resp, Option<PropertySet> properties) {
		return properties.map(set -> writeJSonResponse(resp, toString(set))).getOrElse(() -> writeNotFoundResponse(resp));
	}

	private static Unit writeJSonResponse(HttpServletResponse resp, String response) {
		resp.setContentType(APPLICATION_JSON);
		return Try(() -> resp.getWriter().write(response)).getOrElse(() -> Unit);
	}

	private static Unit writeNotFoundResponse(HttpServletResponse resp) {
		return writeErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "No such property set");
	}
	
	private static Unit writeInternalErrorResponse(HttpServletResponse resp, Throwable t) {
		return writeErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getMessage());
	}

	private static Unit writeErrorResponse(HttpServletResponse resp, int errorCode, String message) {
		return Try(() -> resp.sendError(errorCode, message)).getOrElse(() -> Unit);
	}
}
