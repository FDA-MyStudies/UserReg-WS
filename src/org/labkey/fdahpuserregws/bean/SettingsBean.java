package org.labkey.fdahpuserregws.bean;

/**
 * Created by Ravinder on 2/3/2017.
 */
public class SettingsBean
{
    private Boolean _remoteNotifications=false;
    private Boolean _localNotifications=false;
    private Boolean _touchId=false;
    private Boolean _passcode=false;
    private String _remindersTime="";
    private String _locale="";

    public Boolean getRemoteNotifications()
    {
        return _remoteNotifications;
    }

    public void setRemoteNotifications(Boolean remoteNotifications)
    {
        _remoteNotifications = remoteNotifications;
    }

    public Boolean getLocalNotifications()
    {
        return _localNotifications;
    }

    public void setLocalNotifications(Boolean localNotifications)
    {
        _localNotifications = localNotifications;
    }

    public Boolean getTouchId()
    {
        return _touchId;
    }

    public void setTouchId(Boolean touchId)
    {
        _touchId = touchId;
    }

    public Boolean getPasscode()
    {
        return _passcode;
    }

    public void setPasscode(Boolean passcode)
    {
        _passcode = passcode;
    }

    public String getRemindersTime()
    {
        return _remindersTime;
    }

    public void setRemindersTime(String remindersTime)
    {
        _remindersTime = remindersTime;
    }

    public String getLocale()
    {
        return _locale;
    }

    public void setLocale(String locale)
    {
        _locale = locale;
    }
}
