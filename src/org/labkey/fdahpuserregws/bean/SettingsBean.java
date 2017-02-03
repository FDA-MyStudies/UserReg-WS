package org.labkey.fdahpuserregws.bean;

/**
 * Created by Ravinder on 2/3/2017.
 */
public class SettingsBean
{
    private boolean _remoteNotifications;
    private boolean _localNotifications;
    private boolean _touchId;
    private boolean _passcode;

    public boolean isRemoteNotifications()
    {
        return _remoteNotifications;
    }

    public void setRemoteNotifications(boolean remoteNotifications)
    {
        _remoteNotifications = remoteNotifications;
    }

    public boolean isLocalNotifications()
    {
        return _localNotifications;
    }

    public void setLocalNotifications(boolean localNotifications)
    {
        _localNotifications = localNotifications;
    }

    public boolean isTouchId()
    {
        return _touchId;
    }

    public void setTouchId(boolean touchId)
    {
        _touchId = touchId;
    }

    public boolean isPasscode()
    {
        return _passcode;
    }

    public void setPasscode(boolean passcode)
    {
        _passcode = passcode;
    }
}
