package com.hphc.remoteapi.registration;

import org.labkey.remoteapi.CommandResponse;

import java.util.Map;

public class RegisterCommand extends RegistrationCommand<CommandResponse>
{
    public RegisterCommand(String orgId, String appId, String email, String password)
    {
        super("register", orgId, appId);
        setParameters(Map.of("emailId", email, "password", password));
    }

    @Override
    protected String getRequestType()
    {
        return "POST";
    }
}
