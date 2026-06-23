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
package com.tencent.dlc.cli.command;

import com.tencent.dlc.core.TencentDLCConstants;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.util.concurrent.Callable;

/**
 * Prints CLI and JDBC driver version information.
 */
@CommandLine.Command(
    name = "version",
    description = "Show version information"
)
public class VersionCommand implements Callable<Integer> {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        String cliVersion = getClass().getPackage().getImplementationVersion();
        if (cliVersion == null) {
            cliVersion = "1.0.0-SNAPSHOT";
        }
        out.println("tdlc-cli " + cliVersion);

        // The bundled driver version is known from the POM dependency.
        String driverVersion = TencentDLCConstants.class.getPackage().getImplementationVersion();
        if (driverVersion == null) {
            driverVersion = "2.5.9";
        }
        out.println("dlc-jdbc-driver " + driverVersion);
        return 0;
    }
}
