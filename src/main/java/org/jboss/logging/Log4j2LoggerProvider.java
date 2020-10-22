/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2013 Red Hat, Inc.
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.ThreadContext;

final class Log4j2LoggerProvider implements LoggerProvider {

    private static final Supplier<Set<String>> GET_LOGGER_NAMES;
    private static final Function<String, Collection<Object>> GET_APPENDERS;

    static {
        GET_LOGGER_NAMES = AccessController.doPrivileged((PrivilegedAction<Supplier<Set<String>>>) () -> {
            try {
                final Class<?> loggerContext = Class.forName("org.apache.logging.log4j.core.LoggerContext", false, Log4j2LoggerProvider.class.getClassLoader());
                final Method getContext = loggerContext.getMethod("getContext", boolean.class);
                final Method getLoggers = loggerContext.getMethod("getLoggers");
                return () -> {
                    try {
                        final Object context = getContext.invoke(null, false);
                        @SuppressWarnings("unchecked")
                        final Collection<org.apache.logging.log4j.Logger> loggers = (Collection<org.apache.logging.log4j.Logger>) getLoggers.invoke(context);
                        return loggers.stream()
                                .map(org.apache.logging.log4j.Logger::getName)
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
                final Class<?> loggerContext = Class.forName("org.apache.logging.log4j.core.LoggerContext", false, Log4j2LoggerProvider.class.getClassLoader());
                final Class<?> logger = Class.forName("org.apache.logging.log4j.core.Logger", false, Log4j2LoggerProvider.class.getClassLoader());
                final Method getContext = loggerContext.getMethod("getContext", boolean.class);
                final Method getLogger = loggerContext.getMethod("getLogger", String.class);
                final Method getAppenders = logger.getMethod("getAppenders");
                return (name) -> {
                    try {
                        final Object context = getContext.invoke(null, false);
                        final Object l = getLogger.invoke(context, name);
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> appenders = (Map<String, Object>) getAppenders.invoke(l);
                        return new ArrayList<>(appenders.values());
                    } catch (Throwable ignore) {
                    }
                    return Collections.emptySet();
                };

            } catch (Throwable ignore) {
            }
            return (name) -> Collections.emptySet();
        });
    }

    @Override
    public Log4j2Logger getLogger(String name) {
        return new Log4j2Logger(name);
    }

    @Override
    public void clearMdc() {
        ThreadContext.clearMap();
    }

    @Override
    public Object putMdc(String key, Object value) {
        try {
            return ThreadContext.get(key);
        } finally {
            ThreadContext.put(key, String.valueOf(value));
        }
    }

    @Override
    public Object getMdc(String key) {
        return ThreadContext.get(key);
    }

    @Override
    public void removeMdc(String key) {
        ThreadContext.remove(key);
    }

    @Override
    public Map<String, Object> getMdcMap() {
        return new HashMap<>(ThreadContext.getImmutableContext());
    }

    @Override
    public void clearNdc() {
        ThreadContext.clearStack();
    }

    @Override
    public String getNdc() {
        return ThreadContext.peek();
    }

    @Override
    public int getNdcDepth() {
        return ThreadContext.getDepth();
    }

    @Override
    public String popNdc() {
        return ThreadContext.pop();
    }

    @Override
    public String peekNdc() {
        return ThreadContext.peek();
    }

    @Override
    public void pushNdc(String message) {
        ThreadContext.push(message);
    }

    @Override
    public void setNdcMaxDepth(int maxDepth) {
        ThreadContext.trim(maxDepth);
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
