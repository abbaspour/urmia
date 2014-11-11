package io.urmia.job;

/**
 *
 * Copyright 2014 by Amin Abbaspour
 *
 * This file is part of Urmia.io
 *
 * Urmia.io is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Urmia.io is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Urmia.io.  If not, see <http://www.gnu.org/licenses/>.
 */

import io.urmia.md.model.job.JobDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

public abstract class JobExecutorFactory {

    private static final Logger log = LoggerFactory.getLogger(JobExecutorFactory.class);

    public abstract JobExecutor newInstance(String hostId, JobDefinition jobDef, String jobId, String mountPoint,
                                            JobDefinition.Phase.Type type);

    public static JobExecutorFactory lookup() {
        ServiceLoader<JobExecutorFactory> loader = ServiceLoader.load(JobExecutorFactory.class);

        for (JobExecutorFactory sf : loader) {
            if (sf instanceof XargsJobExecutor.Factory)
                continue;
            log.info("using JobExecutorFactory: {}", sf.getClass());
            return sf;
        }

        log.warn("no JobExecutor.Factory SPI found. returning default XargsJobExecutor.Factory");
        return new XargsJobExecutor.Factory();
    }

}
