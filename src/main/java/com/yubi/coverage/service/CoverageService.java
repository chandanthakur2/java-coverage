package com.yubi.coverage.service;

import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.html.HTMLFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class CoverageService {

    @Value("${jacoco.address:localhost}")
    private String jacocoAddress;

    @Value("${jacoco.port:8008}")
    private int jacocoPort;

    @Value("${jacoco.report.dir:./coverage-report}")
    private String reportDir;

    @Value("${jacoco.source.dir:./src/main/java}")
    private String sourceDir;

    @Value("${jacoco.package.includes:com.yubi.coverage}")
    private String packageIncludes;

    /**
     * Connects to the JaCoCo agent via TCP and retrieves the current execution data.
     * 
     * @return ExecutionDataStore containing the coverage data
     * @throws IOException if connection fails
     */
    public ExecutionDataStore collectExecutionData() throws IOException {
        ExecutionDataStore executionDataStore = new ExecutionDataStore();
        SessionInfoStore sessionInfoStore = new SessionInfoStore();

        System.out.println("Connecting to JaCoCo agent at " + jacocoAddress + ":" + jacocoPort);
        
        Socket socket = null;
        try {
            // Create socket with timeout
            socket = new Socket();
            socket.setSoTimeout(3000); // 3 second timeout
            socket.connect(new InetSocketAddress(jacocoAddress, jacocoPort), 3000);
            
            System.out.println("Connected to JaCoCo agent successfully");
            
            // Get execution data
            RemoteControlReader reader = new RemoteControlReader(socket.getInputStream());
            reader.setSessionInfoVisitor(sessionInfoStore);
            reader.setExecutionDataVisitor(executionDataStore);
            
            RemoteControlWriter writer = new RemoteControlWriter(socket.getOutputStream());
            writer.visitDumpCommand(true, false);
            
            reader.read();
            System.out.println("Successfully read JaCoCo execution data via TCP");
            
            // Save execution data to file for backup
            saveToFile(executionDataStore, "build/jacoco/runtime.exec");
            
        } catch (Exception e) {
            System.err.println("Error connecting to JaCoCo agent: " + e.getMessage());
            
            // Try to read from file as fallback
            File execFile = new File("build/jacoco/runtime.exec");
            if (execFile.exists()) {
                System.out.println("Falling back to reading execution data from file: " + execFile.getAbsolutePath());
                try (FileInputStream fis = new FileInputStream(execFile)) {
                    ExecutionDataReader reader = new ExecutionDataReader(fis);
                    reader.setSessionInfoVisitor(sessionInfoStore);
                    reader.setExecutionDataVisitor(executionDataStore);
                    reader.read();
                    System.out.println("Successfully read JaCoCo execution data from file");
                } catch (Exception ex) {
                    throw new IOException("Failed to read execution data from both TCP and file: " + ex.getMessage(), ex);
                }
            } else {
                throw new IOException("Cannot connect to JaCoCo agent at " + jacocoAddress + ":" + jacocoPort + 
                    " and no fallback file exists. Make sure the application is running with the JaCoCo agent enabled.", e);
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (Exception e) {
                    // Ignore close errors
                }
            }
        }

        return executionDataStore;
    }
    
    /**
     * Saves execution data to a file for backup purposes.
     */
    private void saveToFile(ExecutionDataStore executionData, String filePath) {
        try {
            File file = new File(filePath);
            File dir = file.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                ExecutionDataWriter writer = new ExecutionDataWriter(fos);
                executionData.accept(writer);
                System.out.println("Saved execution data to file: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to save execution data to file: " + e.getMessage());
            // Don't throw - this is just a backup
        }
    }

    /**
     * Generates an HTML coverage report from the collected execution data.
     * 
     * @return Path to the generated report
     * @throws IOException if report generation fails
     */
    public String generateCoverageReport() throws IOException {
        try {
            ExecutionDataStore executionData = collectExecutionData();
            
            // Ensure report directory exists
            File reportDirectory = new File(reportDir);
            if (!reportDirectory.exists()) {
                System.out.println("Creating report directory: " + reportDirectory.getAbsolutePath());
                if (!reportDirectory.mkdirs()) {
                    System.out.println("Warning: Could not create report directory. Will try to continue anyway.");
                }
            }
            
            HTMLFormatter htmlFormatter = new HTMLFormatter();
            IReportVisitor visitor = htmlFormatter.createVisitor(new FileMultiReportOutput(reportDirectory));
            
            // Create session info store with current data
            SessionInfoStore sessionInfoStore = new SessionInfoStore();
            
            // Initialize the report with session and execution data
            visitor.visitInfo(sessionInfoStore.getInfos(), executionData.getContents());
            
            // Set up the source file locator
            File sourceDirectory = new File(sourceDir);
            System.out.println("Using source directory: " + sourceDirectory.getAbsolutePath());
            DirectorySourceFileLocator sourceFileLocator = new DirectorySourceFileLocator(sourceDirectory, "UTF-8", 4);
            
            // Create coverage builder
            org.jacoco.core.analysis.CoverageBuilder coverageBuilder = new org.jacoco.core.analysis.CoverageBuilder();
            
            // Create analyzer and analyze all class files
            org.jacoco.core.analysis.Analyzer analyzer = new org.jacoco.core.analysis.Analyzer(executionData, coverageBuilder);
            
            // Find all class files in the classpath
            File classesDirectory = new File("./build/classes/java/main");
            if (!classesDirectory.exists()) {
                // Try to find classes in the JAR file
                classesDirectory = new File("./build/libs/coverage-0.0.1-SNAPSHOT.jar");
                System.out.println("Classes directory not found, trying JAR file: " + classesDirectory.getAbsolutePath());
                
                if (!classesDirectory.exists()) {
                    throw new IOException("Neither classes directory nor JAR file found. Make sure the application is compiled.");
                }
            }
            
            System.out.println("Analyzing classes from: " + classesDirectory.getAbsolutePath());
            analyzer.analyzeAll(classesDirectory);
            
            // Create the report
            visitor.visitBundle(coverageBuilder.getBundle(packageIncludes), sourceFileLocator);
            visitor.visitEnd();
            
            System.out.println("Coverage report generated successfully at: " + reportDirectory.getAbsolutePath() + "/index.html");
            return reportDirectory.getAbsolutePath() + "/index.html";
        } catch (IOException e) {
            System.err.println("IOException during report generation: " + e.getMessage());
            throw e; // Rethrow original IOException
        } catch (Exception e) {
            System.err.println("Exception during report generation: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to generate coverage report: " + e.getMessage(), e);
        }
    }

    /**
     * Resets the coverage data by recreating the JaCoCo agent connection.
     * 
     * @throws IOException if reset fails
     */
    public void resetCoverage() throws IOException {
        System.out.println("Forcefully resetting all coverage data");
        
        // Step 1: Delete any existing execution data file
        try {
            File execFile = new File("build/jacoco/runtime.exec");
            if (execFile.exists()) {
                if (execFile.delete()) {
                    System.out.println("Deleted existing execution data file");
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not delete execution data file: " + e.getMessage());
        }
        
        // Step 2: Try to reset via TCP with a more direct approach
        boolean tcpResetSuccessful = false;
        Socket socket = null;
        try {
            // Create socket with timeout
            socket = new Socket();
            socket.setSoTimeout(1000); // 1 second timeout
            socket.connect(new InetSocketAddress(jacocoAddress, jacocoPort), 1000);
            
            // Send reset command
            RemoteControlWriter writer = new RemoteControlWriter(socket.getOutputStream());
            writer.visitDumpCommand(false, true);
            
            // Don't wait for a response
            socket.shutdownOutput();
            socket.close();
            socket = null;
            
            tcpResetSuccessful = true;
            System.out.println("Successfully reset TCP-based coverage");
            
        } catch (Exception e) {
            System.err.println("Warning: Could not reset TCP-based coverage: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (Exception e) {
                    // Ignore close errors
                }
            }
        }
        
        // Step 3: If TCP reset failed, try to create a dummy report which sometimes forces a reset
        if (!tcpResetSuccessful) {
            try {
                // Create an empty execution data store
                ExecutionDataStore emptyStore = new ExecutionDataStore();
                
                // Save it to a file
                saveToFile(emptyStore, "build/jacoco/runtime.exec");
                
                // Create a dummy report directory
                File dummyReportDir = new File("build/reports/jacoco/dummy");
                if (!dummyReportDir.exists()) {
                    dummyReportDir.mkdirs();
                }
                
                // Generate a dummy report
                HTMLFormatter htmlFormatter = new HTMLFormatter();
                IReportVisitor visitor = htmlFormatter.createVisitor(new FileMultiReportOutput(dummyReportDir));
                
                // Initialize with empty data
                visitor.visitInfo(new SessionInfoStore().getInfos(), emptyStore.getContents());
                
                // Create coverage builder
                org.jacoco.core.analysis.CoverageBuilder coverageBuilder = new org.jacoco.core.analysis.CoverageBuilder();
                
                // Visit the bundle and end
                visitor.visitBundle(coverageBuilder.getBundle(packageIncludes), 
                        new DirectorySourceFileLocator(new File(sourceDir), "UTF-8", 4));
                visitor.visitEnd();
                
                System.out.println("Created dummy report to force reset");
            } catch (Exception e) {
                System.err.println("Warning: Could not create dummy report: " + e.getMessage());
            }
        }
        
        System.out.println("Coverage data reset completed");
    }

    /**
     * Saves the raw execution data to a file for later analysis.
     * 
     * @return Path to the saved file
     * @throws IOException if saving fails
     */
    public String saveExecutionData() throws IOException {
        ExecutionDataStore executionData = collectExecutionData();
        
        Path dataDir = Paths.get("./jacoco-data");
        if (!Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
        }
        
        String filename = "jacoco-" + System.currentTimeMillis() + ".exec";
        Path dataFile = dataDir.resolve(filename);
        
        try (FileOutputStream fos = new FileOutputStream(dataFile.toFile())) {
            org.jacoco.core.data.ExecutionDataWriter writer = new org.jacoco.core.data.ExecutionDataWriter(fos);
            executionData.accept(writer);
        }
        
        return dataFile.toString();
    }
    
    /**
     * Checks if the JaCoCo agent is accessible via TCP.
     * 
     * @return true if the agent is accessible, false otherwise
     */
    public boolean isAgentAccessible() {
        System.out.println("Checking if JaCoCo agent is accessible at " + jacocoAddress + ":" + jacocoPort);
        
        Socket socket = null;
        try {
            // Create socket with short timeout
            socket = new Socket();
            socket.setSoTimeout(1000); // 1 second timeout
            socket.connect(new InetSocketAddress(jacocoAddress, jacocoPort), 1000);
            
            System.out.println("JaCoCo agent is accessible");
            return true;
        } catch (Exception e) {
            System.out.println("JaCoCo agent is not accessible: " + e.getMessage());
            return false;
        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (Exception e) {
                    // Ignore close errors
                }
            }
        }
    }
}
