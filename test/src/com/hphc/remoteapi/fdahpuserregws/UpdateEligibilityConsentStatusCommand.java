package com.hphc.remoteapi.fdahpuserregws;

import com.hphc.remoteapi.fdahpuserregws.params.RegistrationSession;
import org.labkey.remoteapi.CommandResponse;

public class UpdateEligibilityConsentStatusCommand extends FdahpUserRegWSCommand<CommandResponse>
{
    public UpdateEligibilityConsentStatusCommand(RegistrationSession session)
    {
        super("updateEligibilityConsentStatus", session);
    }

    @Override
    protected String getRequestType()
    {
        return "POST";
    }
}
