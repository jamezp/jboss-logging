/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2011 Red Hat, Inc.
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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

final class LoggerProviders {
    static final String LOGGING_PROVIDER_KEY = "org.jboss.logging.provider";

    static final LoggerProvider PROVIDER = getFinder().find();

    private static LoggerProviderFinder getFinder() {
        final ClassLoader cl = getTccl();

        // Next try for a service provider
        try {
            final ServiceLoader<LoggerProviderFinder> loader = ServiceLoader.load(LoggerProviderFinder.class, cl);
            final Iterator<LoggerProviderFinder> iter = loader.iterator();
            for (; ; )
                try {
                    if (!iter.hasNext()) break;
                    return iter.next();
                } catch (ServiceConfigurationError ignore) {
                }
        } catch (Throwable ignore) {
            // TODO consider printing the stack trace as it should only happen once
        }
        return DefaultLoggerProviderFinder.getInstance();
    }

    private LoggerProviders() {
    }

    // TODO (jrp) might not need the TCCL
    private static ClassLoader getTccl() {
        if (System.getSecurityManager() == null) {
            return Thread.currentThread().getContextClassLoader();
        }
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }
}
