package com.hphc.remoteapi.fdahpuserregws;

import org.json.JSONObject;

import java.util.Map;

public class LoginCommand extends BaseRegistrationCommand<LoginResponse>
{
    public LoginCommand(String orgId, String appId, String email, String password)
    {
        super("login", orgId, appId);
        setJsonObject(Map.of("emailId", email, "password", password));
    }

    @Override
    protected LoginResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new LoginResponse(text, status, contentType, json, this);
    }

    @Override
    protected String getRequestType()
    {
        return "POST";
    }
}
