package co.fs.evo.http.response;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.servlet.AsyncContext;

import com.hazelcast.nio.DataSerializable;

import co.fs.evo.exceptions.resources.InternalErrorException;
import co.fs.evo.exceptions.resources.NotFoundException;
import co.fs.evo.exceptions.resources.ResourceException;
import co.fs.evo.http.request.RequestInfo;
import co.fs.evo.security.EvoUser;

public interface Response extends DataSerializable {

	public void send(RequestInfo request, AsyncContext ctx, EvoUser userDetails) throws ResourceException; 
    
	public void loadResource(RequestInfo requestInfo) throws NotFoundException, InternalErrorException;

	public void readData(DataInput in) throws IOException;

	public void writeData(DataOutput out) throws IOException;
    
}
