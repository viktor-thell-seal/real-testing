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

package org.hrodberaht.injection.plugin.junit.liquibase;

import liquibase.Liquibase;
import liquibase.database.core.HsqlDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.hrodberaht.injection.core.internal.exception.InjectRuntimeException;
import org.hrodberaht.injection.plugin.datasource.jdbc.JDBCService;
import org.hrodberaht.injection.plugin.datasource.jdbc.JDBCServiceFactory;
import org.hrodberaht.injection.plugin.datasource.jdbc.internal.JDBCException;
import org.hrodberaht.injection.plugin.junit.ResourceWatcher;
import org.hrodberaht.injection.plugin.junit.datasource.DataSourceProxyInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

class LiquibaseRunner {

    private static final Logger LOG = LoggerFactory.getLogger(LiquibaseRunner.class);

    private final String verificationQuery;
    private final String liquibaseStorageName;
    private final File tempStore;
    private final ResourceWatcher resourceWatcher;

    LiquibaseRunner(String verificationQuery, String liquibaseStorageName, ResourceWatcher resourceWatcher) {
        this.verificationQuery = verificationQuery;
        this.liquibaseStorageName = liquibaseStorageName;
        this.tempStore = new File(this.liquibaseStorageName);
        this.resourceWatcher = resourceWatcher;
    }

    void liquiBaseSchemaCreation(DataSource dataSource, String liquiBaseSchema) throws SQLException, LiquibaseException {

        if (!(dataSource instanceof DataSourceProxyInterface)) {
            throw new InjectRuntimeException("DataSource is not correct for JUnit testing, muse be " + DataSourceProxyInterface.class.getName());
        }

        DataSourceProxyInterface dataSourceProxyInterface = (DataSourceProxyInterface) dataSource;

        Boolean isLoaded = isLoaded(dataSourceProxyInterface);
        if (!isLoaded) {
            loadSchemaDataStore(dataSourceProxyInterface, liquiBaseSchema);
        } else {
            LOG.info("Will not run Liquibase update on schema, as its already loaded : {}", liquiBaseSchema);
        }
    }

    private Boolean isLoaded(DataSourceProxyInterface dataSourceProxyInterface) {
        Boolean isLoadedAlready = false;
        try {
            JDBCService jdbcService = JDBCServiceFactory.of(dataSourceProxyInterface);
            isLoadedAlready = jdbcService.querySingle(verificationQuery
                    ,
                    (rs, iteration) -> {
                        rs.getString(1);
                        return true;
                    }
            );
        } catch (JDBCException e) {
            return false;
        }
        return isLoadedAlready == null ? false : isLoadedAlready;
    }

    private void loadSchemaDataStore(DataSourceProxyInterface dataSource, String liquiBaseSchema) throws SQLException, LiquibaseException {
        if (isTempStoreValid()) {
            LOG.info("--------------- RUNNING RESTORE -------------");
            LOG.info("Restoring schema from backup file : {}", liquiBaseSchema);
            readFromFile(dataSource);
        } else {
            loadSchemaFromConfig(dataSource, liquiBaseSchema);
            if (tempStore.exists() && !tempStore.delete()) {
                LOG.debug("could not delete " + tempStore.getPath());
            }
            dataSource.createSnapshot(liquibaseStorageName);
        }
    }

    private void readFromFile(DataSourceProxyInterface dataSource) {
        dataSource.loadSnapshot(liquibaseStorageName);
    }

    private boolean isTempStoreValid() {
        return tempStore.exists() && !resourceWatcher.hasChanged();
    }


    private void loadSchemaFromConfig(DataSourceProxyInterface dataSource, String liquiBaseSchema) {

        try {
            DataSourceProxyInterface dataSourceProxyInterface = init(dataSource);
            dataSourceProxyInterface.runWithConnectionAndCommit(con -> {
                        LOG.info("--------------- RUNNING LIQUIBASE -------------");
                        LOG.info("Running Liquidbase update on schema: {}", liquiBaseSchema);
                        return runLuqibaseUpdateWithConnection(liquiBaseSchema, con);
                    }
            );
        } catch (Exception e) {
            throw new InjectRuntimeException(e);
        }

    }

    private boolean runLuqibaseUpdateWithConnection(String liquiBaseSchema, Connection con) {
        try {
            HsqlDatabase hsqlDatabase = new HsqlDatabase() {
                // This feature exists in a PR against liquibase
                public boolean failOnDefferable() {
                    return false;
                }
            };
            hsqlDatabase.setConnection(new JdbcConnection(con));
            Liquibase liquibase = new Liquibase(liquiBaseSchema,
                    new ClassLoaderResourceAccessor(), hsqlDatabase);
            liquibase.update("");
        } catch (LiquibaseException e) {
            throw new InjectRuntimeException(e);
        }
        return true;
    }

    private DataSourceProxyInterface init(DataSourceProxyInterface dataSource) throws SQLException {
        // Connection has to be called to initialize the proxy to be able to call getNativeConnection
        dataSource.getConnection();
        return dataSource;
    }

}
