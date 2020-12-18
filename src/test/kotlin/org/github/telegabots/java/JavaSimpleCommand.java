package org.github.telegabots.java;

import org.github.telegabots.BaseCommand;
import org.github.telegabots.annotation.CommandHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Command deliberately written in Java
 */
public class JavaSimpleCommand extends BaseCommand {
    @CommandHandler
    public void handle(String text) {
        assertEquals(this, context.currentCommand());
    }
}
