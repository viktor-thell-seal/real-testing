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

package org.hrodberaht.injection.plugin.datasource.embedded.vendors;

import org.hrodberaht.injection.plugin.datasource.DataSourceException;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

public class TestDataSourceWrapper implements javax.sql.DataSource {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(TestDataSourceWrapper.class);

    private final VendorDriverManager vendorDriverManager;
    private final String name;
    private ThreadLocal<TestConnection> connectionThread = new ThreadLocal<>();

    public TestDataSourceWrapper(String name, VendorDriverManager vendorDriverManager) {
        this.name = name;
        this.vendorDriverManager = vendorDriverManager;
    }

    private void initConnection() {
        try {
            if (connectionThread.get() == null) {
                this.connectionThread.set(vendorDriverManager.getConnection());
                LOG.info("Creating connection {} - {}", this, connectionThread.get());
                this.connectionThread.get().setAutoCommit(false);
            }
            LOG.info("reusing connection {} - {}", this, connectionThread.get());
        } catch (SQLException e) {
            throw new DataSourceException(e);
        }
    }

    public void commitNativeConnection() {
        try {
            if (connectionThread.get() != null) {
                connectionThread.get().commitIt();
            }
        } catch (SQLException e) {
            throw new DataSourceException(e);
        }
    }

    public void clearDataSource() {
        LOG.info("clearDataSource {} - {}", this, connectionThread.get());
        TestConnection connection = connectionThread.get();
        if (connection != null) {
            connection.dontFailRollback();
            connection.dontFailClose();
            connectionThread.remove();
        }
    }


    @Override
    public Connection getConnection() throws SQLException {
        initConnection();
        return connectionThread.get();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        initConnection();
        return connectionThread.get();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {

    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {

    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(this)) + "(" + name + ")";
    }

    public String getName() {
        return name;
    }
}

