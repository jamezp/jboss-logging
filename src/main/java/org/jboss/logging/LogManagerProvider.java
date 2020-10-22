/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.logging;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides an API to query the back end log manager.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface LogManagerProvider {

    /**
     * Returns a provider.
     *
     * @return the provider
     */
    static LogManagerProvider getInstance() {
        return LoggerProviders.PROVIDER;
    }

    /**
     * Returns a set of loggers that have been created on the log manager.
     *
     * @return the loggers set
     */
    default Set<Logger> getLoggers() {
        return getLoggerNames()
                .stream()
                .map(Logger::getLogger)
                .collect(Collectors.toSet());
    }

    /**
     * Returns a set of loggers that have been created on the log manager.
     *
     * @return the loggers set
     */
    Set<String> getLoggerNames();

    /**
     * Returns a collection of handlers for the logger name.
     *
     * @param name the name of the logger
     * @param <T>  the handler type
     *
     * @return the collection of handlers
     */
    <T> Collection<T> getHandlers(String name);

    /**
     * Returns a collection of handlers for the logger.
     *
     * @param logger the logger
     * @param <T>    the handler type
     *
     * @return the collection of handlers
     */
    <T> Collection<T> getHandlers(Logger logger);
}
