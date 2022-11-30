/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jkiss.code.NotNull;

public class LogOutputStream extends OutputStream {

    /**
     * The File object to store messages.  This value may be null.
     */
    private final File currentLogFile;
    
    private final File logFileLocation;

    /**
     * The Writer to log messages to.
     */
    private volatile FileOutputStream currentLogFileOutput = null;
    private volatile long currentLogSize;

    private final int maxLogSize;
    private final int maxLogFiles;
    
    private final String logFileName;
    private final String logFileNameExtension;
    private final Predicate<String> logFileNamePattern;
    
    public LogOutputStream(@NotNull File debugLogFile, int maxLogSize, int maxLogFiles) throws IOException {
        if (debugLogFile.exists() && !debugLogFile.isFile()) {
            throw new IOException(
                "Failed to initialize debug log output due to the target is not a file: " + debugLogFile.getAbsolutePath()
            );
        }
        
        this.currentLogFile = debugLogFile;
        this.logFileLocation = debugLogFile.getParentFile();
        this.maxLogSize = maxLogSize;
        this.maxLogFiles = maxLogFiles;

        String fileName = debugLogFile.getName();
        int fnameExtStart = fileName.lastIndexOf('.');
        if (fnameExtStart >= 0) {
            logFileName = fileName.substring(0, fnameExtStart);
            logFileNameExtension = fileName.substring(fnameExtStart);
        } else {
            logFileName = fileName;
            logFileNameExtension = "";
        }

        String logFileNameRegexStr = "^" + Pattern.quote(logFileName) + "\\-[0-9]+" + Pattern.quote(logFileNameExtension) + "$";
        logFileNamePattern = Pattern.compile(logFileNameRegexStr).asMatchPredicate();
        
        if (debugLogFile.exists()) {
            currentLogSize = currentLogFile.length();
            this.rotateCurrentLogFile(true);
        } else {
            currentLogSize = 0;
            if (this.logFileLocation.mkdirs()) {
                throw new IOException("Failed to initialize debug log output location: " + debugLogFile.getAbsolutePath());
            }
        }
    }
    
    @Override
    public synchronized void write(int b) throws IOException {
        this.getLogFileWriter().write(b);
        currentLogSize++;
    }
    
    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        this.getLogFileWriter().write(b, off, len);
        currentLogSize += len;
    }
    
    @Override
    public synchronized void flush() throws IOException {
        if (currentLogFileOutput != null) {
            currentLogFileOutput.flush();
        }
    }
    
    @Override
    public synchronized void close() throws IOException {
        if (currentLogFileOutput != null) {
            currentLogFileOutput.close();
            currentLogFileOutput = null;
        }
    }

    private OutputStream getLogFileWriter() throws IOException {
        if (currentLogFileOutput == null || this.rotateCurrentLogFile(false)) {
            currentLogFileOutput = new FileOutputStream(currentLogFile, true);
        }
        return currentLogFileOutput;
    }

    /**
     * Checks the log file size. If the log file size reaches the limit then the log is rotated
     * @return false if the file doen't exist or the log files doesn't need to be rotated
     * @throws IOException 
     */
    private boolean rotateCurrentLogFile(boolean force) throws IOException {
        if (currentLogFile.exists() && (currentLogSize > maxLogSize || force)) {
            this.close();
            
            File newFile = new File(logFileLocation, logFileName + "-" + System.currentTimeMillis() + logFileNameExtension);
            currentLogFile.renameTo(newFile);
            currentLogSize = 0;
            
            File[] logFiles = logFileLocation.listFiles((File dir, String name) -> logFileNamePattern.test(name));
            Arrays.sort(logFiles, (a, b) -> a.getName().compareTo(b.getName()));
            for (int i = 0, count = logFiles.length; i < logFiles.length && count > maxLogFiles; i++, count--) {
                logFiles[i].delete();
            }
            
            return true;
        } else {
            return false;
        }
    }
    
}
