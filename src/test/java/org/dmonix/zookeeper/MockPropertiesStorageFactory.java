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

import static javascalautils.TryCompanion.Success;
import static javascalautils.OptionCompanion.Option;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javascalautils.Option;
import javascalautils.Try;
import javascalautils.Unit;

/**
 * Mock implementation of {@link PropertiesStorageFactory} and {@link PropertiesStorage}.
 * @author Peter Nerg
 */
public final class MockPropertiesStorageFactory implements PropertiesStorageFactory, PropertiesStorage {

	private final Map<String, PropertySet> properties = new HashMap<>();
	
	public static PropertiesStorageFactory apply(String connectString) {
		return new MockPropertiesStorageFactory();
	}
	
	/* (non-Javadoc)
	 * @see org.dmonix.zookeeper.PropertiesStorageFactory#withRootPath(java.lang.String)
	 */
	@Override
	public PropertiesStorageFactory withRootPath(String rootPath) {
		return this;
	}
	
	/* (non-Javadoc)
	 * @see org.dmonix.zookeeper.PropertiesStorageFactory#create()
	 */
	@Override
	public Try<PropertiesStorage> create() {
		return Success(this);
	}

	/* (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() {
	}

	/* (non-Javadoc)
	 * @see org.dmonix.zookeeper.PropertiesStorage#get(java.lang.String)
	 */
	@Override
	public Try<Option<PropertySet>> get(String name) {
		return Success(Option(properties.get(name)));
	}

	/* (non-Javadoc)
	 * @see org.dmonix.zookeeper.PropertiesStorage#store(org.dmonix.zookeeper.PropertySet)
	 */
	@Override
	public Try<Unit> store(PropertySet propertySet) {
		properties.put(propertySet.name(), propertySet);
		return Success(Unit.Instance);
	}

	/* (non-Javadoc)
	 * @see org.dmonix.zookeeper.PropertiesStorage#delete(java.lang.String)
	 */
	@Override
	public Try<Unit> delete(String name) {
		properties.remove(name);
		return Success(Unit.Instance);
	}

	/* (non-Javadoc)
	 * @see org.dmonix.zookeeper.PropertiesStorage#propertySets()
	 */
	@Override
	public Try<List<String>> propertySets() {
		return Success(new ArrayList<>(properties.keySet()));
	}
}
