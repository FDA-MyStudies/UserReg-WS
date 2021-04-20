package com.hphc.remoteapi.registration;

import org.labkey.remoteapi.CommandResponse;

public class UserProfileCommand extends RegistrationCommand<CommandResponse>
{
    public UserProfileCommand(String orgId, String appId)
    {
        super("userProfile", orgId, appId);
    }
}
