package com.hphc.remoteapi.fdahpuserregws;

import com.hphc.remoteapi.fdahpuserregws.params.RegistrationSession;
import org.labkey.remoteapi.CommandResponse;

public class LogoutCommand extends BaseRegistrationCommand<CommandResponse>
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
