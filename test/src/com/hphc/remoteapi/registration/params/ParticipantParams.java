package com.hphc.remoteapi.registration.params;

import org.labkey.api.action.ReturnUrlForm;

import java.util.Date;

/**
 * Test params for 'com.hphc.mystudies.bean.ParticipantForm'
 */
public class ParticipantParams extends ReturnUrlForm
{
    private String _userId;
    private String _firstName;
    private String _lastName;
    private String _emailId;
    private String _password;
    private Boolean _usePassCode;
    private Boolean _touchId;
    private Boolean _localNotification;
    private Boolean _remoteNotification;
    private Boolean _reminderFlag;
    private String _auth;
    private Integer _status;
    private Boolean _tempPassword;
    private Date _tempPasswordDate;


    public String getUserId()
    {
        return _userId;
    }

    public void setUserId(String userId)
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

    public String getEmailId()
    {
        return _emailId;
    }

    public void setEmailId(String emailId)
    {
        _emailId = emailId;
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


    public Boolean getReminderFlag()
    {
        return _reminderFlag;
    }

    public void setReminderFlag(Boolean reminderFlag)
    {
        _reminderFlag = reminderFlag;
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

    public Boolean getTempPassword()
    {
        return _tempPassword;
    }

    public void setTempPassword(Boolean tempPassword)
    {
        _tempPassword = tempPassword;
    }

    public Date getTempPasswordDate()
    {
        return _tempPasswordDate;
    }

    public void setTempPasswordDate(Date tempPasswordDate)
    {
        _tempPasswordDate = tempPasswordDate;
    }
}
