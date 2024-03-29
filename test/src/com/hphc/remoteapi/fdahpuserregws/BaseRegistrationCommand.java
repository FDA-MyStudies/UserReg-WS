package com.hphc.remoteapi.fdahpuserregws;

import com.hphc.remoteapi.fdahpuserregws.params.RegistrationSession;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONObject;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class BaseRegistrationCommand<ResponseType extends CommandResponse> extends Command<ResponseType>
{
    public static final String CONTROLLER = "fdahpuserregws";

    private final RegistrationSession _session;

    private final JSONObject _json = new JSONObject();

    protected BaseRegistrationCommand(String actionName, RegistrationSession session)
    {
        super(CONTROLLER, actionName);
        _session = session;
    }

    protected BaseRegistrationCommand(String actionName, String orgId, String appId)
    {
        this(actionName, new RegistrationSession(orgId, appId, null, null));
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
        if (_session.getUserId() != null)
        {
            headers.put("userId", _session.getUserId());
        }
        if (_session.getAuth() != null)
        {
            headers.put("auth", _session.getAuth());
        }
        return headers;
    }

    protected HttpUriRequest _createRequest(URI uri)
    {
        return switch (getRequestType())
        {
            case "GET" -> super.createRequest(uri);
            case "POST" -> createPost(uri);
            case "DELETE" -> new HttpDelete(uri);
            default -> throw new IllegalArgumentException("Unsupported request type: " + getRequestType());
        };
    }

    protected String getRequestType()
    {
        return "GET";
    }

    protected final String getOrgId()
    {
        return _session.getOrgId();
    }

    protected final String getApplicationId()
    {
        return _session.getApplicationId();
    }

    private HttpUriRequest createPost(URI uri)
    {
        HttpPost request = new HttpPost(uri);

        if (null != _json && !_json.isEmpty())
        {
            request.setEntity(new StringEntity(_json.toString(), ContentType.APPLICATION_JSON));
        }

        return request;
    }

    protected void setJsonObject(Map<String, Object> json)
    {
        if (!"POST".equals(getRequestType()))
        {
            throw new IllegalArgumentException("JSON body will only be included in 'POST' requests");
        }
        _json.clear();
        json.forEach(_json::put);
    }

    @Override
    public void setParameters(Map<String, Object> parameters)
    {
        super.setParameters(new HashMap<>(parameters));
    }
}
