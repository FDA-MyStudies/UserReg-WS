package com.hphc.remoteapi.fdahpuserregws;

import org.json.simple.JSONObject;

import java.util.Map;

public class FeedbackCommand extends FdahpUserRegWSCommand<LoginResponse>
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
