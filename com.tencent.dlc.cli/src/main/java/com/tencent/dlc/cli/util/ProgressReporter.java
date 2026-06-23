/*
 * Tencent DLC CLI
 * Copyright (C) 2026 Bing Tong and contributors
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
package com.tencent.dlc.cli.util;

import java.io.PrintStream;

/**
 * Reports progress to the user. Implementations may write to stderr or do nothing
 * when quiet mode is enabled.
 */
public interface ProgressReporter {

    ProgressReporter NO_OP = new ProgressReporter() {
        @Override
        public void report(String message) {
            // no-op
        }

        @Override
        public void done() {
            // no-op
        }
    };

    /**
     * Reports an intermediate progress message.
     */
    void report(String message);

    /**
     * Reports completion.
     */
    void done();

    /**
     * Default console reporter that writes to stderr.
     */
    static ProgressReporter console(PrintStream stream) {
        return new ProgressReporter() {
            @Override
            public void report(String message) {
                stream.println(message);
            }

            @Override
            public void done() {
                // nothing extra to print
            }
        };
    }
}
