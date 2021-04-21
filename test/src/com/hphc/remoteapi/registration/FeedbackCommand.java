package com.hphc.remoteapi.registration;

import java.util.Map;

public class FeedbackCommand extends RegistrationCommand<LoginResponse>
{
    public FeedbackCommand(String orgId, String appId, String feedback)
    {
        super("login", orgId, appId);
        setParameters(Map.of("body", feedback));
    }

    @Override
    protected String getRequestType()
    {
        return "POST";
    }
}
