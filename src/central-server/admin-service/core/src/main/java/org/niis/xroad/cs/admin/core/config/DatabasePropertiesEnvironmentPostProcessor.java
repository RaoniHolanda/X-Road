/**
 * The MIT License
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.cs.admin.core.config;

import ee.ria.xroad.common.SystemProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Profile("nontest")
public class DatabasePropertiesEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Map<String, String> SIMPLE_DB_PROPERTY_NAMES_TO_SPRING_PROPERTIES;

    private static final String HOST_DB_PROPERTY = "host";
    private static final String PORT_DB_PROPERTY = "port";
    private static final String DATABASE_DB_PROPERTY = "database";
    private static final String SCHEMA_DB_PROPERTY = "schema";
    private static final String USERNAME_DB_PROPERTY = "username";
    private static final String PUBLIC_SCHEMA = "public";
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "5432";
    private static final String CONNECTION_URL_PREFIX = "jdbc:postgresql://";
    private static final String SPRING_DATASOURCE_URL = "spring.datasource.url";
    private static final String SPRING_SCHEMA = "spring.datasource.hikari.data-source-properties.currentSchema";

    static {
        final HashMap<String, String> tmp = new HashMap<>();
        tmp.put(USERNAME_DB_PROPERTY, "spring.datasource.username");
        tmp.put("password", "spring.datasource.password");
        SIMPLE_DB_PROPERTY_NAMES_TO_SPRING_PROPERTIES = Collections.unmodifiableMap(tmp);
        // url = jdbc:postgresql://<host>:<port>/<database>
    }

    String getPropertySourceName() {
        return "fromDbPropertiesFile";
    }

    String getPropertyFilePath() {
        return SystemProperties.getCenterDatabasePropertiesFile();
    }

    boolean hasSimpleOneToOneMapping(String propertyName) {
        return SIMPLE_DB_PROPERTY_NAMES_TO_SPRING_PROPERTIES.containsKey(propertyName);
    }

    String mapToSpringPropertyName(String originalPropertyName) {
        return SIMPLE_DB_PROPERTY_NAMES_TO_SPRING_PROPERTIES.get(originalPropertyName);
    }

    private String createSchemaProperty(Properties dbProperties) {
        String schema = dbProperties.getProperty(SCHEMA_DB_PROPERTY,
                dbProperties.getProperty(USERNAME_DB_PROPERTY));
        if (!PUBLIC_SCHEMA.equals(schema)) {
            schema = schema + "," + PUBLIC_SCHEMA;
        }
        return schema;
    }

    private String createConnectionUrl(Properties dbProperties) {
        StringBuilder b = new StringBuilder();
        b.append(CONNECTION_URL_PREFIX);
        String host = dbProperties.getProperty(HOST_DB_PROPERTY, DEFAULT_HOST);
        String port = dbProperties.getProperty(PORT_DB_PROPERTY, DEFAULT_PORT);
        String database = dbProperties.getProperty(DATABASE_DB_PROPERTY);
        if (database == null) throw new NullPointerException();
        b.append(host);
        b.append(":");
        b.append(port);
        String secondaryHosts = dbProperties.getProperty("secondary_hosts");
        if (secondaryHosts != null) {
            Arrays.stream(secondaryHosts.split(",")).forEach((secondaryHost) -> {
                b.append(",");
                b.append(secondaryHost);
                if (!secondaryHost.contains(":")) {
                    b.append(":");
                    b.append(port);
                }
            });
        }
        b.append("/");
        b.append(database);
        return b.toString();
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        // we read properties from file only if not testing
        if (environment.acceptsProfiles(Profiles.of("nontest"))) {
            // called twice since IntelliJ tests load the class twice
            CentralServerSystemPropertiesInitializer.initialize();
            try {
                Properties originalProperties = new Properties();

                try (FileInputStream originalPropertiesStream = new FileInputStream(getPropertyFilePath())) {
                    originalProperties.load(originalPropertiesStream);
                }

                // simple properties
                Map<String, Object> springPropertiesMap = new HashMap<>();
                for (String originalPropertyName : originalProperties.stringPropertyNames()) {
                    if (hasSimpleOneToOneMapping(originalPropertyName)) {
                        String value = originalProperties.getProperty(originalPropertyName);
                        springPropertiesMap.put(
                                mapToSpringPropertyName(originalPropertyName),
                                value);
                    }
                }

                // connection url
                springPropertiesMap.put(SPRING_DATASOURCE_URL, createConnectionUrl(originalProperties));

                // schema
                springPropertiesMap.put(SPRING_SCHEMA, createSchemaProperty(originalProperties));

                environment.getPropertySources().addFirst(new MapPropertySource(
                        getPropertySourceName(), springPropertiesMap));

            } catch (Exception e) {
                log.error("Failed to process environment properties", e);
            }
        }
    }
}
