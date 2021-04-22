package com.hphc.remoteapi.fdahpuserregws;

import org.labkey.remoteapi.CommandResponse;

import java.util.Map;

public class VerifyCommand extends FdahpUserRegWSCommand<CommandResponse>
{
    public VerifyCommand(String orgId, String appId, String email, String code)
    {
        super("verify", orgId, appId);
        setJsonObject(Map.of("emailId", email, "code", code));
    }

    @Override
    protected String getRequestType()
    {
        return "POST";
    }
}
