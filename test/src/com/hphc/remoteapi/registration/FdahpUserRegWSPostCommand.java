package com.hphc.remoteapi.registration;

import org.apache.http.client.methods.HttpUriRequest;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;

import java.net.URI;

import static com.hphc.remoteapi.registration.FdahpUserRegWSCommandUtils.addHeaders;

public class FdahpUserRegWSPostCommand extends PostCommand<CommandResponse>
{
    private final FdahpUserRegWSCommandHeaders _headers = new FdahpUserRegWSCommandHeaders();

    public FdahpUserRegWSPostCommand(String actionName)
    {
        super(FdahpUserRegWSCommandUtils.CONTROLLER, actionName);
    }

    @Override
    protected HttpUriRequest createRequest(URI uri)
    {
        return addHeaders(super.createRequest(uri), _headers);
    }
}
