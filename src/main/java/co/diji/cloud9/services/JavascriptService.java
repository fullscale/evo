package co.diji.cloud9.services;

import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.tika.io.IOUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import co.diji.cloud9.javascript.NodeClient;
import co.diji.cloud9.javascript.PrimitiveWrapFactory;
import co.diji.cloud9.javascript.XMLHttpRequest;
import co.diji.cloud9.javascript.ElasticJsClient;

@Component
public class JavascriptService {

    private static final XLogger logger = XLoggerFactory.getXLogger(JavascriptService.class);

    @Autowired
    private ConfigService config;

    private ScriptableObject sharedScope = null;

    /**
     * Loads javascript resources into shared scope
     */
    public ScriptableObject initializeSharedScope() {
        logger.entry();
        Context cx = getContext();

        // provides access to importPackage and importClass
        // seal standard imports
        ScriptableObject scope = new ImporterTopLevel(cx, true);

        try {
            // for ajax calls
            ScriptableObject.defineClass(scope, XMLHttpRequest.class);
            ScriptableObject.defineClass(scope, ElasticJsClient.class);
            ScriptableObject.defineClass(scope, NodeClient.class);

            // used in cloud9 javascript api to detect if we are running server side or not
            scope.put("ServerSideC9", scope, true);

            // load shared libs
            loadLib(cx, scope, "underscore", "file:resources/js/underscore-min.js");
            loadLib(cx, scope, "ejs", "file:resources/js/elastic.min.js");
            loadLib(cx, scope, "elastic-client-loader", "file:resources/js/elastic-client-loader.js");

            // seal everything not already sealed
            scope.sealObject();
        } catch (Exception e) {
            logger.warn("Unable to load server side javascript shared scope: {}", e.getMessage(), e);
        } finally {
            Context.exit();
        }

        logger.exit();
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
        logger.entry(name, path);
        Reader reader = null;
        try {
            reader = new InputStreamReader(config.getResourceInputStream(path));
            cx.evaluateReader(scope, reader, name, 1, null);
        } catch (Exception e) {
            logger.warn("Error loading lib: {}", name, e);
        } finally {
            if (reader != null) {
                IOUtils.closeQuietly(reader);
            }
        }
        logger.exit();
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
