package org.labkey.fdahpuserregws.bean;

import org.labkey.api.action.ReturnUrlForm;

/**
 * Created by Ravinder on 2/2/2017.
 */
public class ParticipantForm extends ReturnUrlForm
{
    private Integer _userId;
    private String _firstName;
    private String _lastName;
    private String _email;
    private String _password;
    private Boolean _usePassCode;
    private Boolean _touchId;
    private Boolean _localNotification;
    private Boolean _remoteNotification;
    private Boolean _reminder_flag;
    private String _auth;
    private Integer _status;

    public Integer getUserId()
    {
        return _userId;
    }

    public void setUserId(Integer userId)
    {
        _userId = userId;
    }

    public String getFirstName()
    {
        return _firstName;
    }

    public void setFirstName(String firstName)
    {
        _firstName = firstName;
    }

    public String getLastName()
    {
        return _lastName;
    }

    public void setLastName(String lastName)
    {
        _lastName = lastName;
    }

    public String getEmail()
    {
        return _email;
    }

    public void setEmail(String email)
    {
        _email = email;
    }

    public String getPassword()
    {
        return _password;
    }

    public void setPassword(String password)
    {
        _password = password;
    }

    public Boolean getUsePassCode()
    {
        return _usePassCode;
    }

    public void setUsePassCode(Boolean usePassCode)
    {
        _usePassCode = usePassCode;
    }

    public Boolean getTouchId()
    {
        return _touchId;
    }

    public void setTouchId(Boolean touchId)
    {
        _touchId = touchId;
    }

    public Boolean getLocalNotification()
    {
        return _localNotification;
    }

    public void setLocalNotification(Boolean localNotification)
    {
        _localNotification = localNotification;
    }

    public Boolean getRemoteNotification()
    {
        return _remoteNotification;
    }

    public void setRemoteNotification(Boolean remoteNotification)
    {
        _remoteNotification = remoteNotification;
    }

    public Boolean getReminder_flag()
    {
        return _reminder_flag;
    }

    public void setReminder_flag(Boolean reminder_flag)
    {
        _reminder_flag = reminder_flag;
    }

    public String getAuth()
    {
        return _auth;
    }

    public void setAuth(String auth)
    {
        _auth = auth;
    }

    public Integer getStatus()
    {
        return _status;
    }

    public void setStatus(Integer status)
    {
        _status = status;
    }
}
