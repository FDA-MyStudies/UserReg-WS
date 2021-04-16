package com.hphc.remoteapi.registration;

public class VerifyCommand extends FdahpUserRegWSPostCommand
{
    public VerifyCommand(String orgId, String appId)
    {
        super("verify", orgId, appId);
    }
}
