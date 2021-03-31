package com.hphc.remoteapi.registration;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;

import java.io.IOException;
import java.net.URI;

import static com.hphc.remoteapi.registration.FdahpUserRegWSCommandUtils.addHeaders;

public class FdahpUserRegWSCommand<ResponseType extends CommandResponse> extends Command<ResponseType>
{
    private final FdahpUserRegWSCommandHeaders _headers = new FdahpUserRegWSCommandHeaders();

    public FdahpUserRegWSCommand(String actionName)
    {
        super(FdahpUserRegWSCommandUtils.CONTROLLER, actionName);
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
        return addHeaders(request, _headers);
    }

    protected boolean isPost()
    {
        return false;
    }

    public void setApplicationId(String applicationId)
    {
        _headers._applicationId = applicationId;
    }

    public void setAuthKey(String auth)
    {
        _headers._auth = auth;
    }

    public void setUserId(String userId)
    {
        _headers._userId = userId;
    }

    public void setOrgId(String orgId)
    {
        _headers._orgId = orgId;
    }
}
