package org.hrodberaht.injection.extensions.spring.testservices.spring;

import org.hrodberaht.injection.extensions.spring.instance.SpringInject;
import org.hrodberaht.injection.extensions.spring.testservices.simple.AnyServiceInner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Created by alexbrob on 2016-03-29.
 */
@Component
public class SpringBean {

    @SpringInject
    private AnyServiceInner anyServiceInner;

    @Autowired
    @Qualifier("MyDataSource")
    private DataSource dataSource;

    public String getName(){
        return "SpringBeanName";
    }

    public AnyServiceInner getAnyServiceInner() {
        return anyServiceInner;
    }
}
