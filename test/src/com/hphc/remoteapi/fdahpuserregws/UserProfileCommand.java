package com.hphc.remoteapi.fdahpuserregws;

import org.labkey.remoteapi.CommandResponse;

public class UserProfileCommand extends FdahpUserRegWSCommand<CommandResponse>
{
    public UserProfileCommand(String orgId, String appId)
    {
        super("userProfile", orgId, appId);
    }
}
