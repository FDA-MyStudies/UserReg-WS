package org.labkey.fdahpuserregws.bean;

import org.json.JSONArray;

/**
 * Created by Ravinder on 5/3/2017.
 */
public class NotificationBean
{
    private String _studyId;
    private String _customStudyId;
    private String _notificationText;
    private String _notificationTitle;
    private String _notificationType;
    private String _notificationSubType;
    private Integer _notificationId;

    private JSONArray _deviceToken;

    public String getStudyId()
    {
        return _studyId;
    }

    public void setStudyId(String studyId)
    {
        _studyId = studyId;
    }

    public String getCustomStudyId()
    {
        return _customStudyId;
    }

    public void setCustomStudyId(String customStudyId)
    {
        _customStudyId = customStudyId;
    }

    public String getNotificationText()
    {
        return _notificationText;
    }

    public void setNotificationText(String notificationText)
    {
        _notificationText = notificationText;
    }

    public String getNotificationTitle()
    {
        return _notificationTitle;
    }

    public void setNotificationTitle(String notificationTitle)
    {
        _notificationTitle = notificationTitle;
    }

    public String getNotificationType()
    {
        return _notificationType;
    }

    public void setNotificationType(String notificationType)
    {
        _notificationType = notificationType;
    }

    public String getNotificationSubType()
    {
        return _notificationSubType;
    }

    public void setNotificationSubType(String notificationSubType)
    {
        _notificationSubType = notificationSubType;
    }

    public JSONArray getDeviceToken()
    {
        return _deviceToken;
    }

    public void setDeviceToken(JSONArray deviceToken)
    {
        _deviceToken = deviceToken;
    }

    public Integer getNotificationId()
    {
        return _notificationId;
    }

    public void setNotificationId(Integer notificationId)
    {
        _notificationId = notificationId;
    }
}
