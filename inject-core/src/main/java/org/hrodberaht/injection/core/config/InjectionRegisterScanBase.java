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

import org.hrodberaht.injection.core.InjectContainer;
import org.hrodberaht.injection.core.internal.InjectionContainerManager;
import org.hrodberaht.injection.core.internal.InjectionRegisterModule;
import org.hrodberaht.injection.core.internal.ScopeContainer;
import org.hrodberaht.injection.core.register.InjectionRegister;
import org.hrodberaht.injection.core.register.RegistrationModuleAnnotation;
import org.hrodberaht.injection.core.spi.InjectionRegisterScanInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Injection Extension JUnit
 *
 * @author Robert Alexandersson
 * 2010-okt-26 19:09:57
 * @version 1.0
 * @since 1.0
 */
public abstract class InjectionRegisterScanBase<T extends InjectionRegisterScanBase> extends InjectionRegisterModule implements InjectionRegisterScanInterface, ScanningService {

    private static final Logger LOG = LoggerFactory.getLogger(InjectionRegisterScanBase.class);

    private InjectionRegister referedRegister;
    private ClassScanner classScanner = new ClassScanner();

    protected InjectionRegisterScanBase() {
    }


    public InjectionRegisterScanBase(InjectionRegister register) {
        super(register);
        referedRegister = register;
    }

    public abstract T copy();

    @Override
    public InjectContainer getInjectContainer() {
        return container;
    }

    public void setInjectContainer(InjectContainer injectContainer) {
        super.container = (InjectionContainerManager) injectContainer;
    }

    public T scanPackage(String... packagenames) {
        for (String packagename : packagenames) {

            List<Class> listOfClasses = classScanner.getClasses(packagename);
            for (Class aClazz : listOfClasses) {
                ClassScanningUtil.createRegistration(aClazz, listOfClasses, this);
            }
        }
        return (T) this;
    }

    public T scanPackageExclude(String packagename, Class... manuallyexcluded) {
        List<Class> listOfClasses = classScanner.getClasses(packagename);
        List<Class> listOfFilteredClasses = new ArrayList<>(listOfClasses.size() - manuallyexcluded.length);
        // remove the manual excludes
        for (Class aClazz : listOfClasses) {
            if (!manuallyExcluded(aClazz, manuallyexcluded)) {
                listOfFilteredClasses.add(aClazz);
            }
        }
        for (Class aClazz : listOfFilteredClasses) {
            ClassScanningUtil.createRegistration(aClazz, listOfFilteredClasses, this);
        }
        return (T) this;
    }

    public void registerForScanner(Class aClazz, Class serviceClass, ScopeContainer.Scope scope) {
        RegistrationModuleAnnotation registrationModule = new RegistrationModuleAnnotation() {
            @Override
            public void registrations() {
                register(aClazz).scopeAs(scope).with(serviceClass);
            }
        };
        container.register(registrationModule);
    }


    private boolean manuallyExcluded(Class aClazz, Class[] manuallyexluded) {
        for (Class excluded : manuallyexluded) {
            if (excluded == aClazz) {
                return true;
            }
        }
        return false;
    }


    public void overrideRegister(final Class serviceDefinition, final Object service) {
        RegistrationModuleAnnotation registrationModule = new RegistrationModuleAnnotation() {
            @Override
            public void registrations() {
                register(serviceDefinition).withInstance(service);
            }
        };
        container.register(registrationModule);
    }


}
