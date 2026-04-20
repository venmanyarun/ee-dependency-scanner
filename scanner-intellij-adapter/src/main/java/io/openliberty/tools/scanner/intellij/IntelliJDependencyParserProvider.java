package io.openliberty.tools.scanner.intellij;

import io.openliberty.tools.scanner.parser.DependencyParser;
import io.openliberty.tools.scanner.parser.DependencyParserProvider;

import java.util.Collections;
import java.util.List;

/**
 * IntelliJ adapter provider.
 *
 * This module is intended to use IntelliJ project/module/library model APIs
 * for resolved dependency collection when packaged with IntelliJ platform
 * dependencies.
 */
public class IntelliJDependencyParserProvider implements DependencyParserProvider {

    @Override
    public List<DependencyParser> getParsers() {
        return Collections.singletonList(new IntelliJProjectModelParser());
    }

    @Override
    public int getPriority() {
        return 5;
    }
}


