/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.jersey;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import io.bootique.di.Injector;
import io.bootique.di.Key;
import io.bootique.di.TypeLiteral;
import org.glassfish.hk2.api.DescriptorType;
import org.glassfish.hk2.api.DescriptorVisibility;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.JustInTimeInjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.AbstractActiveDescriptor;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

/**
 * This class is a bridge between Bootique DI container and Jersey's HK2.
 */
public class BqInjectorBridge implements JustInTimeInjectionResolver {

	/*
	 * In terms of HK2 this is a fallback resolver that is used when required service not found in a ServiceLocator.
 	 * We try to create here a custom ActiveDescriptor (once again this is a HK2 term) that will get that service
 	 * from Bootique DI injector.
	 */

	private static final String GLASSFISH_PACKAGE = "org.glassfish";

	private Injector injector;
	private ServiceLocator locator;

	@Inject
	public BqInjectorBridge(Injector injector, ServiceLocator locator) {
		this.injector = injector;
		this.locator = locator;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public boolean justInTimeResolution(Injectee failedInjectionPoint) {
		// HK2 injection point can have multiple qualifiers, Bq DI doesn't support that
		if(failedInjectionPoint.getRequiredQualifiers().size() > 1) {
			return false;
		}

		// get proper Bq DI key from failedInjectionPoint
		TypeLiteral<?> typeLiteral = TypeLiteral.of(failedInjectionPoint.getRequiredType());
		Annotation bindingAnnotation = failedInjectionPoint.getRequiredQualifiers().isEmpty()
				? null
				: failedInjectionPoint.getRequiredQualifiers().iterator().next();
		Key<?> key = bindingAnnotation == null
				? Key.get(typeLiteral)
				: Key.get(typeLiteral, bindingAnnotation);

		if(!injector.hasProvider(key) && !allowDynamicInjectionForKey(key)) {
			return false;
		}

		// register custom descriptor in a HK2's ServiceLocator
		ServiceLocatorUtilities.addOneDescriptor(locator, new BqBindingActiveDescriptor(
				injector.getProvider(key),
				failedInjectionPoint.getRequiredType(),
				failedInjectionPoint.getRequiredQualifiers(),
				typeLiteral.getRawType()
		));
		// notify that we have added a new descriptor
		return true;
	}

	protected boolean allowDynamicInjectionForKey(Key<?> key) {
		String packageName = key.getType().getRawType().getPackage().getName();
		return !packageName.startsWith(GLASSFISH_PACKAGE);
	}

	/**
	 * This descriptor just holds {@link Provider} reference to get the required service from Bootique DI.
	 */
	private static class BqBindingActiveDescriptor<T> extends AbstractActiveDescriptor<T> {

		private final Provider<T> provider;
		private final Type implType;
		private final Class<T> implClass;

		public BqBindingActiveDescriptor(Provider<T> provider, Type implType, Set<Annotation> qualifiers, Class<T> implClass) {
			super(
					Collections.singleton(implType),
					Singleton.class,
					null,
					qualifiers,
					DescriptorType.CLASS,
					DescriptorVisibility.NORMAL,
					0,
					false,
					false,
					null,
					Collections.emptyMap()
			);
			this.provider = provider;
			this.implType = implType;
			this.implClass = implClass;
			setImplementation(implClass.getName());
		}

		public BqBindingActiveDescriptor() {
			this.provider = null;
			this.implClass = null;
			this.implType = null;
		}

		@Override
		public Class<T> getImplementationClass() {
			return implClass;
		}

		@Override
		public Type getImplementationType() {
			return implType;
		}

		@Override
		public T create(ServiceHandle<?> root) {
			return provider.get();
		}
	}
}
