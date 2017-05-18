package org.labkey.fdahpuserregws.model;

import org.labkey.api.data.Entity;

import java.util.Date;

/**
 * Created by Ravinder on 1/31/2017.
 */
public class AuthInfo extends Entity
{
    private int _AuthId;
    private String _ParticipantId;
    private String _DeviceToken;
    private String _DeviceType;
    private Date _CreatedOn;
    private Date _ModifiedOn;
    private String _AuthKey;
    private String _IosAppVersion;
    private String _AndroidAppVersion;

    private Date _SessionExpiredDate;


    public int getAuthId()
    {
        return _AuthId;
    }

    public void setAuthId(int authId)
    {
        _AuthId = authId;
    }

    public String getParticipantId()
    {
        return _ParticipantId;
    }

    public void setParticipantId(String participantId)
    {
        _ParticipantId = participantId;
    }

    public String getDeviceToken()
    {
        return _DeviceToken;
    }

    public void setDeviceToken(String deviceToken)
    {
        _DeviceToken = deviceToken;
    }

    public String getDeviceType()
    {
        return _DeviceType;
    }

    public void setDeviceType(String deviceType)
    {
        _DeviceType = deviceType;
    }

    public Date getCreatedOn()
    {
        return _CreatedOn;
    }

    public void setCreatedOn(Date createdOn)
    {
        _CreatedOn = createdOn;
    }

    public Date getModifiedOn()
    {
        return _ModifiedOn;
    }

    public void setModifiedOn(Date modifiedOn)
    {
        _ModifiedOn = modifiedOn;
    }

    public String getAuthKey()
    {
        return _AuthKey;
    }

    public void setAuthKey(String authKey)
    {
        _AuthKey = authKey;
    }

    public String getIosAppVersion()
    {
        return _IosAppVersion;
    }

    public void setIosAppVersion(String iosAppVersion)
    {
        _IosAppVersion = iosAppVersion;
    }

    public String getAndroidAppVersion()
    {
        return _AndroidAppVersion;
    }

    public void setAndroidAppVersion(String androidAppVersion)
    {
        _AndroidAppVersion = androidAppVersion;
    }

    public Date getSessionExpiredDate()
    {
        return _SessionExpiredDate;
    }

    public void setSessionExpiredDate(Date sessionExpiredDate)
    {
        _SessionExpiredDate = sessionExpiredDate;
    }


}
