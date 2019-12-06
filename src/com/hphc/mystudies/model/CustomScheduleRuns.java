package com.hphc.mystudies.model;

import org.labkey.api.data.Entity;

public class CustomScheduleRuns extends Entity
{
    private Integer _Id;
    private String _StudyId;
    private String _ActivityId;
    private String _ApplicationId;
    private String _OrgId;
    private String _RunStartDate;
    private String _RunEndDate;

    public String getApplicationId()
    {
        return _ApplicationId;
    }

    public void setApplicationId(String applicationId)
    {
        _ApplicationId = applicationId;
    }

    public String getOrgId()
    {
        return _OrgId;
    }

    public void setOrgId(String orgId)
    {
        _OrgId = orgId;
    }

    public Integer getId()
    {
        return _Id;
    }

    public void setId(Integer id)
    {
        _Id = id;
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

    public String getRunStartDate()
    {
        return _RunStartDate;
    }

    public void setRunStartDate(String runStartDate)
    {
        _RunStartDate = runStartDate;
    }

    public String getRunEndDate()
    {
        return _RunEndDate;
    }

    public void setRunEndDate(String runEndDate)
    {
        _RunEndDate = runEndDate;
    }
}
