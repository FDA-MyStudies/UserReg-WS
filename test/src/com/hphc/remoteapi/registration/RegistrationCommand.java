package com.hphc.remoteapi.registration;

import com.hphc.remoteapi.registration.params.RegistrationSession;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class RegistrationCommand<ResponseType extends CommandResponse> extends Command<ResponseType>
{
    private static final String CONTROLLER = "fdahpuserregws";

    private final RegistrationSession _auth;

    protected RegistrationCommand(String actionName, RegistrationSession auth)
    {
        super(CONTROLLER, actionName);
        _auth = auth;
    }

    public RegistrationCommand(String actionName, String orgId, String appId)
    {
        this(actionName, new RegistrationSession(orgId, appId, null, null));
    }

    @Override
    public RegistrationCommand<?> copy()
    {
        return new RegistrationCommand<>(getActionName(), _auth);
    }

    @Override
    public ResponseType execute(Connection connection, String folderPath) throws IOException, CommandException
    {
        if (!(connection instanceof NoCsrfConnection))
        {
            throw new IllegalStateException("Don't use a standard API 'Connection' for registration APIs");
        }
        return super.execute(connection, folderPath);
    }

    @Override
    protected final HttpUriRequest createRequest(URI uri)
    {
        HttpUriRequest request = _createRequest(uri);
        for (Map.Entry<String, String> header : getHeaders().entrySet())
        {
            request.addHeader(header.getKey(), header.getValue());
        }
        return request;
    }

    protected Map<String, String> getHeaders()
    {
        Map<String, String> headers = new HashMap<>();
        headers.put("orgId", getOrgId());
        headers.put("applicationId", getApplicationId());
        if (_auth.getUserId() != null)
        {
            headers.put("userId", _auth.getUserId());
        }
        if (_auth.getAuth() != null)
        {
            headers.put("auth", _auth.getAuth());
        }
        return headers;
    }

    protected HttpUriRequest _createRequest(URI uri)
    {
        switch (getRequestType())
        {
            case "GET":
                return super.createRequest(uri);
            case "POST":
                return createPost(uri);
            case "DELETE":
                return new HttpDelete(uri);
            default:
                throw new IllegalArgumentException("Unsupported request type: " + getRequestType());
        }
    }

    protected String getRequestType()
    {
        return "GET";
    }

    protected final String getOrgId()
    {
        return _auth.getOrgId();
    }

    protected final String getApplicationId()
    {
        return _auth.getApplicationId();
    }

    private HttpUriRequest createPost(URI uri)
    {
        HttpPost request = new HttpPost(uri);

        //set the post body based on the supplied JSON object
        JSONObject json = getJsonObject();

        if (null != json)
        {
            request.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));
        }

        return request;
    }

    protected JSONObject getJsonObject()
    {
        return null;
    }

    @Override
    public void setParameters(Map<String, Object> parameters)
    {
        super.setParameters(new HashMap<>(parameters));
    }
}
