package com.emwaver.ismwaver.ui.console;

import com.emwaver.ismwaver.CC1101;
import com.emwaver.ismwaver.Console;
import com.emwaver.ismwaver.Utils;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * This class can be used to evaluate any string expression using the open source,
 * RHINO javascript engine.
 *
 * Add this line - compile 'org.mozilla:rhino:1.7R4'
 * To your module app dependency gradle to install the jar library.
 *
 * Follow my tutorial at
 * {@link} https://github.com/brionsilva/Android-Rhino-Example
 *
 * @author  Brion Mario
 * @version 1.0
 * @since   2017-03-08
 */

public class ScriptsEngine{

    private Context rhino;
    private Scriptable scope;

    private CC1101 cc1101;


    private Console console;

    private Utils utils;


    private static final String SCRIPT = "function evaluate(arithmetic){ return eval(arithmetic); }";

    public ScriptsEngine(CC1101 cc1101, Console console, Utils utils) {
        this.cc1101 = cc1101;
        this.console = console;
        this.utils = utils;
    }


    public String executeJavaScript(String script) {
        String errorMessage = null;

        try {
            rhino = Context.enter();
            rhino.setOptimizationLevel(-1);
            scope = rhino.initStandardObjects();

            // Make entire CC1101, Serial, and Console classes accessible from javascript
            ScriptableObject.putProperty(scope, "CC1101", Context.javaToJS(cc1101, scope));
            ScriptableObject.putProperty(scope, "Console", Context.javaToJS(console, scope));
            ScriptableObject.putProperty(scope, "Utils", Context.javaToJS(utils, scope));

            // Execute the JavaScript script
            rhino.evaluateString(scope, script, "JavaScript", 1, null);
        } catch (RhinoException e) {
            e.printStackTrace();
            errorMessage = e.getMessage();
        } finally {
            Context.exit();
        }

        // Return the error message, or null if there was no error
        return errorMessage;
    }

}

