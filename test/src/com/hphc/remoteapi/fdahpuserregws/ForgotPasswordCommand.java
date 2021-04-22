package com.hphc.remoteapi.fdahpuserregws;

import org.labkey.remoteapi.CommandResponse;

import java.util.Map;

public class ForgotPasswordCommand extends BaseRegistrationCommand<CommandResponse>
{
    public ForgotPasswordCommand(String orgId, String appId, String email)
    {
        super("forgotPassword", orgId, appId);
        setJsonObject(Map.of("emailId", email));
    }

    @Override
    protected String getRequestType()
    {
        return "POST";
    }
}
