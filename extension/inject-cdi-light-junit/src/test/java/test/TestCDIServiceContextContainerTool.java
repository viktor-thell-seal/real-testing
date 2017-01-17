package test;

import org.hrodberaht.injection.extensions.junit.ContainerContext;
import org.hrodberaht.injection.extensions.junit.JUnitRunner;
import org.hrodberaht.injection.extensions.junit.util.ContainerLifeCycleTestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import test.config.CDIContainerConfigExample;
import test.service.CDIServiceInterface;
import test.service.SimpleServiceSingleton;

import javax.inject.Inject;
import javax.sql.DataSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit Test JUnit (using @Inject)
 *
 * @author Robert Alexandersson
 *         2010-okt-11 19:31:30
 * @version 1.0
 * @since 1.0
 */
@ContainerContext(CDIContainerConfigExample.class)
@RunWith(JUnitRunner.class)
public class TestCDIServiceContextContainerTool {

    @Inject
    private CDIServiceInterface anInterface;

    @Inject
    private ContainerLifeCycleTestUtil containerLifeCycleTestUtil;

    @Test
    public void testReWiringManualMock() {
        String something = anInterface.findSomething(12L);
        assertEquals("Something", something);

        String somethingDeep = anInterface.findSomethingDeep(12L);
        assertEquals("initialized", somethingDeep);

        containerLifeCycleTestUtil.registerServiceInstance(CDIServiceInterface.class, new CDIServiceInterface(){

            @Override
            public String findSomething(long l) {
                return "Mocking";
            }

            @Override
            public String findSomethingDeep(long l) {
                return "DeepMocking";
            }

            @Override
            public DataSource getDataSource() {
                return null;
            }

            @Override
            public SimpleServiceSingleton getLoadedPostContainer() {
                return null;
            }
        });

        assertEquals("Mocking", containerLifeCycleTestUtil.getService(CDIServiceInterface.class)
                .findSomething(14L));

        assertEquals("DeepMocking", containerLifeCycleTestUtil.getService(CDIServiceInterface.class)
                .findSomethingDeep(14L));

        assertNull(containerLifeCycleTestUtil.getService(CDIServiceInterface.class)
                .getDataSource());

        assertNull(containerLifeCycleTestUtil.getService(CDIServiceInterface.class)
                .getLoadedPostContainer());


    }

    @Test
    public void testReWiringRegistration() {
        String something = anInterface.findSomething(12L);
        assertEquals("Something", something);

        String somethingDeep = anInterface.findSomethingDeep(12L);
        assertEquals("initialized", somethingDeep);

        /* TO COME
        containerLifeCycleTestUtil.registerModule();

        assertEquals("Mocking", containerLifeCycleTestUtil.getService(CDIServiceInterface.class)
                .findSomething(14L));

        assertEquals("DeepMocking", containerLifeCycleTestUtil.getService(CDIServiceInterface.class)
                .findSomethingDeep(14L));

        assertNull(containerLifeCycleTestUtil.getService(CDIServiceInterface.class)
                .getDataSource());

        assertNull(containerLifeCycleTestUtil.getService(CDIServiceInterface.class)
                .getLoadedPostContainer());
        */

    }

}
