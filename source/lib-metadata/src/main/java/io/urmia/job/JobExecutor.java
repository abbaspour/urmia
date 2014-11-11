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

import io.urmia.md.model.job.JobInput;

import java.io.IOException;

public interface JobExecutor extends Runnable {

    public static enum Status {
        INIT,
        SUCCESSFUL,
        FAILED
    }

    void addInput(JobInput input) throws IOException;
    void terminateInput() throws IOException;

    String getOutputPath();

    int exitCode() throws InterruptedException;
}
