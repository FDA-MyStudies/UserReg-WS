package com.hphc.remoteapi.fdahpuserregws;

import java.util.Map;

public class FeedbackCommand extends BaseRegistrationCommand<LoginResponse>
{
    public FeedbackCommand(String orgId, String appId, String feedback)
    {
        super("feedback", orgId, appId);
        setJsonObject(Map.of("body", feedback));
    }

    @Override
    protected String getRequestType()
    {
        return "POST";
    }
}
