package com.hphc.remoteapi.fdahpuserregws.params;

import org.json.simple.JSONObject;

/**
 * Mirrors 'com.hphc.mystudies.bean.AppPropertiesDetailsBean'
 */
public class AppPropertiesDetails
{
    private final String _OrgId;
    private final String _AppId;

//    private String _AppType;
//    private String _IosBundleId;
//    private String _AndroidBundleId;
//    private String _IosCertificate;
//    private String _IosCertificatePassword;
//    private String _AndroidServerKey;
    private String _Email;
//    private String _EmailPassword;
    private String _RegisterEmailSubject;
    private String _RegisterEmailBody;
    private String _ForgotPassEmailSubject;
    private String _ForgotPassEmailBody;
    private String _FeedbackEmail;
    private String _ContactUsEmail;
    private String _AppName;
    private boolean _MethodHandler = false;

    public AppPropertiesDetails(String orgId, String appId)
    {
        _OrgId = orgId;
        _AppId = appId;
    }

    public JSONObject toJSONObject()
    {
        JSONObject json = new JSONObject();
        json.put("AppId", _AppId);
        json.put("OrgId", _OrgId);
        json.put("Email", _Email);
        json.put("RegisterEmailSubject", _RegisterEmailSubject);
        json.put("RegisterEmailBody", _RegisterEmailBody);
        json.put("ForgotPassEmailSubject", _ForgotPassEmailSubject);
        json.put("ForgotPassEmailBody", _ForgotPassEmailBody);
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

    public String getOrgId()
    {
        return _OrgId;
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

    public String getRegisterEmailBody()
    {
        return _RegisterEmailBody;
    }

    public void setRegisterEmailBody(String registerEmailBody)
    {
        _RegisterEmailBody = registerEmailBody;
    }

    public String getForgotPassEmailBody()
    {
        return _ForgotPassEmailBody;
    }

    public void setForgotPassEmailBody(String forgotPassEmailBody)
    {
        _ForgotPassEmailBody = forgotPassEmailBody;
    }

    public String getRegisterEmailSubject()
    {
        return _RegisterEmailSubject;
    }

    public void setRegisterEmailSubject(String registerEmailSubject)
    {
        _RegisterEmailSubject = registerEmailSubject;
    }

    public String getForgotPassEmailSubject()
    {
        return _ForgotPassEmailSubject;
    }

    public void setForgotPassEmailSubject(String forgotPassEmailSubject)
    {
        _ForgotPassEmailSubject = forgotPassEmailSubject;
    }
}
