package com.hphc.remoteapi.fdahpuserregws;

import org.labkey.remoteapi.CommandResponse;

import java.util.Map;

public class RegisterCommand extends BaseRegistrationCommand<CommandResponse>
{
    public RegisterCommand(String orgId, String appId, String email, String password)
    {
        super("register", orgId, appId);
        setJsonObject(Map.of("emailId", email, "password", password));
    }

    @Override
    protected String getRequestType()
    {
        return "POST";
    }
}
