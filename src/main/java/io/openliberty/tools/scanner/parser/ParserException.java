package io.openliberty.tools.scanner.parser;

/**
 * Exception thrown when a parser encounters an error during dependency parsing.
 */
public class ParserException extends Exception {
    
    public ParserException(String message) {
        super(message);
    }
    
    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ParserException(Throwable cause) {
        super(cause);
    }
}

