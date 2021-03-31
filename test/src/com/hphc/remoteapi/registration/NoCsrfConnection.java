package com.hphc.remoteapi.registration;

import org.apache.http.HttpRequest;
import org.labkey.remoteapi.Connection;

import java.io.IOException;
import java.net.URISyntaxException;

public class NoCsrfConnection extends Connection
{
    public NoCsrfConnection(String baseUrl) throws URISyntaxException, IOException
    {
        super(baseUrl);
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
