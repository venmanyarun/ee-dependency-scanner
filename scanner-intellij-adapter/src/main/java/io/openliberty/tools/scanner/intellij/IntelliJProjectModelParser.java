package io.openliberty.tools.scanner.intellij;

import io.openliberty.tools.scanner.model.DependencyInfo;
import io.openliberty.tools.scanner.parser.DependencyParser;
import io.openliberty.tools.scanner.parser.ParserException;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Placeholder IntelliJ parser.
 *
 * Real implementation must use IntelliJ project/module/library model APIs
 * and resolved library/order-entry information. It must not use PSI for
 * dependency resolution.
 */
public class IntelliJProjectModelParser implements DependencyParser {

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
        return "IntelliJ Project Model";
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


