package io.openliberty.tools.scanner.eclipse;

import io.openliberty.tools.scanner.parser.DependencyParser;
import io.openliberty.tools.scanner.parser.DependencyParserProvider;

import java.util.Collections;
import java.util.List;

/**
 * Eclipse adapter provider.
 *
 * This module is intended to use Eclipse JDT, Buildship, and M2E resolved
 * project/classpath models when packaged with Eclipse platform dependencies.
 */
public class EclipseDependencyParserProvider implements DependencyParserProvider {

    @Override
    public List<DependencyParser> getParsers() {
        return Collections.singletonList(new EclipseProjectModelParser());
    }

    @Override
    public int getPriority() {
        return 5;
    }
}


