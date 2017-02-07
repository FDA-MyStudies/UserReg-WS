package org.labkey.fdahpuserregws.model;

import org.labkey.api.data.Entity;

/**
 * Created by Ravinder on 1/31/2017.
 */
public class ParticipantActivities extends Entity
{
    private Integer _Id;
    private Integer _ParticipantId;
    private Integer _StudyId;
    private Integer _ActivityId;
    private Integer _ActivityComplete;
    private String _ActivityType;
    private Boolean _Bookmark=false;
    private String _Status;
    private String _ActivityVersion;

    public Integer getId()
    {
        return _Id;
    }

    public void setId(Integer id)
    {
        _Id = id;
    }

    public Integer getParticipantId()
    {
        return _ParticipantId;
    }

    public void setParticipantId(Integer participantId)
    {
        _ParticipantId = participantId;
    }

    public Integer getStudyId()
    {
        return _StudyId;
    }

    public void setStudyId(Integer studyId)
    {
        _StudyId = studyId;
    }

    public Integer getActivityId()
    {
        return _ActivityId;
    }

    public void setActivityId(Integer activityId)
    {
        _ActivityId = activityId;
    }

    public Integer getActivityComplete()
    {
        return _ActivityComplete;
    }

    public void setActivityComplete(Integer activityComplete)
    {
        _ActivityComplete = activityComplete;
    }

    public String getActivityType()
    {
        return _ActivityType;
    }

    public void setActivityType(String activityType)
    {
        _ActivityType = activityType;
    }

    public Boolean getBookmark()
    {
        return _Bookmark;
    }

    public void setBookmark(Boolean bookmark)
    {
        _Bookmark = bookmark;
    }

    public String getStatus()
    {
        return _Status;
    }

    public void setStatus(String status)
    {
        _Status = status;
    }

    public String getActivityVersion()
    {
        return _ActivityVersion;
    }

    public void setActivityVersion(String activityVersion)
    {
        _ActivityVersion = activityVersion;
    }
}
