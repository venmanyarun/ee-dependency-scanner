/**
 * Integration example for Liberty Tools for VS Code with EE Dependency Scanner.
 * 
 * Liberty Tools for VS Code provides development tools for Liberty applications.
 * It can use the EE Dependency Scanner to detect Jakarta EE and MicroProfile versions
 * to provide appropriate language support and features.
 * 
 * This TypeScript example shows how to integrate the scanner from a VS Code extension.
 */

import * as vscode from 'vscode';
import * as path from 'path';
import * as child_process from 'child_process';

interface DependencyInfo {
    groupId: string;
    artifactId: string;
    version: string;
    type: string;
    source: string;
}

interface AnalysisResult {
    jakartaEEVersion: string | null;
    javaEEVersion: string | null;
    microProfileVersion: string | null;
    allDependencies: DependencyInfo[];
    primaryParserUsed: string;
}

/**
 * Liberty Tools VS Code integration with EE Dependency Scanner.
 */
export class LibertyToolsVSCodeIntegration {
    
    private scannerJarPath: string;
    private outputChannel: vscode.OutputChannel;
    private cache: Map<string, AnalysisResult> = new Map();
    
    constructor(context: vscode.ExtensionContext) {
        // Path to the scanner JAR bundled with the extension
        this.scannerJarPath = path.join(
            context.extensionPath,
            'jars',
            'ee-dependency-scanner-core.jar'
        );
        
        this.outputChannel = vscode.window.createOutputChannel('Liberty Tools - EE Scanner');
    }
    
    /**
     * Scans a workspace folder for Jakarta EE and MicroProfile dependencies.
     * 
     * @param workspaceFolder The VS Code workspace folder to scan
     * @returns Promise with analysis results
     */
    async scanWorkspace(workspaceFolder: vscode.WorkspaceFolder): Promise<AnalysisResult> {
        const projectPath = workspaceFolder.uri.fsPath;
        
        // Check cache
        if (this.cache.has(projectPath)) {
            this.outputChannel.appendLine(`Using cached results for: ${projectPath}`);
            return this.cache.get(projectPath)!;
        }
        
        this.outputChannel.appendLine(`Scanning workspace: ${projectPath}`);
        
        try {
            const result = await this.runScanner(projectPath);
            this.cache.set(projectPath, result);
            return result;
        } catch (error) {
            this.outputChannel.appendLine(`Error scanning workspace: ${error}`);
            throw error;
        }
    }
    
    /**
     * Runs the Java-based scanner as a child process.
     */
    private async runScanner(projectPath: string): Promise<AnalysisResult> {
        return new Promise((resolve, reject) => {
            // Create a simple Java wrapper that outputs JSON
            const javaCode = `
                import io.openliberty.tools.scanner.analyzer.ClasspathAnalyzer;
                import io.openliberty.tools.scanner.model.ClasspathAnalysisResult;
                import com.google.gson.Gson;
                import java.io.File;
                
                public class ScannerWrapper {
                    public static void main(String[] args) {
                        ClasspathAnalyzer analyzer = new ClasspathAnalyzer();
                        ClasspathAnalysisResult result = analyzer.analyzeProject(new File(args[0]));
                        System.out.println(new Gson().toJson(result));
                    }
                }
            `;
            
            const command = `java -cp "${this.scannerJarPath}" ScannerWrapper "${projectPath}"`;
            
            child_process.exec(command, { maxBuffer: 1024 * 1024 * 10 }, (error, stdout, stderr) => {
                if (error) {
                    reject(new Error(`Scanner execution failed: ${error.message}`));
                    return;
                }
                
                if (stderr) {
                    this.outputChannel.appendLine(`Scanner stderr: ${stderr}`);
                }
                
                try {
                    const result = JSON.parse(stdout) as AnalysisResult;
                    resolve(result);
                } catch (parseError) {
                    reject(new Error(`Failed to parse scanner output: ${parseError}`));
                }
            });
        });
    }
    
    /**
     * Gets the Jakarta EE version for a workspace.
     */
    async getJakartaEEVersion(workspaceFolder: vscode.WorkspaceFolder): Promise<string | null> {
        const result = await this.scanWorkspace(workspaceFolder);
        return result.jakartaEEVersion;
    }
    
    /**
     * Gets the MicroProfile version for a workspace.
     */
    async getMicroProfileVersion(workspaceFolder: vscode.WorkspaceFolder): Promise<string | null> {
        const result = await this.scanWorkspace(workspaceFolder);
        return result.microProfileVersion;
    }
    
    /**
     * Checks if the workspace is a Jakarta EE project.
     */
    async isJakartaEEProject(workspaceFolder: vscode.WorkspaceFolder): Promise<boolean> {
        const version = await this.getJakartaEEVersion(workspaceFolder);
        return version !== null && version.length > 0;
    }
    
    /**
     * Checks if the workspace is a legacy Java EE project.
     */
    async isJavaEEProject(workspaceFolder: vscode.WorkspaceFolder): Promise<boolean> {
        const result = await this.scanWorkspace(workspaceFolder);
        return result.javaEEVersion !== null && result.javaEEVersion.length > 0;
    }
    
    /**
     * Invalidates the cache for a workspace (call when project changes).
     */
    invalidateCache(workspaceFolder: vscode.WorkspaceFolder): void {
        const projectPath = workspaceFolder.uri.fsPath;
        this.cache.delete(projectPath);
        this.outputChannel.appendLine(`Cache invalidated for: ${projectPath}`);
    }
    
