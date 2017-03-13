package org.labkey.fdahpuserregws.model;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.Entity;

/**
 * Created by Ravinder on 1/31/2017.
 */
public class UserDetails extends Entity
{
    private Integer _Id;
    private String _FirstName;
    private String _LastName;
    private String _Email;
    private Boolean _UsePassCode=false;
    private Boolean _TouchId=false;
    private Boolean _LocalNotificationFlag=false;
    private Boolean _RemoteNotificationFlag=false;
    private Integer Status;
    private String _Password;
    private String _ReminderTime;
    private String _SecurityToken;

    public Integer getId()
    {
        return _Id;
    }

    public void setId(Integer id)
    {
        _Id = id;
    }

    public String getFirstName()
    {
        return _FirstName;
    }

    public void setFirstName(String firstName)
    {
        _FirstName = firstName;
    }

    public String getLastName()
    {
        return _LastName;
    }

    public void setLastName(String lastName)
    {
        _LastName = lastName;
    }

    public String getEmail()
    {
        return _Email;
    }

    public void setEmail(String email)
    {
        _Email = email;
    }

    public Boolean getUsePassCode()
    {
        return _UsePassCode;
    }

    public void setUsePassCode(Boolean usePassCode)
    {
        _UsePassCode = usePassCode;
    }

    public Boolean getTouchId()
    {
        return _TouchId;
    }

    public void setTouchId(Boolean touchId)
    {
        _TouchId = touchId;
    }

    public Boolean getLocalNotificationFlag()
    {
        return _LocalNotificationFlag;
    }

    public void setLocalNotificationFlag(Boolean localNotificationFlag)
    {
        _LocalNotificationFlag = localNotificationFlag;
    }

    public Boolean getRemoteNotificationFlag()
    {
        return _RemoteNotificationFlag;
    }

    public void setRemoteNotificationFlag(Boolean remoteNotificationFlag)
    {
        _RemoteNotificationFlag = remoteNotificationFlag;
    }

    public Integer getStatus()
    {
        return Status;
    }

    public void setStatus(Integer status)
    {
        Status = status;
    }

    public String getPassword()
    {
        return _Password;
    }

    public void setPassword(String password)
    {
        _Password = password;
    }

    public String getReminderTime()
    {
        return _ReminderTime;
    }

    public void setReminderTime(String reminderTime)
    {
        _ReminderTime = reminderTime;
    }

    public String getSecurityToken()
    {
        return _SecurityToken;
    }

    public void setSecurityToken(String securityToken)
    {
        _SecurityToken = securityToken;
    }

    @Nullable
    @Override
    public String getEntityId()
    {
        return super.getEntityId();
    }

}
