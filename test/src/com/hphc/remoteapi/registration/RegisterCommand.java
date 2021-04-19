package com.hphc.remoteapi.registration;

import org.labkey.remoteapi.CommandResponse;

public class RegisterCommand extends FdahpUserRegWSPostCommand<CommandResponse>
{
    public RegisterCommand(String orgId, String appId)
    {
        super("register", orgId, appId);
    }
}
