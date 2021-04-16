package com.hphc.remoteapi.registration;

public class ResendConfirmationCommand extends FdahpUserRegWSPostCommand
{
    public ResendConfirmationCommand(String orgId, String appId)
    {
        super("resendConfirmation", orgId, appId);
    }
}
