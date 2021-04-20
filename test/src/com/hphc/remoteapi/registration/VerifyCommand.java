package com.hphc.remoteapi.registration;

import org.labkey.remoteapi.CommandResponse;

import java.util.Map;

public class VerifyCommand extends RegistrationCommand<CommandResponse>
{
    public VerifyCommand(String orgId, String appId, String email, String code)
    {
        super("verify", orgId, appId);
        setParameters(Map.of("emailId", email, "code", code));
    }

    @Override
    protected String getRequestType()
    {
        return "POST";
    }
}
