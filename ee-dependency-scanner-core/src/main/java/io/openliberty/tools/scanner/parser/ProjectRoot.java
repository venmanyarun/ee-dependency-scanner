package io.openliberty.tools.scanner.parser;

import java.io.File;

/**
 * Abstraction for project root that can represent different IDE project models.
 * This allows parsers to work with File-based paths, IntelliJ Modules, Eclipse IJavaProjects, etc.
 * 
 * @param <T> the underlying project representation type
 */
public class ProjectRoot<T> {
    
    private final T projectModel;
    private final File projectDirectory;
    private final ProjectRootType type;
    
    /**
     * Creates a file-based project root.
     */
    public static ProjectRoot<File> fromFile(File projectDirectory) {
        return new ProjectRoot<>(projectDirectory, projectDirectory, ProjectRootType.FILE);
    }
    
    /**
     * Creates an IntelliJ Module-based project root.
     * 
     * @param module the IntelliJ Module instance
     * @param moduleDirectory the module's root directory
     */
    public static <M> ProjectRoot<M> fromIntelliJModule(M module, File moduleDirectory) {
        return new ProjectRoot<>(module, moduleDirectory, ProjectRootType.INTELLIJ_MODULE);
    }
    
    /**
     * Creates an Eclipse IJavaProject-based project root.
     * 
     * @param javaProject the Eclipse IJavaProject instance
     * @param projectDirectory the project's root directory
     */
    public static <J> ProjectRoot<J> fromEclipseProject(J javaProject, File projectDirectory) {
        return new ProjectRoot<>(javaProject, projectDirectory, ProjectRootType.ECLIPSE_JAVA_PROJECT);
    }
    
    private ProjectRoot(T projectModel, File projectDirectory, ProjectRootType type) {
        this.projectModel = projectModel;
        this.projectDirectory = projectDirectory;
        this.type = type;
    }
    
    /**
     * Gets the underlying project model (File, Module, IJavaProject, etc.).
     */
    public T getProjectModel() {
        return projectModel;
    }
    
    /**
     * Gets the project directory as a File.
     */
    public File getProjectDirectory() {
        return projectDirectory;
    }
    
    /**
     * Gets the type of project root.
     */
    public ProjectRootType getType() {
        return type;
    }
    
    /**
     * Checks if this is a file-based project root.
     */
    public boolean isFile() {
        return type == ProjectRootType.FILE;
    }
    
    /**
     * Checks if this is an IntelliJ Module-based project root.
     */
    public boolean isIntelliJModule() {
        return type == ProjectRootType.INTELLIJ_MODULE;
    }
    
    /**
     * Checks if this is an Eclipse IJavaProject-based project root.
     */
    public boolean isEclipseProject() {
        return type == ProjectRootType.ECLIPSE_JAVA_PROJECT;
    }
    
    /**
     * Type of project root representation.
     */
    public enum ProjectRootType {
        /** File-based project root (directory or build file) */
        FILE,
        
        /** IntelliJ IDEA Module */
        INTELLIJ_MODULE,
        
        /** Eclipse IJavaProject */
        ECLIPSE_JAVA_PROJECT
    }
}


