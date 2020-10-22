/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2010 Red Hat, Inc.
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

package org.jboss.logging;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.LocationAwareLogger;

final class Slf4jLoggerProvider extends AbstractLoggerProvider implements LoggerProvider {

    private static final Supplier<Set<String>> GET_LOGGER_NAMES;
    private static final Function<String, Collection<Object>> GET_APPENDERS;

    static {
        GET_LOGGER_NAMES = AccessController.doPrivileged((PrivilegedAction<Supplier<Set<String>>>) () -> {
            try {
                final ClassLoader cl = Slf4jLoggerProvider.class.getClassLoader();
                final Class<?> loggerFactory = Class.forName("org.slf4j.LoggerFactory", false, cl);
                final Class<?> loggerContext = Class.forName("ch.qos.logback.classic.LoggerContext", false, cl);
                final Method getILoggerFactory = loggerFactory.getMethod("getILoggerFactory");
                final Method getLoggerList = loggerContext.getMethod("getLoggerList");
                return () -> {
                    try {
                        final Object context = getILoggerFactory.invoke(null);
                        @SuppressWarnings("unchecked")
                        final Collection<org.slf4j.Logger> loggers = (Collection<org.slf4j.Logger>) getLoggerList.invoke(context);
                        return loggers.stream()
                                .map(org.slf4j.Logger::getName)
                                .collect(Collectors.toSet());
                    } catch (Throwable ignore) {
                    }
                    return Collections.emptySet();
                };
            } catch (Throwable ignore) {
            }
            return Collections::emptySet;
        });

        GET_APPENDERS = AccessController.doPrivileged((PrivilegedAction<Function<String, Collection<Object>>>) () -> {
            try {
                final ClassLoader cl = Slf4jLoggerProvider.class.getClassLoader();
                final Class<?> loggerFactory = Class.forName("org.slf4j.LoggerFactory", false, cl);
                final Class<?> logger = Class.forName("ch.qos.logback.classic.Logger", false, cl);
                final Method getLogger = loggerFactory.getMethod("getLogger", String.class);
                final Method iteratorForAppenders = logger.getMethod("iteratorForAppenders");
                return (name) -> {
                    try {
                        final Object l = getLogger.invoke(null, name);
                        @SuppressWarnings("unchecked")
                        final Iterator<Object> appenders = (Iterator<Object>) iteratorForAppenders.invoke(l);
                        final Collection<Object> result = new ArrayList<>();
                        while (appenders.hasNext()) {
                            result.add(appenders.next());
                        }
                        return result;
                    } catch (Throwable ignore) {
                    }
                    return Collections.emptySet();
                };

            } catch (Throwable ignore) {
            }
            return (name) -> Collections.emptySet();
        });
    }

    public Logger getLogger(final String name) {
        org.slf4j.Logger l = LoggerFactory.getLogger(name);
        if (l instanceof LocationAwareLogger) {
            return new Slf4jLocationAwareLogger(name, (LocationAwareLogger) l);
        }
        return new Slf4jLogger(name, l);
    }

    public void clearMdc() {
        MDC.clear();
    }

    public Object putMdc(final String key, final Object value) {
        try {
            return MDC.get(key);
        } finally {
            if (value == null) {
                MDC.remove(key);
            } else {
                MDC.put(key, String.valueOf(value));
            }
        }
    }

    public Object getMdc(final String key) {
        return MDC.get(key);
    }

    public void removeMdc(final String key) {
        MDC.remove(key);
    }

    public Map<String, Object> getMdcMap() {
        final Map<String, String> copy = MDC.getCopyOfContextMap();
        return copy == null ? Collections.emptyMap() : new LinkedHashMap<>(copy);
    }

    @Override
    public Set<String> getLoggerNames() {
        return GET_LOGGER_NAMES.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Collection<T> getHandlers(final String name) {
        return (Collection<T>) GET_APPENDERS.apply(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Collection<T> getHandlers(final Logger logger) {
        return (Collection<T>) GET_APPENDERS.apply(logger.getName());
    }
}
