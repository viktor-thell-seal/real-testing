/*
 * Copyright (c) 2017 org.hrodberaht
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hrodberaht.injection.core.config;

import org.hrodberaht.injection.core.internal.InjectionContainerManager;
import org.hrodberaht.injection.core.internal.ResourceInject;
import org.hrodberaht.injection.core.internal.ScopeContainer;
import org.hrodberaht.injection.core.internal.annotation.DefaultInjectionPointFinder;
import org.hrodberaht.injection.core.internal.annotation.InjectionFinder;
import org.hrodberaht.injection.core.register.InjectionRegister;
import org.hrodberaht.injection.core.register.RegistrationModuleAnnotation;
import org.hrodberaht.injection.core.spi.ContainerConfigBuilder;
import org.hrodberaht.injection.core.spi.ResourceFactory;
import org.hrodberaht.injection.core.spi.module.CustomInjectionPointFinderModule;
import org.hrodberaht.injection.core.stream.InjectionRegistryBuilder;

/**
 * Unit Test JUnit (using @Inject)
 *
 * @author Robert Alexandersson
 * 2010-okt-11 19:35:27
 * @version 1.0
 * @since 1.0
 */
public abstract class ContainerConfig<T extends InjectionRegister> implements ContainerConfigBuilder {

    protected final InjectionRegistryBuilder injectionRegistryBuilder = new InjectionRegistryBuilder(this);
    protected ResourceFactory resourceFactory = createResourceFactory();
    protected ResourceInject resourceInjection = createResourceInject();
    private InjectionRegister originalRegister = null;
    private InjectionRegister activeRegister = null;

    protected abstract ResourceFactory createResourceFactory();

    protected abstract ResourceInject createResourceInject();

    protected abstract void appendResources(InjectionRegister registerModule);

    @Override
    public void buildConfig() {
        originalRegister = injectionRegistryBuilder.build();
        appendResources(originalRegister);
        activeRegister = originalRegister.copy();
    }

    @Override
    public void initiateConfig() {
        register(injectionRegistryBuilder);
        injectionRegistryBuilder.register(
                registrations -> registrations.register(
                        new CustomInjectionPointFinderModule(createDefaultInjectionPointFinder())
                )
        );
    }


    public abstract void register(InjectionRegistryBuilder registryBuilder);

    @Override
    public T getActiveRegister() {
        return (T) activeRegister;
    }

    @Override
    public void addSingletonActiveRegistry() {
        RegistrationModuleAnnotation injectionRegisterModuleConfig = prepareModuleSingletonForRegistry();
        activeRegister.register(injectionRegisterModuleConfig);
    }

    private RegistrationModuleAnnotation prepareModuleSingletonForRegistry() {
        final InjectionRegister configBase = this.activeRegister;
        return new RegistrationModuleAnnotation() {
            @Override
            public void registrations() {
                register(InjectionRegister.class)
                        // .named("ActiveRegisterModule")
                        .scopeAs(ScopeContainer.Scope.SINGLETON)
                        .registerTypeAs(InjectionContainerManager.RegisterType.FINAL)
                        .withInstance(configBase);
            }
        };
    }

    @Override
    public void cleanActiveContainer() {
        activeRegister = originalRegister.copy();
    }

    public ResourceFactory getResourceFactory() {
        return resourceFactory;
    }

    protected InjectionFinder createDefaultInjectionPointFinder() {
        return new DefaultInjectionPointFinder(this);
    }
}
