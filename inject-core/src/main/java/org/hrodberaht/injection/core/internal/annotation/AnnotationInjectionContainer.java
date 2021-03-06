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

package org.hrodberaht.injection.core.internal.annotation;

import org.hrodberaht.injection.core.internal.InjectionContainer;
import org.hrodberaht.injection.core.internal.InjectionContainerBase;
import org.hrodberaht.injection.core.internal.InjectionContainerManager;
import org.hrodberaht.injection.core.internal.InjectionKey;
import org.hrodberaht.injection.core.internal.RegistrationInjectionContainer;
import org.hrodberaht.injection.core.internal.ScopeContainer;
import org.hrodberaht.injection.core.internal.ServiceRegister;
import org.hrodberaht.injection.core.internal.annotation.creator.InstanceCreator;
import org.hrodberaht.injection.core.internal.annotation.scope.FactoryScopeHandler;
import org.hrodberaht.injection.core.internal.annotation.scope.SingletonScopeHandler;
import org.hrodberaht.injection.core.internal.annotation.scope.VariableFactoryScopeHandler;
import org.hrodberaht.injection.core.internal.exception.DependencyLocationError;
import org.hrodberaht.injection.core.internal.exception.DuplicateRegistrationException;
import org.hrodberaht.injection.core.internal.exception.InjectRuntimeException;
import org.hrodberaht.injection.core.register.RegistrationModule;
import org.hrodberaht.injection.core.register.RegistrationModuleAnnotation;
import org.hrodberaht.injection.core.register.VariableInjectionFactory;
import org.hrodberaht.injection.core.register.internal.RegistrationInstanceSimple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple Java Utils - Container
 *
 * @author Robert Alexandersson
 * 2010-maj-29 12:38:22
 * @version 1.0
 * @since 1.0
 */
