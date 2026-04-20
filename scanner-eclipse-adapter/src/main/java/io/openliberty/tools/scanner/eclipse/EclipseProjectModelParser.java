package io.openliberty.tools.scanner.eclipse;

import io.openliberty.tools.scanner.model.DependencyInfo;
import io.openliberty.tools.scanner.parser.DependencyParser;
import io.openliberty.tools.scanner.parser.ParserException;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Placeholder Eclipse parser.
 *
 * Real implementation must use Eclipse JDT, Buildship, and M2E resolved
 * classpath/project model APIs. It must not rely on raw .classpath parsing
 * when the resolved workspace model is available.
 */
public class EclipseProjectModelParser implements DependencyParser {

    @Override
    public List<DependencyInfo> parse(File path) throws ParserException {
        return Collections.emptyList();
    }

    @Override
    public boolean canParse(File path) {
        return false;
    }

    @Override
    public String getParserName() {
        return "Eclipse Project Model";
    }

    @Override
    public boolean isIdeProjectModelParser() {
        return true;
    }

    @Override
    public int getPriority() {
        return 5;
    }
}


