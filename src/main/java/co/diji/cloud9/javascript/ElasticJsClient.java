package co.diji.cloud9.javascript;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.json.JsonParser;
import org.mozilla.javascript.json.JsonParser.ParseException;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

public class ElasticJsClient extends ScriptableObject {
	
    public static final long serialVersionUID = 1L;
    private static final XLogger logger = XLoggerFactory.getXLogger(ElasticJsClient.class);
    private String host = "http://localhost:2600/v1";

    public void jsConstructor() {}
    public String getClassName() { return "ElasticJsClient"; }

    public Object jsFunction_post(String path, Object data, NativeFunction callbackFn) {
        String content = data == null ? "" : data.toString();
        return handleResponse(execRestCall("POST", host + path, content), callbackFn);
    }
    
    public void jsFunction_put(String path, Object data, NativeFunction callbackFn)    {/* TODO */}
    public void jsFunction_get(String path, Object data, NativeFunction callbackFn)    {/* TODO */}
    public void jsFunction_delete(String path, Object data, NativeFunction callbackFn) {/* TODO */}

    private String execRestCall(String method, String uri, String content) {
        try {
        	/* connect to host */
            URL url = new URL(uri);
            HttpURLConnection urlConnection = ((HttpURLConnection)url.openConnection());
            urlConnection.setRequestMethod(method);
            
            if (content.length() > 0) {
                urlConnection.setDoOutput(true);
            }
            
            urlConnection.setDoInput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setRequestProperty("content-type", "application/json");
            urlConnection.connect();
            
            /* send request body */
            if (content.length() > 0) {
                OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream(), "ASCII");
                out.write(content);
                out.flush();
                out.close();
            }
                
            /* read the response */
            InputStream is = urlConnection.getInputStream();
            StringBuffer sb = new StringBuffer();

            int i;
            while ((i = is.read()) != -1) {
                sb.append((char)i);
            }
            is.close();
            return sb.toString();
            
        } catch (MalformedURLException e) {
            throw new RuntimeException("MalformedURLException: " + e, e);
        } catch (IOException e) {
            throw new RuntimeException("IOException: " + e, e);
        }
    }
    
    private Object handleResponse(String response, NativeFunction cb) {
        Object json = null;
        if (response != null && response.length() > 0) {
            Context cx = Context.enter();
            try {
                Scriptable scope = cx.initStandardObjects();
                json = new JsonParser(cx, scope).parseValue(response);
                if (cb != null) {
                	cb.call(cx, scope, this, new Object[] {json});
                }
            } catch (ParseException pe) {
            	logger.warn(pe.getMessage());
            } finally {
                Context.exit();
            }
        }
        return json;
    }
}
