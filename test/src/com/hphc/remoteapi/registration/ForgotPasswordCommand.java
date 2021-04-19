package com.hphc.remoteapi.registration;

import org.labkey.remoteapi.CommandResponse;

import java.util.Map;

public class ForgotPasswordCommand extends FdahpUserRegWSPostCommand<CommandResponse>
{
    public ForgotPasswordCommand(String orgId, String appId, String email)
    {
        super("forgotPassword", orgId, appId);
        setParameters(Map.of("emailId", email));
    }
}
