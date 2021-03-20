package com.hphc.remoteapi.registration;

import org.apache.http.client.methods.HttpUriRequest;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandResponse;

import java.net.URI;

import static com.hphc.remoteapi.registration.FdahpUserRegWSCommandUtils.addHeaders;

public class FdahpUserRegWSCommand extends Command<CommandResponse>
{
    private final FdahpUserRegWSCommandHeaders _headers = new FdahpUserRegWSCommandHeaders();

    public FdahpUserRegWSCommand(String actionName)
    {
        super(FdahpUserRegWSCommandUtils.CONTROLLER, actionName);
    }

    @Override
    protected HttpUriRequest createRequest(URI uri)
    {
        return addHeaders(super.createRequest(uri), _headers);
    }
}
