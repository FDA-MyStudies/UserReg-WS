package com.hphc.remoteapi.fdahpuserregws;

import com.hphc.remoteapi.fdahpuserregws.params.RegistrationSession;
import org.labkey.remoteapi.CommandResponse;

import java.util.Map;

public class ChangePasswordCommand extends FdahpUserRegWSCommand<CommandResponse>
{
    public ChangePasswordCommand(RegistrationSession auth, String currentPassword, String newPassword)
    {
        super("changePassword", auth);
        setParameters(Map.of("currentPassword", currentPassword, "newPassword", newPassword));
    }

    @Override
    protected String getRequestType()
    {
        return "POST";
    }
}
