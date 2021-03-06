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

package org.hrodberaht.injection.extensions.plugin.junit.datasource.service;

import org.hrodberaht.injection.extensions.plugin.junit.model.User;
import org.hrodberaht.injection.plugin.datasource.jdbc.InsertOrUpdater;
import org.hrodberaht.injection.plugin.datasource.jdbc.JDBCService;
import org.hrodberaht.injection.plugin.datasource.jdbc.JDBCServiceFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Singleton;
import javax.sql.DataSource;

@Singleton
public class UserServiceBean implements UserService {

    @Resource
    private DataSource dataSource;

    @Resource(name = "MyDataSource")
    private DataSource namedDataSource;

    private JDBCService jdbcService;


    @PostConstruct
    public void init() {
        jdbcService = JDBCServiceFactory.of(dataSource);
    }

    public User find(String userName) {

        return jdbcService.querySingle(
                "select * from user where username = ?",
                (rs, iteration) -> new User(rs.getString("username"), rs.getString("password")),
                userName);

    }

    public void changePassword(String userName, String newPassword) {
        InsertOrUpdater insertOrUpdater = jdbcService.createInsertOrUpdate("user");
        insertOrUpdater.where("username", userName);
        insertOrUpdater.field("password", newPassword);
        jdbcService.insertOrUpdate(insertOrUpdater);
    }

    @Override
    public int loginCount(String userName) {
        jdbcService.execute("update user set loginCount = loginCount + 1");
        return jdbcService.querySingle(
                "select loginCount from user where username = ?",
                (rs, iteration) -> rs.getInt("loginCount"),
                userName);
    }

    public boolean loginCompare(String userName, String password) {
        User user = find(userName);
        if (user == null) {
            return false;
        }
        return user.getPassword().equals(password);
    }

    @Override
    public DataSource getTypedDataSource() {
        return dataSource;
    }

    @Override
    public DataSource getNamedDataSource() {
        return namedDataSource;
    }

    @Override
    public void createUser(String userName, String password) {
        InsertOrUpdater insertOrUpdater = jdbcService.createInsertOrUpdate("user");
        insertOrUpdater.field("username", userName);
        insertOrUpdater.field("password", password);
        jdbcService.insertOrUpdate(insertOrUpdater);
    }

}
