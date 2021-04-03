package com.hphc.remoteapi.registration.params;

import org.json.simple.JSONObject;

public class AppPropertiesDetails
{
//    private int _Id;
    private String _AppId;
    private String _OrgId;
//    private String _IosBundleId;
//    private String _AndroidBundleId;
//    private String _IosCertificate;
//    private String _IosCertificatePassword;
//    private String _AndroidServerKey;
    private String _Email;
//    private String _EmailPassword;
    private String _RegEmailSub;
    private String _RegEmailBody;
    private String _ForgotEmailSub;
    private String _ForgotEmailBody;
    private String _FeedbackEmail;
    private String _ContactUsEmail;
    private String _AppName;
    private boolean _MethodHandler = false;

    public JSONObject toJSONObject()
    {
        JSONObject json = new JSONObject();
        json.put("AppId", _AppId);
        json.put("OrgId", _OrgId);
        json.put("Email", _Email);
        json.put("RegEmailSub", _RegEmailSub);
        json.put("RegEmailBody", _RegEmailBody);
        json.put("ForgotEmailSub", _ForgotEmailSub);
        json.put("ForgotEmailBody", _ForgotEmailBody);
        json.put("FeedbackEmail", _FeedbackEmail);
        json.put("ContactUsEmail", _ContactUsEmail);
        json.put("AppName", _AppName);
        return json;
    }

    public String getFeedbackEmail()
    {
        return _FeedbackEmail;
    }

    public void setFeedbackEmail(String feedbackEmail)
    {
        _FeedbackEmail = feedbackEmail;
    }

    public String getContactUsEmail()
    {
        return _ContactUsEmail;
    }

    public void setContactUsEmail(String contactUsEmail)
    {
        _ContactUsEmail = contactUsEmail;
    }

    public String getAppName()
    {
        return _AppName;
    }

    public void setAppName(String appName)
    {
        _AppName = appName;
    }

    public boolean isMethodHandler()
    {
        return _MethodHandler;
    }

    public void setMethodHandler(boolean methodHandler)
    {
        _MethodHandler = methodHandler;
    }

    public String getEmail()
    {
        return _Email;
    }

    public void setEmail(String email)
    {
        _Email = email;
    }

//    public String getEmailPassword()
//    {
//        return _EmailPassword;
//    }
//
//    public void setEmailPassword(String emailPassword)
//    {
//        _EmailPassword = emailPassword;
//    }
//
//    public int getId()
//    {
//        return _Id;
//    }
//
//    public void setId(int id)
//    {
//        _Id = id;
//    }

    public String getAppId()
    {
        return _AppId;
    }

    public void setAppId(String appId)
    {
        _AppId = appId;
    }

    public String getOrgId()
    {
        return _OrgId;
    }

    public void setOrgId(String orgId)
    {
        _OrgId = orgId;
    }

//    public String getIosBundleId()
//    {
//        return _IosBundleId;
//    }
//
//    public void setIosBundleId(String iosBundleId)
//    {
//        _IosBundleId = iosBundleId;
//    }
//
//    public String getAndroidBundleId()
//    {
//        return _AndroidBundleId;
//    }
//
//    public void setAndroidBundleId(String androidBundleId)
//    {
//        _AndroidBundleId = androidBundleId;
//    }
//
//    public String getIosCertificate()
//    {
//        return _IosCertificate;
//    }
//
//    public void setIosCertificate(String iosCertificate)
//    {
//        _IosCertificate = iosCertificate;
//    }
//
//    public String getIosCertificatePassword()
//    {
//        return _IosCertificatePassword;
//    }
//
//    public void setIosCertificatePassword(String iosCertificatePassword)
//    {
//        _IosCertificatePassword = iosCertificatePassword;
//    }
//
//    public String getAndroidServerKey()
//    {
//        return _AndroidServerKey;
//    }
//
//    public void setAndroidServerKey(String androidServerKey)
//    {
//        _AndroidServerKey = androidServerKey;
//    }

    public String getRegEmailBody()
    {
        return _RegEmailBody;
    }

    public void setRegEmailBody(String regEmailBody)
    {
        _RegEmailBody = regEmailBody;
    }

    public String getForgotEmailBody()
    {
        return _ForgotEmailBody;
    }

    public void setForgotEmailBody(String forgotEmailBody)
    {
        _ForgotEmailBody = forgotEmailBody;
    }

    public String getRegEmailSub()
    {
        return _RegEmailSub;
    }

    public void setRegEmailSub(String regEmailSub)
    {
        _RegEmailSub = regEmailSub;
    }

    public String getForgotEmailSub()
    {
        return _ForgotEmailSub;
    }

    public void setForgotEmailSub(String forgotEmailSub)
    {
        _ForgotEmailSub = forgotEmailSub;
    }
}
