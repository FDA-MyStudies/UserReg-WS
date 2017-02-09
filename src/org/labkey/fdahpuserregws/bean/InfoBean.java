package org.labkey.fdahpuserregws.bean;

/**
 * Created by Ravinder on 2/7/2017.
 */
public class InfoBean
{
    String _os="";
    String _appVersion="";
    String _deviceToken="";

    public String getOs()
    {
        return _os;
    }

    public void setOs(String os)
    {
        _os = os;
    }

    public String getAppVersion()
    {
        return _appVersion;
    }

    public void setAppVersion(String appVersion)
    {
        _appVersion = appVersion;
    }

    public String getDeviceToken()
    {
        return _deviceToken;
    }

    public void setDeviceToken(String deviceToken)
    {
        _deviceToken = deviceToken;
    }
}
