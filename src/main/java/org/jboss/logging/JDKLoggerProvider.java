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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogManager;

final class JDKLoggerProvider extends AbstractMdcLoggerProvider implements LoggerProvider {

    public Logger getLogger(final String name) {
        return new JDKLogger(name);
    }

    @Override
    public Set<String> getLoggerNames() {
        return new LinkedHashSet<>(Collections.list(LogManager.getLogManager().getLoggerNames()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Handler> getHandlers(final String name) {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(name);
        final Handler[] handlers = logger.getHandlers();
        return handlers == null ? Collections.emptySet() : new LinkedHashSet<>(Arrays.asList(handlers));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Handler> getHandlers(final Logger logger) {
        // TODO (jrp) this can be done better
        return getHandlers(logger.getName());
    }
}
