package com.hphc.mystudies.bean;

public class AppPropertiesDetailsBean
{
    private String _AppId;
    private String _AppType;
    private String _OrgId;
    private String _IosBundleId;
    private String _AndroidBundleId;
    private String _IosCertificate;
    private String _IosCertificatePassword;
    private String _AndroidServerKey;

    public String getAppId()
    {
        return _AppId;
    }

    public void setAppId(String appId)
    {
        _AppId = appId;
    }

    public String getAppType()
    {
        return _AppType;
    }

    public void setAppType(String appType)
    {
        _AppType = appType;
    }

    public String getOrgId()
    {
        return _OrgId;
    }

    public void setOrgId(String orgId)
    {
        _OrgId = orgId;
    }

    public String getIosBundleId()
    {
        return _IosBundleId;
    }

    public void setIosBundleId(String iosBundleId)
    {
        _IosBundleId = iosBundleId;
    }

    public String getAndroidBundleId()
    {
        return _AndroidBundleId;
    }

    public void setAndroidBundleId(String androidBundleId)
    {
        _AndroidBundleId = androidBundleId;
    }

    public String getIosCertificate()
    {
        return _IosCertificate;
    }

    public void setIosCertificate(String iosCertificate)
    {
        _IosCertificate = iosCertificate;
    }

    public String getIosCertificatePassword()
    {
        return _IosCertificatePassword;
    }

    public void setIosCertificatePassword(String iosCertificatePassword)
    {
        _IosCertificatePassword = iosCertificatePassword;
    }

    public String getAndroidServerKey()
    {
        return _AndroidServerKey;
    }

    public void setAndroidServerKey(String androidServerKey)
    {
        _AndroidServerKey = androidServerKey;
    }
}