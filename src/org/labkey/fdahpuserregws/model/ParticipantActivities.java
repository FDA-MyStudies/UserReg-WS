package org.labkey.fdahpuserregws.model;

import org.labkey.api.data.Entity;

/**
 * Created by Ravinder on 1/31/2017.
 */
public class ParticipantActivities extends Entity
{
    private Integer _Id;
    private String _ParticipantId;
    private String _StudyId;
    private String _ActivityId;
    private Integer _ActivityCompleteId;
    private String _ActivityType;
    private Boolean _Bookmark=false;
    private String _Status;
    private String _ActivityVersion;
    private String _ActivityState;
    private String _ActivityRunId;

    private Integer _Total;
    private Integer _Completed;
    private Integer _Missed;

    public Integer getId()
    {
        return _Id;
    }

    public void setId(Integer id)
    {
        _Id = id;
    }

    public String getParticipantId()
    {
        return _ParticipantId;
    }

    public void setParticipantId(String participantId)
    {
        _ParticipantId = participantId;
    }

    public String getActivityId()
    {
        return _ActivityId;
    }

    public void setActivityId(String activityId)
    {
        _ActivityId = activityId;
    }

    public Integer getActivityCompleteId()
    {
        return _ActivityCompleteId;
    }

    public void setActivityCompleteId(Integer activityCompleteId)
    {
        _ActivityCompleteId = activityCompleteId;
    }

    public String getActivityState()
    {
        return _ActivityState;
    }

    public void setActivityState(String activityState)
    {
        _ActivityState = activityState;
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

    public String getStudyId()
    {
        return _StudyId;
    }

    public void setStudyId(String studyId)
    {
        _StudyId = studyId;
    }

    public String getActivityRunId()
    {
        return _ActivityRunId;
    }

    public void setActivityRunId(String activityRunId)
    {
        _ActivityRunId = activityRunId;
    }

    public Integer getTotal()
    {
        return _Total;
    }

    public void setTotal(Integer total)
    {
        _Total = total;
    }

    public Integer getCompleted()
    {
        return _Completed;
    }

    public void setCompleted(Integer completed)
    {
        _Completed = completed;
    }

    public Integer getMissed()
    {
        return _Missed;
    }

    public void setMissed(Integer missed)
    {
        _Missed = missed;
    }
}
