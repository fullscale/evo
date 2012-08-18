package co.diji.cloud9.javascript;

import java.io.FileReader;
import java.io.Reader;

import org.apache.tika.io.IOUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import co.diji.cloud9.services.ConfigService;

@Component
public class JavascriptHelper {

    protected static final Logger logger = LoggerFactory.getLogger(JavascriptHelper.class);

    @Autowired
    private ConfigService config;

    private ScriptableObject sharedScope = null;

    /**
     * Loads javascript resources into shared scope
     */
    public ScriptableObject initializeSharedScope() {
        logger.trace("initializing shared scope");
        Context cx = getContext();

        // provides access to importPackage and importClass
        // seal standard imports
        ScriptableObject scope = new ImporterTopLevel(cx, true);

        try {
            // for ajax calls
            ScriptableObject.defineClass(scope, XMLHttpRequest.class);

            // used in cloud9 javascript api to detect if we are running server side or not
            scope.put("ServerSideC9", scope, true);

            // load shared libs
            loadLib(cx, scope, "underscore", "/resources/js/underscore-min.js");
            loadLib(cx, scope, "c9api", "/resources/js/c9/c9api.min.js");

            // seal everything not already sealed
            scope.sealObject();
        } catch (Exception e) {
            logger.warn("Unable to load server side javascript shared scope: {}", e.getMessage(), e);
        } finally {
            Context.exit();
        }

        return scope;
    }

    /**
     * Enter's Rhino execution context
     * 
     * @return the context
     */
    public Context getContext() {
        Context cx = Context.enter();
        cx.setLanguageVersion(180);
        PrimitiveWrapFactory wrapper = new PrimitiveWrapFactory();
        wrapper.setJavaPrimitiveWrap(false);
        cx.setWrapFactory(wrapper);
        cx.setOptimizationLevel(9);

        return cx;
    }

    /**
     * Loads shared libs into the scope
     * 
     * @param cx the context
     * @param scope the scope to load the lib into
     * @param name the script name
     * @param path the path to the lib
     */
    public void loadLib(Context cx, ScriptableObject scope, String name, String path) {
        logger.trace("in loadLib name:{} path:{}", name, path);
        Reader reader = null;
        try {
            reader = new FileReader(config.getResourceFile(path));
            cx.evaluateReader(scope, reader, name, 1, null);
        } catch (Exception e) {
            logger.warn("Error loading lib: {}", name, e);
        } finally {
            if (reader != null) {
                IOUtils.closeQuietly(reader);
            }
        }
        logger.trace("exit loadLib");
    }

    /**
     * @return the shared scope
     */
    public ScriptableObject getSharedScope() {
        // initialize on first use
        if (sharedScope == null) {
            sharedScope = initializeSharedScope();
        }

        return sharedScope;
    }
}
