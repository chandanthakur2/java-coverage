package com.yubi.coverage.service;

import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
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

    // File-based JaCoCo configuration
    @Value("${jacoco.exec.file:./build/jacoco/runtime.exec}")
    private String jacocoExecFile;

    @Value("${jacoco.report.dir:./coverage-report}")
    private String reportDir;

    @Value("${jacoco.source.dir:./src/main/java}")
    private String sourceDir;

    @Value("${jacoco.package.includes:com.yubi.coverage}")
    private String packageIncludes;

    /**
     * Retrieves the current execution data from the JaCoCo TCP server.
     * 
     * @return ExecutionDataStore containing the coverage data
     * @throws IOException if connection fails
     */
    public ExecutionDataStore collectExecutionData() throws IOException {
        ExecutionDataStore executionDataStore = new ExecutionDataStore();
        SessionInfoStore sessionInfoStore = new SessionInfoStore();

        try (Socket socket = new Socket(InetAddress.getByName("localhost"), 6300)) {
            org.jacoco.core.data.ExecutionDataReader reader = new ExecutionDataReader(socket.getInputStream());
            reader.setExecutionDataVisitor(executionDataStore);
            reader.setSessionInfoVisitor(sessionInfoStore);
            reader.read();
        } catch (Exception e) {
            throw new IOException("Error connecting to JaCoCo agent: " + e.getMessage(), e);
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
                throw new IOException("Classes directory not found at: " + classesDirectory.getAbsolutePath() + ". Make sure the application is compiled.");
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
     * Resets the coverage data by sending a reset command to the JaCoCo TCP server.
     * 
     * @throws IOException if reset fails
     */
    public void resetCoverage() throws IOException {
        try (Socket socket = new Socket(InetAddress.getByName("localhost"), 6300)) {
            org.jacoco.core.runtime.RemoteControlReader reader = new org.jacoco.core.runtime.RemoteControlReader(socket.getInputStream());
            org.jacoco.core.runtime.RemoteControlWriter writer = new org.jacoco.core.runtime.RemoteControlWriter(socket.getOutputStream());
            
            // Send reset command
            writer.visitDumpCommand(true, false);
            reader.read();
        } catch (Exception e) {
            throw new IOException("Failed to reset coverage data: " + e.getMessage(), e);
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
}
