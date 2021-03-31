package com.hphc.remoteapi.registration;

import org.apache.http.client.methods.HttpUriRequest;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;

import java.net.URI;

import static com.hphc.remoteapi.registration.FdahpUserRegWSCommandUtils.addHeaders;

public class FdahpUserRegWSPostCommand<ResponseType extends CommandResponse> extends FdahpUserRegWSCommand<ResponseType>
{
    public FdahpUserRegWSPostCommand(String actionName)
    {
        super(actionName);
    }

    @Override
    protected boolean isPost()
    {
        return true;
    }
}
