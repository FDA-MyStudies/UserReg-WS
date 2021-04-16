package com.hphc.remoteapi.registration;

public class RegisterCommand extends FdahpUserRegWSPostCommand
{
    public RegisterCommand(String orgId, String appId)
    {
        super("register", orgId, appId);
    }
}
