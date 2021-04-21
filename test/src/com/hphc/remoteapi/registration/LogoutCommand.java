package com.hphc.remoteapi.registration;

import com.hphc.remoteapi.registration.params.RegistrationSession;
import org.labkey.remoteapi.CommandResponse;

public class LogoutCommand extends RegistrationCommand<CommandResponse>
{
    public LogoutCommand(RegistrationSession session)
    {
        super("logout", session);
    }

    @Override
    protected String getRequestType()
    {
        return "DELETE";
    }
}
