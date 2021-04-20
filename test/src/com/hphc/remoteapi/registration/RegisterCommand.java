package com.hphc.remoteapi.registration;

import org.labkey.remoteapi.CommandResponse;

public class RegisterCommand extends RegistrationCommand<CommandResponse>
{
    public RegisterCommand(String orgId, String appId)
    {
        super("register", orgId, appId);
    }

    @Override
    protected String getRequestType()
    {
        return "POST";
    }
}
