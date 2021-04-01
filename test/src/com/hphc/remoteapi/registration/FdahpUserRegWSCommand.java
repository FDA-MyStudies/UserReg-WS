package com.hphc.remoteapi.registration;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class FdahpUserRegWSCommand<ResponseType extends CommandResponse> extends Command<ResponseType>
{
    private static final String CONTROLLER = "fdahpuserregws";
    private final Map<String, String> _headers = new HashMap<>();

    public FdahpUserRegWSCommand(String actionName)
    {
        super(CONTROLLER, actionName);
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
    protected HttpUriRequest createRequest(URI uri)
    {
        HttpUriRequest request = isPost() ? new HttpPost(uri) : super.createRequest(uri);
        for (Map.Entry<String, String> header : _headers.entrySet())
        {
            request.addHeader(header.getKey(), header.getValue());
        }
        return request;
    }

    @Override
    public void setParameters(Map<String, Object> parameters)
    {
        super.setParameters(new HashMap<>(parameters));
    }

    protected boolean isPost()
    {
        return false;
    }

    // Header setters

    public void setApplicationId(String applicationId)
    {
        _headers.put("applicationId", applicationId);
    }

    public void setAuthKey(String auth)
    {
        _headers.put("auth", auth);
    }

    public void setUserId(String userId)
    {
        _headers.put("userId", userId);
    }

    public void setOrgId(String orgId)
    {
        _headers.put("orgId", orgId);
    }
}
