package com.hphc.remoteapi.fdahpuserregws;

import com.hphc.remoteapi.fdahpuserregws.params.RegistrationSession;
import org.json.JSONObject;
import org.labkey.remoteapi.CommandResponse;

public class LoginResponse extends CommandResponse
{
    private final String _orgId;
    private final String _appId;
    private final String _auth;
    private final String _userId;

    private final String _refreshToken;
    private final Boolean _verified;

    public LoginResponse(String text, int statusCode, String contentType, JSONObject json, BaseRegistrationCommand<?> sourceCommand)
    {
        super(text, statusCode, contentType, json, sourceCommand);
        _orgId = sourceCommand.getOrgId();
        _appId = sourceCommand.getApplicationId();
        _auth = (String) json.get("auth");
        _userId = (String) json.get("userId");

        _refreshToken = (String) json.get("refreshToken");
        _verified = (Boolean) json.get("verified");
    }

    public RegistrationSession getSession()
    {
        return new RegistrationSession(_orgId, _appId, _auth, _userId);
    }

    public String getRefreshToken()
    {
        return _refreshToken;
    }

    public Boolean getVerified()
    {
        return _verified;
    }
}
