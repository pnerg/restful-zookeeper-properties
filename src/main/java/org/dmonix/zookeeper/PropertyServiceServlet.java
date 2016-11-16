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

import javascalautils.Option;
import javascalautils.Try;
import javascalautils.Unit;
import org.dmonix.servlet.JSONServlet;
import org.dmonix.servlet.Request;
import org.dmonix.servlet.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import java.util.List;
import java.util.Map;

import static javascalautils.OptionCompanion.Option;
import static javax.servlet.http.HttpServletResponse.*;

/**
 * The servlet acting as the REST interface for the properties in ZooKeeper. <br>
 * Uses functionality from the <a href="https://github.com/pnerg/zookeeper-properties">zookeeper-properties</a> project to manage the properties in ZooKeeper.
 * 
 * @author Peter Nerg
 * @since 0.6
 */
@WebServlet(name = "PropertyService", displayName = "RESTful ZooKeeper Properties", description = "RESTful interface for managing properties stored in ZooKeeper", urlPatterns = {
		"/properties/*" }, loadOnStartup = 1, initParams = { @WebInitParam(name = "connectString", value = "localhost:2181"), @WebInitParam(name = "rootPath", value = "/etc/properties") })
public final class PropertyServiceServlet extends JSONServlet {

	private static final Logger logger = LoggerFactory.getLogger(PropertyServiceServlet.class);
	
	private static final long serialVersionUID = -5954664255975640068L;

	/** Factory to create access to the ZooKeeper storage */
	private PropertiesStorageFactory propertiesStorageFactory;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
		logger.info("Starting PropertyServiceServlet");
		logger.info("connectString="+config.getInitParameter("connectString"));
		logger.info("rootPath="+config.getInitParameter("rootPath"));
		propertiesStorageFactory = PropertiesStorageFactory.apply(config.getInitParameter("connectString"));
		Option(config.getInitParameter("rootPath")).forEach(value -> propertiesStorageFactory.withRootPath(value));
		
	}

	/**
	 * Manages storage of property sets.
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected Response put(Request req) {
		return req.getPathInfo().map(name -> {
			Try<PropertySet> propSet = propSet(name, req);
			// orNull will never happen as we installed a recover function
			return storeProperties(propSet)
					.map(u -> EmptyResponse(SC_CREATED)).recover(t -> ErrorResponse(t)).orNull();
		}).getOrElse(() -> ErrorResponse(SC_BAD_REQUEST, "Missing property set name"));
	}

	@Override
	protected Response post(Request req) {
		return req.getPathInfo().map(name -> {
			Try<PropertySet> toBeStored = propSet(name, req)
					.flatMap(newProps -> getStoredProperties(name).map(storedProps -> {
						PropertySet combinedProps = storedProps.getOrElse(() -> PropertySet.apply(name));
						newProps.asMap().forEach((k,v) -> combinedProps.set(k,v));
						return combinedProps;
					}));
			// orNull will never happen as we installed a recover function
			return storeProperties(toBeStored)
					.map(u -> EmptyResponse(SC_CREATED)).recover(this::ErrorResponse).orNull();
		}).getOrElse(() -> ErrorResponse(SC_BAD_REQUEST, "Missing property set name"));
	}

	/**
	 * Manages both listing the names of all property sets and listing properties for an individual set.
	 */
	@Override
	protected Try<Response> getWithTry(Request req)  {
		String path = req.getPathInfo().getOrElse(() -> "");

		// list all property set names
		Try<Response> response;
		if (path.isEmpty()) {
			logger.debug("Requesting all property set names");
			Try<List<String>> result = createStorage().flatMap(storage -> storage.propertySets());
			response = result.map(list -> ObjectResponse(list));
		} else {
			logger.debug("Requesting data for property [{}]", path);
			response = getStoredProperties(path).map(this::PropertySetResponse);
		}
		return response;
	}

	/**
	 * Manages delete of a specified property set.
	 */
	@Override
	protected Response delete(Request req) {
		return req.getPathInfo().map(name -> {
			logger.debug("Deleting property set [{}]", name);
			Try<Unit> result = createStorage().flatMap(storage -> storage.delete(name));
			return result.map(r -> EmptyResponse(SC_OK)).recover(this::ErrorResponse).orNull();
		}).getOrElse(() -> ErrorResponse(SC_BAD_REQUEST, "Missing property set name"));
	}

	private Try<Unit> storeProperties(Try<PropertySet> propSet) {
		return propSet.flatMap(set -> createStorage().flatMap(storage -> storage.store(set)));
	}

	/**
	 * Get stored properties
	 * @param name
	 * @return
     */
	private Try<Option<PropertySet>> getStoredProperties(String name) {
		return createStorage().flatMap(storage -> storage.get(name));
	}

	/**
	 * Uses the {@link PropertiesStorageFactory} to create a {@link AutoCloseablePropertiesStorage}.
	 * @return
	 */
	private Try<PropertiesStorage> createStorage() {
		//use the factory to create a PropertiesStorage and then wrap it in a AutoCloseablePropertiesStorage
		return propertiesStorageFactory.create().map(storage -> new AutoCloseablePropertiesStorage(storage));
	}

	/**
	 * Parses the property set data from the json formated data in the HTTP request input stream
	 * @param name The name of the property set
	 * @param req The HTTP request data
	 * @return The property set
	 */
	private static Try<PropertySet> propSet(String name, Request req) {
		return req.fromJson(Map.class).map(m -> (Map<String, String>)m).map(map -> {
			PropertySet set = PropertySet.apply(name);
			map.forEach((k, v) -> set.set(k, v));
			logger.debug("Storing property [{}]", set);
			return set;
		});
	}

	/**
	 * Creates a response using an optional value property set. <br>
	 * If no property set is included a 404 is returned.
	 * @param propertySet
	 * @return
	 */
	private Response PropertySetResponse(Option<PropertySet> propertySet) {
		return propertySet.map(p -> ObjectResponse(p.asMap())).getOrElse(() -> ErrorResponse(SC_NOT_FOUND, "No such property set"));
	}

}
