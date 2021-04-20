package com.hphc.remoteapi.registration;

import com.hphc.remoteapi.registration.params.RegistrationSession;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.CommandResponse;

public class LoginResponse extends CommandResponse
{
    private final String _orgId;
    private final String _appId;
    private final String _auth;
    private final String _userId;

    public LoginResponse(String text, int statusCode, String contentType, JSONObject json, RegistrationCommand<?> sourceCommand)
    {
        super(text, statusCode, contentType, json, sourceCommand);
        _orgId = sourceCommand.getOrgId();
        _appId = sourceCommand.getApplicationId();
        _auth = (String) json.get("auth");
        _userId = (String) json.get("userId");
    }

    public RegistrationSession getAuth()
    {
        return new RegistrationSession(_orgId, _appId, _auth, _userId);
    }
}