public class AnnotationInjectionContainer extends InjectionContainerBase
        implements InjectionContainer, RegistrationInjectionContainer {

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationInjectionContainer.class);

    private Map<InjectionKey, InjectionMetaDataBase> injectionMetaDataCache = new ConcurrentHashMap<InjectionKey, InjectionMetaDataBase>();
    private InjectionContainerManager container;
    private InjectionFinder injectionFinder;
    private InstanceCreator instanceCreator;

    public AnnotationInjectionContainer(InjectionContainerManager container) {
        super.registeredServices = new ServiceRegistryForInjection(injectionMetaDataCache);
        this.container = container;

    }

    public InjectionFinder getInjectionFinder() {
        return injectionFinder;
    }

    public InstanceCreator getInstanceCreator() {
        return instanceCreator;
    }

    public <T> T getService(Class<T> service, InjectionContainerManager.Scope forcedScope, String qualifier) {
        assert (qualifier != null);
        InjectionKey key = getNamedKey(qualifier, service);
        return getQualifiedService(service, forcedScope, key);
    }


    public <T> T getService(
            Class<T> service, InjectionContainerManager.Scope forcedScope, Class<? extends Annotation> qualifier) {
        assert (qualifier != null);
        InjectionKey key = getAnnotatedKey(qualifier, service);
        return getQualifiedService(service, forcedScope, key);
    }


    @SuppressWarnings(value = "unchecked")
    public <T> T getService(Class<T> service, InjectionContainerManager.Scope forcedScope) {
        InjectionKey key = getKey(service);
        ServiceRegister serviceRegister = findServiceRegister(service, key);
        return (T) instantiateService(forcedScope, serviceRegister, key);
    }

    @SuppressWarnings(value = "unchecked")
    public <T, K> T getService(Class<T> service, K variable) {
        InjectionKey key = getNamedKey(VariableInjectionFactory.SERVICE_NAME, service);
        ServiceRegister serviceRegister = findServiceRegister(service, key);
        return (T) instantiateService(variable, serviceRegister, key);
    }

    /**
     * Will find the service register but also perform registration where applicable
     *
     * @param service
     * @param key
     * @return the
     */
    public ServiceRegister findServiceRegister(Class service, InjectionKey key) {
        ServiceRegister serviceRegister = registeredServices.get(key);
        if (serviceRegister == null) {
            Object extendedInstance = injectionFinder != null ? injectionFinder.extendedInjection(key) : null;
            if (extendedInstance != null) {
                return createExtendedRegisterInstance(key, service, extendedInstance);
            } else if (Modifier.isAbstract(service.getModifiers()) && !service.isInterface()) {
                throw new DependencyLocationError(service.getName() +
                        " is abstract and not registered in container, fix this by registering an implementation");
            } else if (!service.isInterface()) {
                serviceRegister = register(key, false);
            } else if (service.isInterface()) {
                serviceRegister = registerForInterface(key, false);
            }
        }
        return serviceRegister;
    }

    private ServiceRegister createExtendedRegisterInstance(InjectionKey key, Class service, Object extendedInstance) {
        RegistrationInstanceSimple registrationInstanceSimple =
                (RegistrationInstanceSimple)
                        new RegistrationInstanceSimple(service)
                                .withFactoryInstance(extendedInstance)
                                .annotated(key.getAnnotation())
                                .registerTypeAs(InjectionContainerManager.RegisterType.EXTENDED)
                                .scopeAs(ScopeContainer.Scope.NEW);
        return createAnStoreRegistration(registrationInstanceSimple, key, null);
    }

    @SuppressWarnings(value = "unchecked")
    public Object createNewInstance(ServiceRegister serviceRegister, InjectionKey key) {
        AnnotationInjection annotationInjection = new AnnotationInjection(injectionMetaDataCache, container, this);
        return annotationInjection.createNewInstance(serviceRegister.getService(), key);
    }

    @SuppressWarnings(value = "unchecked")
    public Object createInstance(ServiceRegister serviceRegister, InjectionKey key) {
        AnnotationInjection annotationInjection = new AnnotationInjection(injectionMetaDataCache, container, this);
        return annotationInjection.createInstance(serviceRegister.getService(), key);
    }

    public void register(
            InjectionKey key, Class service,
            InjectionContainerManager.Scope scope, InjectionContainerManager.RegisterType type, boolean throwError) {
        if (registeredServices.containsKey(key)) {
            reRegisterSupport(key, type, throwError);
        }
        LOG.debug("Putting service into registry for key={}", key);
        RegistrationInstanceSimple registrationInstanceSimple =
                (RegistrationInstanceSimple)
                        new RegistrationInstanceSimple(key.getServiceDefinition())
                                .with(service)
                                .annotated(key.getAnnotation())
                                .registerTypeAs(type)
                                .scopeAs(scope);

        createAnStoreRegistration(registrationInstanceSimple, key, null);
    }

    public void register(InjectionContainerManager injectionContainerManager, RegistrationModule... modules) {
        for (RegistrationModule module : modules) {
            RegistrationModuleAnnotation aModule = (RegistrationModuleAnnotation) module;
            if (aModule.getInjectionFinder() != null) {
                if (this.injectionFinder != null) {
                    throw new IllegalStateException("Can not change injectionFinder once set");
                }
                this.injectionFinder = aModule.getInjectionFinder();
            }
            if (aModule.getInstanceCreator() != null) {
                if (this.instanceCreator != null) {
                    throw new IllegalStateException("Can not change instanceCreator once set");
                }
                this.instanceCreator = aModule.getInstanceCreator();
            }
            aModule.preRegistration(container);
            for (RegistrationInstanceSimple instance : aModule.getRegistrations()) {
                InjectionKey key = instance.getInjectionKey();
                LOG.debug("Putting service into registry for key={}", key.getServiceDefinition());
                createAnStoreRegistration(instance, key, aModule);
            }
            aModule.postRegistration(container);
        }
    }


    public Class findService(InjectionKey key) {
        ServiceRegister serviceRegister = super.registeredServices.get(key);
        if (serviceRegister != null) {
            return serviceRegister.getService();
        } else {
            throw new InjectRuntimeException("Service {0} with Name {1} not registered in SimpleInjection"
                    , key.getServiceDefinition(), key.getQualifier());
        }
    }

    public void injectDependencies(Object service) {
        AnnotationInjection annotationInjection = new AnnotationInjection(injectionMetaDataCache, container, this);
        annotationInjection.injectDependencies(service);
    }

    public void autowireAndPostConstruct(Object service) {
        AnnotationInjection annotationInjection = new AnnotationInjection(injectionMetaDataCache, container, this);
        annotationInjection.autowireAndPostConstruct(service);
    }

    @SuppressWarnings(value = "unchecked")
    private Object instantiateService(Object variable, ServiceRegister serviceRegister, InjectionKey key) {
        AnnotationInjection annotationInjection = new AnnotationInjection(injectionMetaDataCache, container, this);
        return annotationInjection.createInstance(serviceRegister.getService(), key, variable);
    }

    private void reRegisterSupport(InjectionKey key, InjectionContainerManager.RegisterType type, boolean throwError) {
        ServiceRegister serviceRegister = registeredServices.get(key);
        if (serviceRegister.getRegisterType() == InjectionContainerManager.RegisterType.WEAK) {
            return;
        }

        if (serviceRegister.getRegisterType() == InjectionContainerManager.RegisterType.NORMAL) {
            if (type == InjectionContainerManager.RegisterType.OVERRIDE_NORMAL) {
                return;
            }
            throwRegistrationError("a existing Service",
                    "to override register please use overrideRegister method", key, throwError);
        }
        if (serviceRegister.getRegisterType() == InjectionContainerManager.RegisterType.FINAL) {
            throwRegistrationError("a existing FINAL Service", "can not perform override registration", key, throwError);
        }

    }

    private void throwRegistrationError(String message, String help, InjectionKey key, boolean throwError) {
        String qualifier = key.getQualifier();
        if (!throwError) {
            return;
        }
        if (qualifier != null) {
            throw new DuplicateRegistrationException(
                    message + " for qualifier \"{0}\" and serviceDefinition \"{1}\" " +
                            "is already registered, " + help, qualifier, key.getServiceDefinition());
        } else {
            throw new DuplicateRegistrationException(
                    message + " for serviceDefinition \"{0}\" " +
                            "is already registered, " + help, key.getServiceDefinition());
        }
    }

    @SuppressWarnings(value = "unchecked")
    private <T> T getQualifiedService(Class<T> service, InjectionContainerManager.Scope forcedScope, InjectionKey key) {
        if (!registeredServices.containsKey(key) && service.isInterface()) {
            throw new InjectRuntimeException(
                    "Service {0} not registered in SimpleInjection and is an interface", service
            );
        }

        ServiceRegister serviceRegister = registeredServices.get(key);
        if (serviceRegister == null && !service.isInterface()) {
            serviceRegister = register(key, service, null);
        }
        return (T) instantiateService(forcedScope, serviceRegister, key);
    }

    @SuppressWarnings(value = "unchecked")
    private ServiceRegister registerForInterface(InjectionKey key, boolean forceNew) {
        Class serviceDefinition = key.getServiceDefinition();
        AnnotationInjection annotationInjection = new AnnotationInjection(injectionMetaDataCache, container, this);
        Class service = findServiceImplementation(serviceDefinition).getService();
        InjectionMetaData injectionMetaData = annotationInjection.findInjectionData(service, key);
        if (injectionMetaData != null) {
            register(key, injectionMetaData.getServiceClass(), null, null, false);
            return registeredServices.get(key);
        } else {
            throw new InjectRuntimeException("No Service found for Interface {0}", serviceDefinition);
        }
    }

    private ServiceRegister register(InjectionKey key, Class service, InjectionContainerManager.Scope scope) {
        RegistrationInstanceSimple instance = new RegistrationInstanceSimple(service);
        instance.scopeAs(scope);
        if (key == null) {
            key = instance.getInjectionKey();
        }

        if (key.getAnnotation() != null) {
            instance.annotated(key.getAnnotation());
        } else if (key.getName() != null) {
            instance.named(key.getName());
        }

        return createAnStoreRegistration(instance, key, null);
    }

    private ServiceRegister register(InjectionKey key, boolean throwError) {
        register(key, key.getServiceDefinition(), null, null, throwError);
        return registeredServices.get(key);
    }


    private ServiceRegister createAnStoreRegistration(RegistrationInstanceSimple instance,
                                                      InjectionKey key,
                                                      RegistrationModuleAnnotation aModule) {
        InjectionMetaData injectionMetaData = createInjectionMetaData(instance, key);
        ServiceRegister register;
        if (aModule != null) {
            register = createServiceRegister(instance, injectionMetaData, false);
            register.setModule(aModule);
        } else {
            register = createServiceRegister(instance, injectionMetaData, true);
        }
        putServiceIntoRegister(key, register);
        validateRegisters(key, injectionMetaData, register);
        return register;

    }

    private void validateRegisters(InjectionKey key, InjectionMetaData injectionMetaData, ServiceRegister register) {

        // Validate the MetaDataCache
        AnnotationInjection annotationInjection = new AnnotationInjection(injectionMetaDataCache, container, this);
        InjectionMetaData metaData = annotationInjection.getInjectionCacheHandler().find(injectionMetaData);
        if (metaData != injectionMetaData) {
            throw new InjectRuntimeException("InjectionMetaData not cached correctly");
        }

        // Validate the ServiceRegister cache
        ServiceRegister serviceRegisterCache = registeredServices.get(key);
        if (serviceRegisterCache != register) {
            throw new InjectRuntimeException("ServiceRegister not cached correctly");
        }


    }


    private ServiceRegister createServiceRegister(
            RegistrationInstanceSimple instance, InjectionMetaData injectionMetaData, boolean preCreateSingletons) {
        if (preCreateSingletons && instance.getScope() == ScopeContainer.Scope.SINGLETON) {
            return new ServiceRegister(
                    instance.getService(),
                    createInstance(new ServiceRegister(instance.getService()), instance.getInjectionKey()),
                    InjectionContainerManager.Scope.SINGLETON,
                    normalizeType(instance.getRegisterType())
            );
        } else {
            return new ServiceRegister(
                    instance.getService(),
                    null,
                    getAnnotationScope(injectionMetaData),
                    normalizeType(instance.getRegisterType())
            );
        }
    }

    private InjectionMetaData createInjectionMetaData(RegistrationInstanceSimple instance, InjectionKey key) {
        InjectionMetaData injectionMetaData = createInjectionMetaData(instance.getService(), key);
        if (instance.getRegisterType() == InjectionContainerManager.RegisterType.EXTENDED) {
            FactoryScopeHandler scopeHandler = new FactoryScopeHandler(instance.getTheFactory());
            injectionMetaData.setScopeHandler(scopeHandler);
            injectionMetaData.setExtendedInjection(true);
        } else if (instance.getTheInstance() != null) {
            SingletonScopeHandler scopeHandler = new SingletonScopeHandler();
            scopeHandler.addInstance(instance.getTheInstance());
            injectionMetaData.setScopeHandler(scopeHandler);
        } else if (instance.getTheFactory() != null) {
            FactoryScopeHandler scopeHandler = new FactoryScopeHandler(instance.getTheFactory());
            injectionMetaData.setScopeHandler(scopeHandler);
        } else if (instance.getTheVariableFactory() != null) {
            VariableFactoryScopeHandler scopeHandler = new VariableFactoryScopeHandler(instance.getTheVariableFactory());
            injectionMetaData.setScopeHandler(scopeHandler);
        } else {
            // replaces the injection data scope handler
            injectionMetaData.setScopeHandler(InjectionUtils.getScopeHandler(instance.getService(), instance.getScope()));
        }
        return injectionMetaData;
    }

    private InjectionMetaData createInjectionMetaData(Class service, InjectionKey key) {
        AnnotationInjection annotationInjection = new AnnotationInjection(injectionMetaDataCache, container, this);
        return annotationInjection.createInjectionMetaData(service, key, null);
    }


    private InjectionContainerManager.Scope getAnnotationScope(InjectionMetaData injectionMetaData) {
        return injectionMetaData.getScope();
    }

    public AnnotationInjectionContainer copy(InjectionContainerManager injectionContainerManager) {
        AnnotationInjectionContainer annotationInjectionContainer = new AnnotationInjectionContainer(injectionContainerManager);
        annotationInjectionContainer.injectionFinder = this.injectionFinder;
        annotationInjectionContainer.instanceCreator = this.instanceCreator;
        annotationInjectionContainer.injectionMetaDataCache.putAll(this.injectionMetaDataCache);
        for (Map.Entry<InjectionKey, InjectionMetaDataBase> metaDatantry : this.injectionMetaDataCache.entrySet()) {
            InjectionMetaDataBase injectionMetaData = metaDatantry.getValue();
            if (injectionMetaData.getInjectionMetaData().getScope() == ScopeContainer.Scope.SINGLETON) {
                annotationInjectionContainer.injectionMetaDataCache.put(metaDatantry.getKey(), injectionMetaData.copy());
            } else {
                annotationInjectionContainer.injectionMetaDataCache.put(metaDatantry.getKey(), injectionMetaData);
            }
        }

        for (InjectionKey injectionKey : this.registeredServices.keySet()) {
            ServiceRegister serviceRegister = this.registeredServices.get(injectionKey);
            // If the key exists but not the service, it just means we have injected dependencies into it, but do not manage the lifecycle of it
            if (serviceRegister != null) {
                if (serviceRegister.getScope() == ScopeContainer.Scope.SINGLETON) {
                    annotationInjectionContainer.registeredServices.put(injectionKey, serviceRegister.copy());
                } else {
                    annotationInjectionContainer.registeredServices.put(injectionKey, serviceRegister);
                }
            }
        }
        return annotationInjectionContainer;
    }


}