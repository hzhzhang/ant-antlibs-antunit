/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.apache.ant.antunit;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.TeeOutputStream;

/**
 * A test listener for &lt;antunit&gt; modeled aftern the Plain JUnit
 * test listener that is part of Ant.
 */
public abstract class BaseAntUnitListener extends ProjectComponent
    implements AntUnitListener {

    protected BaseAntUnitListener(SendLogTo defaultReportTarget) {
        logTo = defaultReportTarget;
    }

    /**
     * Formatter for timings.
     */
    protected static final NumberFormat nf = NumberFormat.getInstance();

    /**
     * Directory to write reports to.
     */
    private File toDir;

    /**
     * Directory to write reports to.
     */
    protected final File getToDir() {
        return toDir;
    }

    /**
     * Sets the directory to write test reports to.
     */
    public void setToDir(File f) {
        toDir = f;
    }

    /**
     * Where to send log.
     */
    private SendLogTo logTo;

    /**
     * keeps track of the numer of executed targets, the failures an errors.
     */
    protected int runCount, failureCount, errorCount;
    /**
     * time for the starts of the current test-suite and test-target.
     */
    protected long start, testStart;

    /**
     * Where to send the test report.
     */
    protected void setSendLogTo(SendLogTo logTo) {
        this.logTo = logTo;
    }

    public void startTestSuite(Project testProject, String buildFile) {
        start = System.currentTimeMillis();
        runCount = failureCount = errorCount = 0;
    }

    protected final void close(OutputStream out) {
        if (out != System.out && out != System.err) {
            FileUtils.close(out);
        }
    }

    public void startTest(String target) {
        testStart = System.currentTimeMillis();
        runCount++;
    }
    public void addFailure(String target, AssertionFailedException ae) {
        failureCount++;
    }
    public void addError(String target, Throwable ae) {
        errorCount++;
    }

    protected final OutputStream getOut(String buildFile) {
        OutputStream l, f;
        l = f = null;
        if (logTo.getValue().equals(SendLogTo.ANT_LOG)
            || logTo.getValue().equals(SendLogTo.BOTH)) {
            l = new LogOutputStream(this, Project.MSG_INFO);
            if (logTo.getValue().equals(SendLogTo.ANT_LOG)) {
                return l;
            }
        }
        if (logTo.getValue().equals(SendLogTo.FILE)
            || logTo.getValue().equals(SendLogTo.BOTH)) {

            buildFile = FileUtils.getFileUtils()
                .removeLeadingPath(getProject().getBaseDir(),
                                   new File(buildFile));
            if (buildFile.length() > 0
                && buildFile.charAt(0) == File.separatorChar) {
                buildFile = buildFile.substring(1);
            }
            
            String fileName = "TEST-" +
                buildFile.replace(File.separatorChar, '.').replace(':', '.')
                + ".txt";
            File file = toDir == null
                ? getProject().resolveFile(fileName)
                : new File(toDir, fileName);
            try {
                f = new FileOutputStream(file);
            } catch (IOException e) {
                throw new BuildException(e);
            }
            if (logTo.getValue().equals(SendLogTo.FILE)) {
                return f;
            }
        }
        return new TeeOutputStream(l, f);
    }

    public static class SendLogTo extends EnumeratedAttribute {
        public static final String ANT_LOG = "ant";
        public static final String FILE = "file";
        public static final String BOTH = "both";

        public SendLogTo() {}

        public SendLogTo(String s) {
            setValue(s);
        }

        public String[] getValues() {
            return new String[] {ANT_LOG, FILE, BOTH};
        }
    }
}