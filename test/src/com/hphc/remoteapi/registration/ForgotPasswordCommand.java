package com.hphc.remoteapi.registration;

import org.labkey.remoteapi.CommandResponse;

import java.util.Map;

public class ForgotPasswordCommand extends RegistrationCommand<CommandResponse>
{
    public ForgotPasswordCommand(String orgId, String appId, String email)
    {
        super("forgotPassword", orgId, appId);
        setParameters(Map.of("emailId", email));
    }

    @Override
    protected String getRequestType()
    {
        return "POST";
    }
}
