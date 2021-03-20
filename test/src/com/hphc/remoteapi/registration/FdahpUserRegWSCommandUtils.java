package com.hphc.remoteapi.registration;

import org.apache.http.client.methods.HttpUriRequest;

class FdahpUserRegWSCommandUtils
{
    static final String CONTROLLER = "fdahpuserregws";

    private FdahpUserRegWSCommandUtils()
    {
    }

    static HttpUriRequest addHeaders(HttpUriRequest request, FdahpUserRegWSCommandHeaders headers)
    {
        if (headers._auth != null)
        {
            request.addHeader("auth", headers._auth);
        }
        if (headers._applicationId != null)
        {
            request.addHeader("applicationId", headers._applicationId);
        }
        if (headers._orgId != null)
        {
            request.addHeader("orgId", headers._orgId);
        }
        if (headers._userId != null)
        {
            request.addHeader("userId", headers._userId);
        }

        return request;
    }
}
