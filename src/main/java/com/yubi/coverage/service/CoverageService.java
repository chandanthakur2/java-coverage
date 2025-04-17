package com.yubi.coverage.service;

import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
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
     * Connects to the JaCoCo agent and retrieves the current execution data.
     * 
     * @return ExecutionDataStore containing the coverage data
     * @throws IOException if connection fails
     */
    public ExecutionDataStore collectExecutionData() throws IOException {
        ExecutionDataStore executionDataStore = new ExecutionDataStore();
        SessionInfoStore sessionInfoStore = new SessionInfoStore();

        try (Socket socket = new Socket(InetAddress.getByName(jacocoAddress), jacocoPort)) {
            RemoteControlReader reader = new RemoteControlReader(socket.getInputStream());
            reader.setSessionInfoVisitor(sessionInfoStore);
            reader.setExecutionDataVisitor(executionDataStore);
            
            RemoteControlWriter writer = new RemoteControlWriter(socket.getOutputStream());
            writer.visitDumpCommand(true, false);
            
            reader.read();
        } catch (java.net.ConnectException e) {
            throw new IOException("Cannot connect to JaCoCo agent at " + jacocoAddress + ":" + jacocoPort + 
                ". Make sure the application is running with the JaCoCo agent enabled on TCP port " + jacocoPort, e);
        } catch (Exception e) {
            throw new IOException("Error collecting coverage data: " + e.getMessage(), e);
        }

        return executionDataStore;
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
            
            File reportDirectory = new File(reportDir);
            if (!reportDirectory.exists()) {
                reportDirectory.mkdirs();
            }
            
            HTMLFormatter htmlFormatter = new HTMLFormatter();
            IReportVisitor visitor = htmlFormatter.createVisitor(new FileMultiReportOutput(reportDirectory));
            
            // Create session info store with current data
            SessionInfoStore sessionInfoStore = new SessionInfoStore();
            
            // Initialize the report with session and execution data
            visitor.visitInfo(sessionInfoStore.getInfos(), executionData.getContents());
            
            // Set up the source file locator
            File sourceDirectory = new File(sourceDir);
            DirectorySourceFileLocator sourceFileLocator = new DirectorySourceFileLocator(sourceDirectory, "UTF-8", 4);
            
            // Create coverage builder
            org.jacoco.core.analysis.CoverageBuilder coverageBuilder = new org.jacoco.core.analysis.CoverageBuilder();
            
            // Create analyzer and analyze all class files
            org.jacoco.core.analysis.Analyzer analyzer = new org.jacoco.core.analysis.Analyzer(executionData, coverageBuilder);
            
            // Find all class files in the classpath
            File classesDirectory = new File("./build/classes/java/main");
            if (!classesDirectory.exists()) {
                throw new IOException("Classes directory not found at: " + classesDirectory.getAbsolutePath() + 
                    ". Make sure the application is compiled.");
            }
            analyzer.analyzeAll(classesDirectory);
            
            // Create the report
            visitor.visitBundle(coverageBuilder.getBundle(packageIncludes), sourceFileLocator);
            visitor.visitEnd();
            
            return reportDirectory.getAbsolutePath() + "/index.html";
        } catch (IOException e) {
            throw e; // Rethrow original IOException
        } catch (Exception e) {
            throw new IOException("Failed to generate coverage report: " + e.getMessage(), e);
        }
    }

    /**
     * Resets the coverage data in the JaCoCo agent.
     * 
     * @throws IOException if connection fails
     */
    public void resetCoverage() throws IOException {
        try (Socket socket = new Socket(InetAddress.getByName(jacocoAddress), jacocoPort)) {
            RemoteControlWriter writer = new RemoteControlWriter(socket.getOutputStream());
            writer.visitDumpCommand(false, true);
            
            new ExecutionDataReader(socket.getInputStream()).read();
        }
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
     * Checks if the JaCoCo agent is accessible.
     * 
     * @return true if agent is accessible, false otherwise
     */
    public boolean isAgentAccessible() {
        try (Socket socket = new Socket(InetAddress.getByName(jacocoAddress), jacocoPort)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
