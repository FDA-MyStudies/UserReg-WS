package com.hphc.remoteapi.registration;

import org.labkey.remoteapi.CommandResponse;

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
