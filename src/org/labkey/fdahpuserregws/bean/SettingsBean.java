package org.labkey.fdahpuserregws.bean;

/**
 * Created by Ravinder on 2/3/2017.
 */
public class SettingsBean
{
    private Boolean _remoteNotifications;
    private Boolean _localNotifications;
    private Boolean _touchId;
    private Boolean _passcode;
    private String _reminderLeadTime;
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

    public String getReminderLeadTime()
    {
        return _reminderLeadTime;
    }

    public void setReminderLeadTime(String reminderLeadTime)
    {
        _reminderLeadTime = reminderLeadTime;
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
