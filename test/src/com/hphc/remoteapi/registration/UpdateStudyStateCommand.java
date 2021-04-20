package com.hphc.remoteapi.registration;

import com.hphc.remoteapi.registration.params.RegistrationSession;
import org.labkey.remoteapi.CommandResponse;

public class UpdateStudyStateCommand extends RegistrationCommand<CommandResponse>
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
