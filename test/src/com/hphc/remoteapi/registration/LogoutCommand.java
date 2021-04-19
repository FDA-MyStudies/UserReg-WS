package com.hphc.remoteapi.registration;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpUriRequest;
import org.labkey.remoteapi.CommandResponse;

import java.net.URI;
import java.util.Map;

public class LogoutCommand extends FdahpUserRegWSCommand<CommandResponse>
{
    public LogoutCommand(String orgId, String appId, String email, String auth)
    {
        super("logout", orgId, appId);
        setParameters(Map.of("emailId", email));
        setAuthKey(auth);
    }

    @Override
    protected HttpUriRequest _createRequest(URI uri)
    {
        return new HttpDelete(uri);
    }
}
