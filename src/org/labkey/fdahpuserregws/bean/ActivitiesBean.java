package org.labkey.fdahpuserregws.bean;

/**
 * Created by Ravinder on 2/7/2017.
 */
public class ActivitiesBean
{
    private String _activityId="";
    private String _studyId="";
    private String _activityVersion="";
    private String _status="";
    private Boolean _bookmarked;
    private String _activityRunId="";
    private String  _activityState;
    private ActivityRunBean _activityRun;

    public String getActivityId()
    {
        return _activityId;
    }

    public void setActivityId(String activityId)
    {
        _activityId = activityId;
    }

    public String getStudyId()
    {
        return _studyId;
    }

    public void setStudyId(String studyId)
    {
        _studyId = studyId;
    }

    public String getActivityVersion()
    {
        return _activityVersion;
    }

    public void setActivityVersion(String activityVersion)
    {
        _activityVersion = activityVersion;
    }

    public String getStatus()
    {
        return _status;
    }

    public void setStatus(String status)
    {
        _status = status;
    }

    public Boolean getBookmarked()
    {
        return _bookmarked;
    }

    public void setBookmarked(Boolean bookmarked)
    {
        _bookmarked = bookmarked;
    }

    public String getActivityRunId()
    {
        return _activityRunId;
    }

    public void setActivityRunId(String activityRunId)
    {
        _activityRunId = activityRunId;
    }

    public ActivityRunBean getActivityRun()
    {
        return _activityRun;
    }

    public void setActivityRun(ActivityRunBean activityRun)
    {
        _activityRun = activityRun;
    }

    public String getActivityState()
    {
        return _activityState;
    }

    public void setActivityState(String activityState)
    {
        _activityState = activityState;
    }
}
