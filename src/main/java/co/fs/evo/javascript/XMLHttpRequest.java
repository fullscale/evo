package co.fs.evo.javascript;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.ScriptableObject;

public class XMLHttpRequest extends ScriptableObject {
    
    public static final long serialVersionUID = 1L;
  
    private String url;
    private String httpMethod;
    private HttpURLConnection urlConnection;
    private int httpStatus;
    private String httpStatusText;
    private Map<String, Object> requestHeaders;
    private String userName;
    private String password;
    private String responseText;
    private int readyState;
    private NativeFunction readyStateChangeFunction;
    private boolean asyncFlag;
    private Thread asyncThread;

    public void jsConstructor() { }

    public String getClassName() {
        return "XMLHttpRequest";
    }

    public void jsFunction_setRequestHeader(String headerName, String value) {
        if (this.readyState > 1) {
            throw new IllegalStateException("request already in progress");
        }

        if (this.requestHeaders == null) {
            this.requestHeaders = new HashMap<String, Object>();
        }

        this.requestHeaders.put(headerName, value);
    }

    public Map<String,List<String>> jsFunction_getAllResponseHeaders() {
        if (this.readyState < 3) {
            throw new IllegalStateException(
                "must call send before getting response headers");
        }
        return this.urlConnection.getHeaderFields();
    }

    public String jsFunction_getResponseHeader(String headerName) {
        return jsFunction_getAllResponseHeaders().get(headerName).toString();
    }

    public void jsFunction_open(String httpMethod, String url, boolean asyncFlag, String userName, String password) {
        if (this.readyState != 0) {
            throw new IllegalStateException("already open");
        }

        this.httpMethod = httpMethod;

        if (url.startsWith("http")) {
            this.url = url;
        } else {
            throw new IllegalArgumentException("URL protocol must be http: " + url);
        }

        this.asyncFlag = asyncFlag;

        if (("undefined".equals(userName)) || ("".equals(userName))) {
            this.userName = null;
        } else {
            this.userName = userName;
        }
        
        if (("undefined".equals(password)) || ("".equals(password))) {
            this.password = null;
        } else {
            this.password = password;
        }
        
        if (this.userName != null) {
            setAuthenticator();
        }

        setReadyState(1);
    }

    private void setAuthenticator() {
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(XMLHttpRequest.this.userName, XMLHttpRequest.this.password.toCharArray());
            } 
        });
    }

    public void jsFunction_send(Object o) {
        String content = o == null ? "" : o.toString();
        if (this.asyncFlag) {
            // no support for async
            doSend(content);
        } else {
            doSend(content);
        }
    }

    public void jsFunction_abort() {
        if (this.asyncThread != null)
            this.asyncThread.interrupt();
    }

    public int jsGet_readyState() {
        return this.readyState;
    }

    public String jsGet_responseText() {
        if (this.readyState < 2) {
            throw new IllegalStateException("request not yet sent");
        }
        return this.responseText;
    }

    public String jsGet_responseXML() {
        return "";
    }

    public String jsGet_url() {
        return this.url;
    }

    public String jsGet_method() {
        return this.httpMethod;
    }

    public int jsGet_status() {
        return this.httpStatus;
    }

    public String jsGet_statusText() {
        return this.httpStatusText;
    }

    public Object jsGet_onreadystatechange() {
        return this.readyStateChangeFunction;
    }

    public void jsSet_onreadystatechange(NativeFunction function) {
        this.readyStateChangeFunction = function;
    }

    private void doSend(String content) {
        try {
            connect(content);
            setRequestHeaders();
            this.urlConnection.connect();
            sendRequest(content);

            // why only limit reading responses for POST and GET???
            /*
            if (("POST".equals(this.httpMethod)) || ("GET".equals(this.httpMethod))) {
                readResponse();
            }
            */
            readResponse();
        } catch (Exception e) {
            if (this.httpStatus == 0) {
                this.httpStatus = 500;
                this.httpStatusText = e.getMessage();
            } 
        }

        setReadyState(4);
    }

    private void connect(String content) {
        try {
            URL url = new URL(this.url);
            this.urlConnection = ((HttpURLConnection)url.openConnection());
            this.urlConnection.setRequestMethod(this.httpMethod);
            
            if (("POST".equals(this.httpMethod)) || (content.length() > 0)) {
                this.urlConnection.setDoOutput(true);
            }
            
            if (("POST".equals(this.httpMethod)) || ("GET".equals(this.httpMethod))) {
                this.urlConnection.setDoInput(true);
            }
            this.urlConnection.setUseCaches(false);
        } catch (MalformedURLException e) {
            throw new RuntimeException("MalformedURLException: " + e, e);
        } catch (IOException e) {
            throw new RuntimeException("IOException: " + e, e);
        }
    }

    private void setRequestHeaders() {
        if (this.requestHeaders != null) {
            for (Iterator<String> i = this.requestHeaders.keySet().iterator(); i.hasNext(); ) {
                String header = (String)i.next();
                String value = (String)this.requestHeaders.get(header);
                this.urlConnection.setRequestProperty(header, value);
            }
        }
    }

    private void sendRequest(String content) {
        try {
            if (("POST".equals(this.httpMethod)) || (content.length() > 0)) {
                OutputStreamWriter out = new OutputStreamWriter(this.urlConnection.getOutputStream(), "ASCII");
                out.write(content);
                out.flush();
                out.close();
            }

            this.httpStatus = this.urlConnection.getResponseCode();
            this.httpStatusText = this.urlConnection.getResponseMessage();
        } catch (IOException e) {
            throw new RuntimeException("IOException: " + e, e);
        }

        setReadyState(2);
    }

    private void readResponse() {
        try {
            InputStream is = this.urlConnection.getInputStream();
            StringBuffer sb = new StringBuffer();

            setReadyState(3);
            int i;
            while ((i = is.read()) != -1) {
                //int i;
                sb.append((char)i);
            }
            is.close();

            this.responseText = sb.toString();
        } catch (IOException e) {
            throw new RuntimeException("IOException: " + e, e);
        }
    }

    private void setReadyState(int state) {
        this.readyState = state;
        callOnreadyStateChange();
    }

    private void callOnreadyStateChange() {}
}
