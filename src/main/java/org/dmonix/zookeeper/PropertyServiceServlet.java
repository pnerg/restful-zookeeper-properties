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
import java.util.function.Function;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javascalautils.Try;
import javascalautils.Unit;

/**
 * @author Peter Nerg
 */
@WebServlet(name = "PropertyService", displayName = "RESTful ZooKeeper Properties", description = "RESTful interface for managing properties stored in ZooKeeper", urlPatterns = {
		"/properties/*" }, loadOnStartup = 1, initParams = { @WebInitParam(name = "connectString", value = "localhost:6969"), @WebInitParam(name = "rootPath", value = "/etc/properties") })
public class PropertyServiceServlet extends HttpServlet {

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

		System.out.println(config.getInitParameter("connectString"));
		System.out.println(config.getInitParameter("rootPath"));
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

		Try<List<String>> result = propertiesStorageFactory.create().flatMap(storage -> storage.propertySets());
		result.map(names -> {
			Try(() -> resp.getWriter().write(names.toString()));
			return Unit.Instance;
		}).recover(new RecoverFunction(resp));

	}

	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

	}

	
	
	private static final class RecoverFunction implements Function<Throwable, Unit> {
		private final HttpServletResponse resp;

		private RecoverFunction(HttpServletResponse resp) {
			this.resp = resp;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.function.Function#apply(java.lang.Object)
		 */
		@Override
		public Unit apply(Throwable t) {
			Try(() -> {
				resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				t.printStackTrace(resp.getWriter());
			});
			return Unit.Instance;
		}

	}
}
