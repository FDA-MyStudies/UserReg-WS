package com.hphc.remoteapi.fdahpuserregws;

import org.json.simple.JSONObject;

public class FeedbackCommand extends FdahpUserRegWSCommand<LoginResponse>
{
    final String _feedback;

    public FeedbackCommand(String orgId, String appId, String feedback)
    {
        super("feedback", orgId, appId);
        _feedback = feedback;
    }

    @Override
    protected JSONObject getJsonObject()
    {
        JSONObject json = new JSONObject();
        json.put("body", _feedback);
        return json;
    }

    @Override
    protected String getRequestType()
    {
        return "POST";
    }
}
