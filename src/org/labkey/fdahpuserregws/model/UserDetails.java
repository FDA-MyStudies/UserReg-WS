package org.labkey.fdahpuserregws.model;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.Entity;
import org.labkey.api.util.GUID;

import java.util.Date;

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
    private String _UserId;
    private Boolean _TempPassword=false;
    private String _Locale;
    private String _ResetPassword;

    private Date _VerificationDate;
    private Date _TempPasswordDate;

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

    public String getUserId()
    {
        return _UserId;
    }

    public void setUserId(String userId)
    {
        _UserId = userId;
    }

    public Boolean getTempPassword()
    {
        return _TempPassword;
    }

    public void setTempPassword(Boolean tempPassword)
    {
        _TempPassword = tempPassword;
    }

    public String getLocale()
    {
        return _Locale;
    }

    public void setLocale(String locale)
    {
        _Locale = locale;
    }

    public Date getVerificationDate()
    {
        return _VerificationDate;
    }

    public void setVerificationDate(Date verificationDate)
    {
        _VerificationDate = verificationDate;
    }

    public Date getTempPasswordDate()
    {
        return _TempPasswordDate;
    }

    public void setTempPasswordDate(Date tempPasswordDate)
    {
        _TempPasswordDate = tempPasswordDate;
    }

    public String getResetPassword()
    {
        return _ResetPassword;
    }

    public void setResetPassword(String resetPassword)
    {
        _ResetPassword = resetPassword;
    }
}
