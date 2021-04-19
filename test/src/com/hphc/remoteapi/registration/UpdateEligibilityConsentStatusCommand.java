package com.hphc.remoteapi.registration;

import org.labkey.remoteapi.CommandResponse;

public class UpdateEligibilityConsentStatusCommand extends FdahpUserRegWSPostCommand<CommandResponse>
{
    public UpdateEligibilityConsentStatusCommand(String orgId, String appId)
    {
        super("updateEligibilityConsentStatus", orgId, appId);
    }
}
