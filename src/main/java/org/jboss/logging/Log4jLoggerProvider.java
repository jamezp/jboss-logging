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

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Appender;
import org.apache.log4j.LogManager;
import org.apache.log4j.MDC;
import org.apache.log4j.NDC;

final class Log4jLoggerProvider implements LoggerProvider {

    public Logger getLogger(final String name) {
        return new Log4jLogger("".equals(name) ? "ROOT" : name);
    }

    public void clearMdc() {
        MDC.clear();
    }

    public Object getMdc(String key) {
        return MDC.get(key);
    }

    public Map<String, Object> getMdcMap() {
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = MDC.getContext();
        return map == null ? Collections.<String, Object>emptyMap() : map;
    }

    public Object putMdc(String key, Object val) {
        try {
            return MDC.get(key);
        } finally {
            MDC.put(key, val);
        }
    }

    public void removeMdc(String key) {
        MDC.remove(key);
    }

    public void clearNdc() {
        NDC.remove();
    }

    public String getNdc() {
        return NDC.get();
    }

    public int getNdcDepth() {
        return NDC.getDepth();
    }

    public String peekNdc() {
        return NDC.peek();
    }

    public String popNdc() {
        return NDC.pop();
    }

    public void pushNdc(String message) {
        NDC.push(message);
    }

    public void setNdcMaxDepth(int maxDepth) {
        NDC.setMaxDepth(maxDepth);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getLoggerNames() {
        return Collections.list((Enumeration<org.apache.log4j.Logger>) LogManager.getCurrentLoggers())
                .stream()
                .map(org.apache.log4j.Logger::getName)
                .collect(Collectors.toSet());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Appender> getHandlers(final String name) {
        return Collections.list((Enumeration<Appender>) org.apache.log4j.Logger.getLogger(name).getAllAppenders());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Appender> getHandlers(final Logger logger) {
        if (logger instanceof Log4jLogger) {
            return Collections.list((Enumeration<Appender>) ((Log4jLogger) logger).getDelegate().getAllAppenders());
        }
        // This should never happen
        throw new IllegalArgumentException(String.format("Logger %s is not a log4j Logger.", logger));
    }
}
