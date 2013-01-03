package co.fs.evo.rest;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.bytes.ChannelBufferBytesReference;
import org.elasticsearch.common.netty.buffer.ChannelBuffers;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.support.AbstractRestRequest;
import org.elasticsearch.rest.support.RestUtils;

public class InternalRestRequest extends AbstractRestRequest implements RestRequest {
	
    private final Map<String, String> params;

    private final String rawPath;

    private final BytesReference content;
    private final String uri;
    private final Method method;
    
    public InternalRestRequest(String method, String uri, String payload) {
    	this.method = Method.valueOf(method);
    	this.uri = uri;
        this.params = new HashMap<String, String>();
        if (!payload.equals(null)) {
            this.content = new ChannelBufferBytesReference(ChannelBuffers.copiedBuffer(payload.getBytes()));
        } else {
            this.content = BytesArray.EMPTY;
        }

        int pathEndPos = uri.indexOf('?');
        if (pathEndPos < 0) {
            this.rawPath = uri;
        } else {
            this.rawPath = uri.substring(0, pathEndPos);
            RestUtils.decodeQueryString(uri, pathEndPos + 1, params);
        }

    }
	
    @Override
    public Method method() {
        return this.method;
    }

    @Override
    public String uri() {
        return this.uri;
    }

    @Override
    public String rawPath() {
        return this.rawPath;
    }

    @Override
    public boolean hasContent() {
        return content.length() > 0;
    }

    @Override
    public boolean contentUnsafe() {
        return false;
    }
    
    @Override
    public BytesReference content() {
    	return content;
    }

    @Override
    public String header(String name) {
        return "";
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
