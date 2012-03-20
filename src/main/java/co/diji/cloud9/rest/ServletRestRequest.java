package co.diji.cloud9.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.common.Unicode;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.support.AbstractRestRequest;
import org.elasticsearch.rest.support.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServletRestRequest extends AbstractRestRequest implements RestRequest {

    private static final Logger logger = LoggerFactory.getLogger(ServletRestRequest.class);

    private final HttpServletRequest servletRequest;
    private final Method method;
    private final Map<String, String> params;
    private final String path;
    private final byte[] content;

    public ServletRestRequest(HttpServletRequest servletRequest) throws IOException {
        this.servletRequest = servletRequest;
        this.method = Method.valueOf(servletRequest.getMethod());
        this.params = new HashMap<String, String>();

        // substring(3) removes "/v1"
        this.path = servletRequest.getServletPath().substring(3);

        if (servletRequest.getQueryString() != null) {
            RestUtils.decodeQueryString(servletRequest.getQueryString(), 0, params);
        }

        content = Streams.copyToByteArray(servletRequest.getInputStream());
        logger.debug("method:{} path:{} params:{}", new Object[]{method, path, params});
        logger.debug("content: {}", new String(content));
    }

    @Override
    public Method method() {
        return this.method;
    }

    @Override
    public String uri() {
        return this.path;
    }

    @Override
    public String rawPath() {
        return this.path;
    }

    @Override
    public boolean hasContent() {
        return content.length > 0;
    }

    @Override
    public boolean contentUnsafe() {
        return false;
    }

    @Override
    public byte[] contentByteArray() {
        return content;
    }

    @Override
    public int contentByteArrayOffset() {
        return 0;
    }

    @Override
    public int contentLength() {
        return content.length;
    }

    @Override
    public String contentAsString() {
        return Unicode.fromBytes(contentByteArray(), contentByteArrayOffset(), contentLength());
    }

    @Override
    public String header(String name) {
        return servletRequest.getHeader(name);
    }

    @Override
    public Map<String, String> params() {
        return params;
    }

    @Override
    public boolean hasParam(String key) {
        return params.containsKey(key);
    }

    @Override
    public String param(String key) {
        return params.get(key);
    }

    @Override
    public String param(String key, String defaultValue) {
        String value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
}
