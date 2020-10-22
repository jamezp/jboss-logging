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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.MDC;
import org.jboss.logmanager.NDC;

import static org.jboss.logmanager.Logger.AttachmentKey;

final class JBossLogManagerProvider implements LoggerProvider {

    private static final AttachmentKey<Logger> KEY = new AttachmentKey<>();
    private static final AttachmentKey<ConcurrentMap<String, Logger>> LEGACY_KEY = new AttachmentKey<>();

    public Logger getLogger(final String name) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged((PrivilegedAction<Logger>) () -> {
                try {
                    return doGetLogger(name) ;
                } catch (NoSuchMethodError ignore) {
                }
                // fallback
                return doLegacyGetLogger(name);
            });
        } else {
            try {
                return doGetLogger(name) ;
            } catch (NoSuchMethodError ignore) {
            }
            // fallback
            return doLegacyGetLogger(name);
        }
    }

    @Override
    public Set<Logger> getLoggers() {
        return Collections.list(LogManager.getLogManager().getLoggerNames())
                .stream()
                .map(Logger::getLogger)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getLoggerNames() {
        return new LinkedHashSet<>(Collections.list(LogManager.getLogManager().getLoggerNames()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Handler> getHandlers(final String name) {
        final LogContext logContext = LogContext.getLogContext();
        final org.jboss.logmanager.Logger logger = logContext.getLoggerIfExists(name);
        if (logger == null) {
            return Collections.emptySet();
        }
        final Handler[] handlers = logger.getHandlers();
        return handlers == null ? Collections.emptySet() : new LinkedHashSet<>(Arrays.asList(handlers));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Handler> getHandlers(final Logger logger) {
        // TODO (jrp) this can be done better
        return getHandlers(logger.getName());
    }

    private static Logger doLegacyGetLogger(final String name) {
        final org.jboss.logmanager.Logger lmLogger = LogContext.getLogContext().getLogger("");
        ConcurrentMap<String, Logger> loggers = lmLogger.getAttachment(LEGACY_KEY);
        if (loggers == null) {
            loggers = new ConcurrentHashMap<>();
            final ConcurrentMap<String, Logger> appearing = lmLogger.attachIfAbsent(LEGACY_KEY, loggers);
            if (appearing != null) {
                loggers = appearing;
            }
        }

        Logger l = loggers.get(name);
        if (l != null) {
            return l;
        }

        final org.jboss.logmanager.Logger logger = org.jboss.logmanager.Logger.getLogger(name);
        l = new JBossLogManagerLogger(name, logger);
        final Logger appearing = loggers.putIfAbsent(name, l);
        if (appearing == null) {
            return l;
        }
        return appearing;
    }

    private static Logger doGetLogger(final String name) {
        Logger l = LogContext.getLogContext().getAttachment(name, KEY);
        if (l != null) {
            return l;
        }
        final org.jboss.logmanager.Logger logger = org.jboss.logmanager.Logger.getLogger(name);
        l = new JBossLogManagerLogger(name, logger);
        Logger a = logger.attachIfAbsent(KEY, l);
        if (a == null) {
            return l;
        } else {
            return a;
        }
    }

    public void clearMdc() {
        MDC.clear();
    }

    public Object putMdc(final String key, final Object value) {
        return MDC.put(key, String.valueOf(value));
    }

    public Object getMdc(final String key) {
        return MDC.get(key);
    }

    public void removeMdc(final String key) {
        MDC.remove(key);
    }

    @SuppressWarnings({ "unchecked" })
    public Map<String, Object> getMdcMap() {
        // we can re-define the erasure of this map because MDC does not make further use of the copy
        return (Map)MDC.copy();
    }

    public void clearNdc() {
        NDC.clear();
    }

    public String getNdc() {
        return NDC.get();
    }

    public int getNdcDepth() {
        return NDC.getDepth();
    }

    public String popNdc() {
        return NDC.pop();
    }

    public String peekNdc() {
        return NDC.get();
    }

    public void pushNdc(final String message) {
        NDC.push(message);
    }

    public void setNdcMaxDepth(final int maxDepth) {
        NDC.trimTo(maxDepth);
    }
}
