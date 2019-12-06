package com.hphc.mystudies.bean;

public class CustomScheduleRunsBean
{
    private String _StudyId;
    private String _ActivityId;
    private String _runStartDate;
    private String _runEndDate;

    public String getRunStartDate()
    {
        return _runStartDate;
    }

    public void setRunStartDate(String runStartDate)
    {
        _runStartDate = runStartDate;
    }

    public String getRunEndDate()
    {
        return _runEndDate;
    }

    public void setRunEndDate(String runEndDate)
    {
        _runEndDate = runEndDate;
    }

    public String getStudyId()
    {
        return _StudyId;
    }

    public void setStudyId(String studyId)
    {
        _StudyId = studyId;
    }

    public String getActivityId()
    {
        return _ActivityId;
    }

    public void setActivityId(String activityId)
    {
        _ActivityId = activityId;
    }
}
