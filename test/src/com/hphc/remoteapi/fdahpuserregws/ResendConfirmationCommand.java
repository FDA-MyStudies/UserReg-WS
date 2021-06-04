package com.hphc.remoteapi.fdahpuserregws;

import org.labkey.remoteapi.CommandResponse;

import java.util.Map;

public class ResendConfirmationCommand extends BaseRegistrationCommand<CommandResponse>
{
    public ResendConfirmationCommand(String orgId, String appId, String email)
    {
        super("resendConfirmation", orgId, appId);
        setJsonObject(Map.of("emailId", email));
    }

    @Override
    protected String getRequestType()
    {
        return "POST";
    }
}
