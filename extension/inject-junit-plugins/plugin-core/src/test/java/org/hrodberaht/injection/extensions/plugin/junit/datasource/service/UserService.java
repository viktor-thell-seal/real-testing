package org.hrodberaht.injection.extensions.plugin.junit.datasource.service;

import org.hrodberaht.injection.extensions.plugin.junit.model.User;

import javax.sql.DataSource;

/**
 * Qmatic Booking Module
 *
 * @author Robert Alexandersson
 * 2011-05-08 01:53
 * @created 1.0
 * @since 1.0
 */


public interface UserService {

    User find(String userName);

    void changePassword(String userName, String newPassword);

    boolean loginCompare(String userName, String password);

    DataSource getTypedDataSource();

    DataSource getNamedDataSource();

}
