package com.hphc.remoteapi.registration;

public class UpdateEligibilityConsentStatusCommand extends FdahpUserRegWSPostCommand
{
    public UpdateEligibilityConsentStatusCommand(String orgId, String appId)
    {
        super("updateEligibilityConsentStatus", orgId, appId);
    }
}
