package com.hphc.remoteapi.registration;

import com.hphc.remoteapi.registration.params.RegistrationSession;
import org.labkey.remoteapi.CommandResponse;

public class UpdateEligibilityConsentStatusCommand extends RegistrationCommand<CommandResponse>
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
