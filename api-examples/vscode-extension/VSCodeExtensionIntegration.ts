/**
 * VSCode Extension Integration Example
 * 
 * This example shows how to integrate the EE Dependency Scanner API
 * in a VSCode extension using the Language Server Protocol (LSP).
 * 
 * Architecture:
 * 1. VSCode Extension (TypeScript) - Client side
 * 2. Java Language Server - Server side with access to IJavaProject
 * 3. Communication via LSP commands
 */

import * as vscode from 'vscode';
import { LanguageClient } from 'vscode-languageclient/node';

/**
 * Command to check EE/MP dependencies for a project
 * This is called from the VSCode extension
 */
export async function checkProjectDependencies(
    client: LanguageClient,
    projectUri: string
): Promise<DependencyAnalysisResult> {
    try {
        // Send request to Java language server
        // The server has access to IJavaProject and can use the scanner API
        const result = await client.sendRequest<DependencyAnalysisResult>(
            'ee-scanner/analyzeDependencies',
            { projectUri }
        );
        
        return result;
    } catch (error) {
        console.error('Failed to analyze dependencies:', error);
        throw error;
    }
}

/**
 * Command to check if project uses MicroProfile
 */
export async function hasMicroProfileDependency(
    client: LanguageClient,
    projectUri: string
): Promise<boolean> {
    try {
        const result = await client.sendRequest<boolean>(
            'ee-scanner/hasMicroProfile',
            { projectUri }
        );
        
        return result;
    } catch (error) {
        console.error('Failed to check MicroProfile dependency:', error);
        return false;
    }
}

/**
 * Command to get specific dependency version
 */
export async function getDependencyVersion(
    client: LanguageClient,
    projectUri: string,
    groupId: string,
    artifactId: string
): Promise<string | null> {
    try {
        const result = await client.sendRequest<string | null>(
            'ee-scanner/getDependencyVersion',
            { projectUri, groupId, artifactId }
        );
        
        return result;
    } catch (error) {
        console.error('Failed to get dependency version:', error);
        return null;
    }
}

/**
 * Register commands in VSCode extension
 */
export function registerDependencyCommands(
    context: vscode.ExtensionContext,
    client: LanguageClient
): void {
    // Command: Show project dependencies
    context.subscriptions.push(
        vscode.commands.registerCommand('ee-scanner.showDependencies', async () => {
            const editor = vscode.window.activeTextEditor;
            if (!editor) {
                vscode.window.showErrorMessage('No active editor');
                return;
            }

            const projectUri = vscode.workspace.getWorkspaceFolder(editor.document.uri)?.uri.toString();
            if (!projectUri) {
                vscode.window.showErrorMessage('No workspace folder found');
                return;
            }

            try {
                const result = await checkProjectDependencies(client, projectUri);
                
                // Display results
                const message = `
Jakarta EE: ${result.jakartaEEVersion || 'Not detected'}
Java EE: ${result.javaEEVersion || 'Not detected'}
MicroProfile: ${result.microProfileVersion || 'Not detected'}
Total Dependencies: ${result.dependencies.length}
                `.trim();
                
                vscode.window.showInformationMessage(message);
            } catch (error) {
                vscode.window.showErrorMessage(`Failed to analyze dependencies: ${error}`);
            }
        })
    );

    // Command: Check MicroProfile
    context.subscriptions.push(
        vscode.commands.registerCommand('ee-scanner.checkMicroProfile', async () => {
            const editor = vscode.window.activeTextEditor;
            if (!editor) return;

            const projectUri = vscode.workspace.getWorkspaceFolder(editor.document.uri)?.uri.toString();
            if (!projectUri) return;

            const hasMp = await hasMicroProfileDependency(client, projectUri);
            const message = hasMp 
                ? 'This project uses MicroProfile' 
                : 'This project does not use MicroProfile';
            
            vscode.window.showInformationMessage(message);
        })
    );
}

/**
 * Data structures matching the Java API
 */
interface DependencyAnalysisResult {
    dependencies: DependencyInfo[];
    jakartaEEVersion: string | null;
    javaEEVersion: string | null;
    microProfileVersion: string | null;
}

interface DependencyInfo {
    groupId: string;
    artifactId: string;
    version: string;
    type: 'JAKARTA_EE' | 'JAVA_EE' | 'MICROPROFILE' | 'OTHER';
    source: 'MAVEN' | 'GRADLE' | 'JAR_SCAN' | 'ECLIPSE_CLASSPATH' | 'INTELLIJ_MODULE';
}

/**
 * Example usage in extension activation
 */
export async function activate(context: vscode.ExtensionContext) {
    // ... Language client setup ...
    const client: LanguageClient = createLanguageClient();
    
    await client.start();
    
    // Register dependency scanner commands
    registerDependencyCommands(context, client);
    
    console.log('EE Dependency Scanner commands registered');
}

// Placeholder for language client creation
function createLanguageClient(): LanguageClient {
    // Implementation would follow the pattern from liberty-tools-vscode
    throw new Error('Not implemented - see liberty-tools-vscode for full example');
}


