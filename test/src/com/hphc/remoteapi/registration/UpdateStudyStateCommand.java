package com.hphc.remoteapi.registration;

import org.labkey.remoteapi.CommandResponse;

public class UpdateStudyStateCommand extends FdahpUserRegWSPostCommand<CommandResponse>
{
    public UpdateStudyStateCommand(String orgId, String appId)
    {
        super("updateStudyState", orgId, appId);
    }
}
