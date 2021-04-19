package com.hphc.remoteapi.registration;

import org.labkey.remoteapi.CommandResponse;

import java.util.Map;

public class LoginCommand extends FdahpUserRegWSPostCommand<CommandResponse>
{
    public LoginCommand(String orgId, String appId, String email, String password)
    {
        super("login", orgId, appId);
        setParameters(Map.of("emailId", email, "password", password));
    }
}
