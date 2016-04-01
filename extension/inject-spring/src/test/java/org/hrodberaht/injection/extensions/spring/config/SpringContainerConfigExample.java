package org.hrodberaht.injection.extensions.spring.config;

import org.hrodberaht.injection.InjectContainer;
import org.hrodberaht.injection.extensions.junit.internal.JunitSQLContainerService;
import org.hrodberaht.injection.extensions.junit.internal.ProxyResourceCreator;
import org.hrodberaht.injection.extensions.spring.SpringContainerConfigBase;
import org.hrodberaht.injection.spi.ResourceCreator;

import javax.sql.DataSource;

/**
 * Unit Test JUnit (using @Inject)
 *
 * @author Robert Alexandersson
 *         2010-okt-11 19:37:42
 * @version 1.0
 * @since 1.0
 */
public class SpringContainerConfigExample extends SpringContainerConfigBase {

    private JunitSQLContainerService junitSQLContainerService;

    public SpringContainerConfigExample() {
        junitSQLContainerService = new JunitSQLContainerService(this);
        String dataSourceName = "jdbc/MyDataSource";
        DataSource dataSource = createDataSource(dataSourceName);
        // Named resource
        addResource(dataSourceName, dataSource);

        junitSQLContainerService.addSQLSchemas(dataSourceName, "sql");

        // Adding the spring config, will automate the resource registration to also be done to the spring config
        loadSpringConfig("/META-INF/spring-config.xml");

    }

    @Override
    public void addSingletonActiveRegistry() {
        junitSQLContainerService.addSingletonActiveEntityManagers();
        super.addSingletonActiveRegistry();
    }

    @Override
    public InjectContainer createContainer() {
        return createAutoScanContainer("org.hrodberaht.injection.extensions.spring.testservices.simple");
    }

    @Override
    protected ResourceCreator createResourceCreator() {
        return new ProxyResourceCreator();
    }
}
