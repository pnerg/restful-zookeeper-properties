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

import javax.ws.rs.core.Response;

import org.junit.Test;

import javascalautils.Option;
import javascalautils.Try;
import junitextensions.OptionAssert;
import junitextensions.TryAssert;

/**
 * Test the class {@link PropertyService}
 * 
 * @author Peter Nerg
 */
public class TestPropertyService extends BaseAssert implements TryAssert, OptionAssert {

	private final MockPropertiesStorageFactory factory = new MockPropertiesStorageFactory();
	private final PropertyService propertyService = new PropertyService(factory);

	@Test
	public void listPropertySets_noSets() {
		Response response = propertyService.listPropertySets();
		assertEquals(200, response.getStatus());
		@SuppressWarnings("unchecked")
		List<String> names = (List<String>)response.getEntity();
		assertTrue(names.isEmpty());
	}

	@Test
	public void listPropertySets() {
		factory.store(createPropertySet("listPropertySets"));
		
		Response response = propertyService.listPropertySets();
		assertEquals(200, response.getStatus());
		@SuppressWarnings("unchecked")
		List<String> names = (List<String>)response.getEntity();
		assertEquals(1, names.size());
		assertTrue(names.contains("listPropertySets"));
	}

	@Test
	public void setPropertySet() {
		HashMap<String, String> map = new HashMap<>();
		map.put("user.name", "Peter");
		
		Response response = propertyService.setPropertySet("setPropertySet", map);
		assertEquals(201, response.getStatus());
		
		Try<Option<PropertySet>> t = factory.get("setPropertySet");
		assertSuccess(t);
		assertSome(t.orNull());
	}
	
	@Test
	public void getPropertySet() {
		factory.store(createPropertySet("getPropertySet"));
		
		Response response = propertyService.getPropertySet("getPropertySet");
		assertEquals(200, response.getStatus());
	}

	@Test
	public void deletePropertySet_nonExisting() {
		Response response = propertyService.deletePropertySet("deletePropertySet");
		assertEquals(200, response.getStatus());
		Try<Option<PropertySet>> t = factory.get("deletePropertySet");
		assertSuccess(t);
		assertNone(t.orNull());
	}

	@Test
	public void deletePropertySet() {
		factory.store(createPropertySet("deletePropertySet"));
		Response response = propertyService.deletePropertySet("deletePropertySet");
		assertEquals(200, response.getStatus());
	}
	
	@Test
	public void getPropertySet_nonExisting() {
		Response response = propertyService.getPropertySet("no-such-set");
		assertEquals(404, response.getStatus());
	}
	
	private static PropertySet createPropertySet(String name) {
		PropertySet set = PropertySet.apply(name);
		set.set("host", "localhost");
		set.set("port", "6969");
		return set;
	}

}
