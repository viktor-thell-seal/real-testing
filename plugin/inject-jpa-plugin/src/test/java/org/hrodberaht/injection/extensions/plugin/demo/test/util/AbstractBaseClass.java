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

package org.hrodberaht.injection.extensions.plugin.demo.test.util;

import org.hrodberaht.injection.extensions.plugin.demo.test.config.CourseContainerConfigExample;
import org.hrodberaht.injection.plugin.junit.ContainerContext;
import org.hrodberaht.injection.plugin.junit.JUnit4Runner;
import org.junit.runner.RunWith;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;

import static org.hrodberaht.injection.extensions.plugin.demo.test.config.CourseContainerConfigExample.DATASOURCE_NAME;

@ContainerContext(CourseContainerConfigExample.class)
@RunWith(JUnit4Runner.class)
public abstract class AbstractBaseClass {

    @Resource(name = DATASOURCE_NAME)
    private DataSource dataSource;

    protected String init = null;

    @PostConstruct
    protected void init() {
        init = "initiated";
    }

}