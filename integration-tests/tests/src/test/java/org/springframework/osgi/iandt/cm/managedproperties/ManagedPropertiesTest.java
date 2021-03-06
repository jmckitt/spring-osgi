/*
 * Copyright 2006-2009 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.osgi.iandt.cm.managedproperties;

import java.util.Iterator;
import java.util.Properties;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.springframework.osgi.iandt.cm.BaseConfigurationAdminTest;

/**
 * @author Costin Leau
 * 
 */
public class ManagedPropertiesTest extends BaseConfigurationAdminTest {

	private final String ID = getClass().getName();
	private Properties props;
	private static final String SIMPLE = "simple", PROTO = "simplePrototype", CMNG = "containerManaged",
			BMNG = "beanManaged";


	protected String[] getConfigLocations() {
		return new String[] { "org/springframework/osgi/iandt/cm/managedproperties/managedProperties.xml" };
	}

	private void initProperties() {
		System.out.println("Import " + org.springframework.beans.factory.config.CustomEditorConfigurer.class);
		props = new Properties();
		props.setProperty("clazz", System.class.getName());
		props.setProperty("integer", "54321");
	}

	protected void onSetUp() throws Exception {
		super.onSetUp();
		initProperties();
	}

	protected void prepareConfiguration(ConfigurationAdmin configAdmin) throws Exception {
		initProperties();
		Properties localCopy = (Properties) props.clone();
		localCopy.setProperty("string", SIMPLE);
		// prepare simple
		Configuration cfg = configAdmin.getConfiguration(SIMPLE);
		cfg.update(localCopy);

		// prepare simple prototype
		cfg = configAdmin.getConfiguration(PROTO);
		localCopy.setProperty("string", PROTO);
		cfg.update(localCopy);

		// prepare container managed
		cfg = configAdmin.getConfiguration(CMNG);
		localCopy.setProperty("string", CMNG);
		cfg.update(localCopy);

		// bean managed
		cfg = configAdmin.getConfiguration(BMNG);
		localCopy.setProperty("string", BMNG);
		cfg.update(localCopy);
	}

	public void testConfigurationAdminAvailability() throws Exception {
		Configuration cfg = cm.getConfiguration(ID);
		cfg.update(props);
	}

	public void testSimpleBean() throws Exception {
		TestBean simple = (TestBean) applicationContext.getBean(SIMPLE);
		assertEquals(new Integer(54321), simple.getInteger());
		assertEquals(SIMPLE, simple.getString());
		assertEquals(System.class, simple.getClazz());
		assertNotNull(simple.getExoticType());
	}

	public void testPrototype() throws Exception {
		prepareConfiguration(cm);
		final TestBean bean = (TestBean) applicationContext.getBean(PROTO);
		assertEquals(new Integer(54321), bean.getInteger());
		assertEquals(PROTO, bean.getString());

		Properties localCopy = (Properties) props.clone();
		final String newString = PROTO + "#new";
		localCopy.put("string", newString);
		// update properties
		waitForCfgChangeToPropagate(PROTO, localCopy);
		// force another update (just to be sure)
		waitForCfgChangeToPropagate(PROTO, localCopy);
		Thread.sleep(1000 * 5);
		// check new instance
		TestBean anotherBeanInstance = (TestBean) applicationContext.getBean(PROTO);
		assertNotSame(bean, anotherBeanInstance);
		//assertEquals(System.class, simple.getClazz());
		assertEquals(new Integer(54321), anotherBeanInstance.getInteger());
		assertEquals(newString, anotherBeanInstance.getString());
	}

	public void testContainerManagedBean() throws Exception {
		final TestBean bean = (TestBean) applicationContext.getBean(CMNG);
		assertEquals(new Integer(54321), bean.getInteger());
		assertEquals(CMNG, bean.getString());

		Properties localCopy = (Properties) props.clone();
		final String newString = CMNG + "#new";
		localCopy.put("string", newString);

		waitForCfgChangeToPropagate(CMNG, localCopy);
		// check new instance
		TestBean anotherBeanInstance = (TestBean) applicationContext.getBean(CMNG);
		assertSame(bean, anotherBeanInstance);
		//assertEquals(System.class, simple.getClazz());
		assertEquals(new Integer(54321), bean.getInteger());
		assertEquals(newString, bean.getString());
		System.out.println(bean.getClazz());
	}

	public void testBeanManagedBean() throws Exception {
		final TestBean bean = (TestBean) applicationContext.getBean(BMNG);
		assertEquals(new Integer(54321), bean.getInteger());
		assertEquals(BMNG, bean.getString());
		Properties localCopy = (Properties) props.clone();
		final String newString = BMNG + "#new";
		localCopy.put("string", newString);

		assertFalse(localCopy.equals(bean.getProps()));
		waitForCfgChangeToPropagate(BMNG, localCopy);
		// check new instance
		TestBean anotherBeanInstance = (TestBean) applicationContext.getBean(BMNG);
		assertSame(bean, anotherBeanInstance);
		for (Iterator iterator = localCopy.keySet().iterator(); iterator.hasNext();) {
			Object key = iterator.next();
			assertEquals(localCopy.get(key), bean.getProps().get(key));
		}
	}
}