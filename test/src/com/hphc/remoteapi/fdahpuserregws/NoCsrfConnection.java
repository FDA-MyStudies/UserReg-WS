package com.hphc.remoteapi.fdahpuserregws;

import org.apache.http.HttpRequest;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.GuestCredentialsProvider;

public class NoCsrfConnection extends Connection
{
    public NoCsrfConnection(String baseUrl)
    {
        super(baseUrl, new GuestCredentialsProvider());
    }

    @Override
    protected void beforeExecute(HttpRequest request)
    {
        // Don't pre-authenticate or generate session
    }

    @Override
    protected void afterExecute()
    {
        // Don't save session info
    }
}
