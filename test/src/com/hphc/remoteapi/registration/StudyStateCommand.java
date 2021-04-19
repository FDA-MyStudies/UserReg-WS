package com.hphc.remoteapi.registration;

import org.labkey.remoteapi.CommandResponse;

public class StudyStateCommand extends FdahpUserRegWSCommand<CommandResponse>
{
    public StudyStateCommand(String orgId, String appId)
    {
        super("studyState", orgId, appId);
    }
}
