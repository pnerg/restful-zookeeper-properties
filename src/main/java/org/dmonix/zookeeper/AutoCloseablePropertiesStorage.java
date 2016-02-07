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

import static javascalautils.TryCompanion.Try;

import java.util.List;
import java.util.function.Function;

import javascalautils.Option;
import javascalautils.Try;
import javascalautils.Unit;

/**
 * Acts as a proxy for {@link PropertiesStorage} making performing auto-close on the {@link PropertiesStorage} after a performed operation.
 * 
 * @author Peter Nerg
 * @since 0.6
 */
final class AutoCloseablePropertiesStorage implements PropertiesStorage {

	private PropertiesStorage propertiesStorage;

	AutoCloseablePropertiesStorage(PropertiesStorage propertiesStorage) {
		this.propertiesStorage = propertiesStorage;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() {
		Try(() -> propertiesStorage.close());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dmonix.zookeeper.PropertiesStorage#get(java.lang.String)
	 */
	@Override
	public Try<Option<PropertySet>> get(String name) {
		return invoke(storage -> storage.get(name));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dmonix.zookeeper.PropertiesStorage#store(org.dmonix.zookeeper.PropertySet)
	 */
	@Override
	public Try<Unit> store(PropertySet propertySet) {
		return invoke(storage -> storage.store(propertySet));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dmonix.zookeeper.PropertiesStorage#delete(java.lang.String)
	 */
	@Override
	public Try<Unit> delete(String name) {
		return invoke(storage -> storage.delete(name));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dmonix.zookeeper.PropertiesStorage#propertySets()
	 */
	@Override
	public Try<List<String>> propertySets() {
		return invoke(storage -> storage.propertySets());
	}

	/**
	 * Internal operation that will perform the provided function and then automatically close the {@link PropertiesStorage}
	 * @param f
	 * @return
	 */
	private <R> R invoke(Function<PropertiesStorage, R> f) {
		try {
			return f.apply(propertiesStorage);
		} finally {
			close();
		}
	}

}