    /**
     * Shows a status bar item with detected EE version.
     */
    async showVersionInStatusBar(workspaceFolder: vscode.WorkspaceFolder): Promise<vscode.StatusBarItem> {
        const statusBarItem = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Right, 100);
        
        try {
            const result = await this.scanWorkspace(workspaceFolder);
            
            if (result.jakartaEEVersion) {
                statusBarItem.text = `$(package) Jakarta EE ${result.jakartaEEVersion}`;
                statusBarItem.tooltip = 'Jakarta EE version detected by Liberty Tools';
            } else if (result.javaEEVersion) {
                statusBarItem.text = `$(warning) Java EE ${result.javaEEVersion}`;
                statusBarItem.tooltip = 'Legacy Java EE detected - consider migrating to Jakarta EE';
            } else {
                statusBarItem.text = '$(info) No EE detected';
                statusBarItem.tooltip = 'No Jakarta EE or Java EE dependencies found';
            }
            
            if (result.microProfileVersion) {
                statusBarItem.text += ` | MP ${result.microProfileVersion}`;
            }
            
            statusBarItem.show();
        } catch (error) {
            statusBarItem.text = '$(error) EE scan failed';
            statusBarItem.tooltip = `Failed to scan for EE dependencies: ${error}`;
            statusBarItem.show();
        }
        
        return statusBarItem;
    }
    
    /**
     * Configures language servers based on detected versions.
     */
    async configureLSP4Jakarta(workspaceFolder: vscode.WorkspaceFolder): Promise<void> {
        const result = await this.scanWorkspace(workspaceFolder);
        
        // Update VS Code settings for LSP4Jakarta
        const config = vscode.workspace.getConfiguration('jakarta', workspaceFolder.uri);
        
        if (result.jakartaEEVersion) {
            await config.update('version', result.jakartaEEVersion, vscode.ConfigurationTarget.WorkspaceFolder);
            await config.update('enabled', true, vscode.ConfigurationTarget.WorkspaceFolder);
            
            this.outputChannel.appendLine(`Configured LSP4Jakarta for Jakarta EE ${result.jakartaEEVersion}`);
        } else if (result.javaEEVersion) {
            await config.update('enabled', false, vscode.ConfigurationTarget.WorkspaceFolder);
            await config.update('showMigrationSuggestions', true, vscode.ConfigurationTarget.WorkspaceFolder);
            
            this.outputChannel.appendLine('Disabled LSP4Jakarta (Java EE project detected)');
        }
    }
    
    /**
     * Configures language servers based on detected versions.
     */
    async configureLSP4MP(workspaceFolder: vscode.WorkspaceFolder): Promise<void> {
        const result = await this.scanWorkspace(workspaceFolder);
        
        // Update VS Code settings for LSP4MP (MicroProfile)
        const config = vscode.workspace.getConfiguration('microprofile', workspaceFolder.uri);
        
        if (result.microProfileVersion) {
            await config.update('version', result.microProfileVersion, vscode.ConfigurationTarget.WorkspaceFolder);
            await config.update('enabled', true, vscode.ConfigurationTarget.WorkspaceFolder);
            
            this.outputChannel.appendLine(`Configured LSP4MP for MicroProfile ${result.microProfileVersion}`);
        }
    }
    
    /**
     * Registers file watchers to invalidate cache on project changes.
     */
    registerFileWatchers(context: vscode.ExtensionContext): void {
        // Watch for pom.xml changes
        const pomWatcher = vscode.workspace.createFileSystemWatcher('**/pom.xml');
        pomWatcher.onDidChange(uri => {
            const workspaceFolder = vscode.workspace.getWorkspaceFolder(uri);
            if (workspaceFolder) {
                this.invalidateCache(workspaceFolder);
            }
        });
        context.subscriptions.push(pomWatcher);
        
        // Watch for build.gradle changes
        const gradleWatcher = vscode.workspace.createFileSystemWatcher('**/build.gradle');
        gradleWatcher.onDidChange(uri => {
            const workspaceFolder = vscode.workspace.getWorkspaceFolder(uri);
            if (workspaceFolder) {
                this.invalidateCache(workspaceFolder);
            }
        });
        context.subscriptions.push(gradleWatcher);
    }
    
    dispose(): void {
        this.outputChannel.dispose();
        this.cache.clear();
    }
}

/**
 * Example activation function for Liberty Tools VS Code extension.
 */
export function activate(context: vscode.ExtensionContext) {
    const integration = new LibertyToolsVSCodeIntegration(context);
    
    // Register file watchers
    integration.registerFileWatchers(context);
    
    // Scan all workspace folders on activation
    if (vscode.workspace.workspaceFolders) {
        for (const folder of vscode.workspace.workspaceFolders) {
            integration.scanWorkspace(folder).then(result => {
                console.log(`Scanned ${folder.name}:`, result);
                
                // Show version in status bar
                integration.showVersionInStatusBar(folder);
                
                // Configure language servers
                integration.configureLSP4Jakarta(folder);
                integration.configureLSP4MP(folder);
            });
        }
    }
    
    // Register commands
    context.subscriptions.push(
        vscode.commands.registerCommand('liberty.scanDependencies', async () => {
            const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
            if (workspaceFolder) {
                const result = await integration.scanWorkspace(workspaceFolder);
                vscode.window.showInformationMessage(
                    `Jakarta EE: ${result.jakartaEEVersion || 'Not detected'}, ` +
                    `MicroProfile: ${result.microProfileVersion || 'Not detected'}`
                );
            }
        })
    );
    
    context.subscriptions.push(integration);
}


