package org.hrodberaht.injection.extensions.junit.spring.service;

/**
 * Unit Test JUnit (using @Inject)
 *
 * @author Robert Alexandersson
 *         2010-okt-11 19:24:39
 * @version 1.0
 * @since 1.0
 */
public interface SpringServiceInterface {
    void doSomething();

    String findSomething(Long id);

    void doSomethingDeep();

    String findSomethingDeep(Long id);

    String findSomethingDeepWithDataSource(Long id);

    void updateSomethingInDataSource(Long id, String name);
}