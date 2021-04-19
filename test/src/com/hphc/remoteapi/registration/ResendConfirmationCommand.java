package com.hphc.remoteapi.registration;

import org.labkey.remoteapi.CommandResponse;

import java.util.Map;

public class ResendConfirmationCommand extends FdahpUserRegWSPostCommand<CommandResponse>
{
    public ResendConfirmationCommand(String orgId, String appId, String email)
    {
        super("resendConfirmation", orgId, appId);
        setParameters(Map.of("emailId", email));
    }
}
