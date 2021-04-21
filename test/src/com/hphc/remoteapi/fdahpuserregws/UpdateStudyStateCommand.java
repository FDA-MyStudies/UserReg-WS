package com.hphc.remoteapi.fdahpuserregws;

import com.hphc.remoteapi.fdahpuserregws.params.RegistrationSession;
import org.labkey.remoteapi.CommandResponse;

public class UpdateStudyStateCommand extends FdahpUserRegWSCommand<CommandResponse>
{
    public UpdateStudyStateCommand(RegistrationSession session)
    {
        super("updateStudyState", session);
    }

    @Override
    protected String getRequestType()
    {
        return "POST";
    }
}
