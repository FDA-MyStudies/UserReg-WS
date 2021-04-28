package com.hphc.remoteapi.fdahpuserregws.params;

public class RegistrationSession
{
    private final String _orgId;
    private final String _applicationId;
    private final String _auth;
    private final String _userId;

    public RegistrationSession(String orgId, String applicationId, String auth, String userId)
    {
        _orgId = orgId;
        _applicationId = applicationId;
        _auth = auth;
        _userId = userId;
    }

    public String getOrgId()
    {
        return _orgId;
    }

    public String getApplicationId()
    {
        return _applicationId;
    }

    public String getAuth()
    {
        return _auth;
    }

    public String getUserId()
    {
        return _userId;
    }
}
