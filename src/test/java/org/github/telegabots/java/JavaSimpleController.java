package org.github.telegabots.java;

import org.github.telegabots.api.BaseController;
import org.github.telegabots.api.annotation.TextHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Controller deliberately written in Java
 */
public class JavaSimpleController extends BaseController {
    @TextHandler
    public void handle(String text) {
        assertEquals(this, context.currentController());
    }
}
