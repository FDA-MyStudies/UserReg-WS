package com.hphc.remoteapi.registration;

public class UserProfileCommand extends FdahpUserRegWSCommand
{
    public UserProfileCommand(String orgId, String appId)
    {
        super("userProfile", orgId, appId);
    }
}
