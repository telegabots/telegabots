package org.github.telegabots.java;

import org.github.telegabots.api.BaseCommand;
import org.github.telegabots.api.annotation.TextHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Command deliberately written in Java
 */
public class JavaSimpleCommand extends BaseCommand {
    @TextHandler
    public void handle(String text) {
        assertEquals(this, context.currentCommand());
    }
}
