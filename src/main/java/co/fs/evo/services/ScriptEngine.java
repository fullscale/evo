package co.fs.evo.services;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.tika.io.IOUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import co.fs.evo.exceptions.resources.InternalErrorException;
import co.fs.evo.exceptions.resources.NotFoundException;
import co.fs.evo.exceptions.resources.ResourceException;
import co.fs.evo.javascript.ElasticJsClient;
import co.fs.evo.javascript.JSGIRequest;
import co.fs.evo.javascript.JavascriptObject;
import co.fs.evo.javascript.NodeClient;
import co.fs.evo.javascript.PrimitiveWrapFactory;
import co.fs.evo.javascript.XMLHttpRequest;

@Component
public class ScriptEngine {

    private static final XLogger logger = XLoggerFactory.getXLogger(ScriptEngine.class);
    
    protected Map<String, Script> scriptCache;

    @Autowired
    private ConfigService config;

    private ScriptableObject sharedScope = null;
    
    public ScriptEngine() {
    	scriptCache = new ConcurrentHashMap<String, Script>();
    }
    
    public void evict(String k) {
    	scriptCache.remove(k);
    }
    
    public void execute(Context cx, 
    					Scriptable scope, 
    					String code, 
    					String cacheKey) {
    	
        Script script = scriptCache.get(cacheKey);
        if (script != null) {
        	logger.debug("Cache Hit: {}", cacheKey);
        	script.exec(cx, scope);
        } else {
        	logger.debug("Cache Miss: {}", cacheKey);
        	script = compileScript(code);
        	scriptCache.put(cacheKey, script);
        	logger.debug("Added Entry: {}", cacheKey);
        	script.exec(cx, scope);
        }
    }
    
    /**
     * Compiles the code into a script object
     * 
     * @param code the code to compile
     * @return the compiled script
     */
    public Script compileScript(String code) {
        logger.entry();
        Context cx = getContext();
        Script script = null;

        try {
            logger.debug("compiling script");
            script = cx.compileString(code, null, 1, null);
        } finally {
            Context.exit();
        }

        logger.exit();
        return script;
    }

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
    
    /**
     * Executes the javascript controller
     * 
     * @param script the javascript controller script to execute
     * @param jsgi the request object
     * @return response object
     * @throws InternalErrorException on error executing javascript
     * @throws NotFoundException when the expected object is not found
     */
    public JavascriptObject evaluateJavascript(JSGIRequest jsgi, 
    										   String code, 
    										   String cacheKey) 
    	throws ResourceException {
    	
        logger.entry();

        JavascriptObject jsResponse = null;
        JavascriptObject jsRequest = jsgi.env();
        String controller = jsRequest.get("controller");
        String action = jsRequest.get("action");
        logger.debug("controller: {}", controller);
        logger.debug("action: {}", action);
        
        // create Rhino context
        Context cx = getContext();

        try {
            // create scope based on our shared scope
            logger.debug("creating local scope based on shared scope");
            Scriptable scope = cx.newObject(getSharedScope());
            scope.setPrototype(getSharedScope());
            scope.setParentScope(null);

            // add request values to scope
            scope.put("REQUEST", scope, jsRequest.value());
            
            // execute script
            execute(cx, scope, code, cacheKey);

            // pull out the controller function
            Object controllerObj = scope.get(controller, scope);

            // ensure it's a valid function
            if (!(controllerObj instanceof Function)) {
                logger.debug("Controller not a function: {}", controllerObj);
                throw new NotFoundException("Controller Not Found");
            } else {
                // call the controller function
                Object functionArgs[] = {"test"};
                Function f = (Function) controllerObj;
                ScriptableObject result = (ScriptableObject) f.call(cx, scope, scope, functionArgs);

                // actions are properties of the controller's return value
                // they should point to functions
                Object actionMethod = ScriptableObject.getProperty(result, action);

                // ensure the action property does in fact point to a function
                if (!(actionMethod instanceof Function)) {
                    logger.debug("Action not a function: {}", actionMethod);
                    throw new NotFoundException("Action Not Found");
                } else {
                    // call the controller action
                    Object fargs[] = {jsRequest.value()};
                    Function ff = (Function) actionMethod;
                    Object response = ff.call(cx, scope, result, fargs);

                    if (response instanceof String) {
                        jsResponse = new JavascriptObject();
                        jsResponse.put("body", (String) response);
                        jsResponse.put("contentType", "text/plain");
                    } else if (response instanceof NativeObject) {
                        jsResponse = new JavascriptObject((NativeObject) response);
                    } else {
                        logger.debug("Unknown action response: {}", response);
                        throw new InternalErrorException("Unknown action response");
                    }
                }
            }
        } finally {
            Context.exit();
        }

        logger.exit();
        return jsResponse;
    }
}
