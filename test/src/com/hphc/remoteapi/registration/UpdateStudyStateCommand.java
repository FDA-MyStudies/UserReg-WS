package com.hphc.remoteapi.registration;

public class UpdateStudyStateCommand extends FdahpUserRegWSPostCommand
{
    public UpdateStudyStateCommand(String orgId, String appId)
    {
        super("updateStudyState", orgId, appId);
    }
}
