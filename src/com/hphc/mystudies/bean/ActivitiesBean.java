/*
 * Copyright © 2017-2018 Harvard Pilgrim Health Care Institute (HPHCI) and its Contributors.
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * Funding Source: Food and Drug Administration ("Funding Agency") effective 18 September 2014 as Contract no.
 * HHSF22320140030I/HHSF22301006T (the "Prime Contract").
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package com.hphc.mystudies.bean;

import java.util.List;

/**
 * Created by Ravinder on 2/7/2017.
 */
public class ActivitiesBean
{
    private String _activityId = "";
    private String _studyId = "";
    private String _activityVersion = "";
    private String _status = "";
    private Boolean _bookmarked;
    private String _activityRunId = "";
    private String _activityState;
    private String _anchorDateVersion;
    private String _anchorDatecreatedDate;
    private String _activityStartDate;
    private String _activityEndDate;
    private String _lastModifiedDate;
    private List<CustomScheduleRunsBean> _customScheduleRuns;
    private ActivityRunBean _activityRun;

    public String getAnchorDatecreatedDate()
    {
        return _anchorDatecreatedDate;
    }

    public void setAnchorDatecreatedDate(String anchorDatecreatedDate)
    {
        _anchorDatecreatedDate = anchorDatecreatedDate;
    }

    public String getActivityStartDate()
    {
        return _activityStartDate;
    }

    public void setActivityStartDate(String activityStartDate)
    {
        _activityStartDate = activityStartDate;
    }

    public String getActivityEndDate()
    {
        return _activityEndDate;
    }

    public void setActivityEndDate(String activityEndDate)
    {
        _activityEndDate = activityEndDate;
    }

    public String getLastModifiedDate()
    {
        return _lastModifiedDate;
    }

    public void setLastModifiedDate(String lastModifiedDate)
    {
        _lastModifiedDate = lastModifiedDate;
    }

    public List<CustomScheduleRunsBean> getCustomScheduleRuns()
    {
        return _customScheduleRuns;
    }

    public void setCustomScheduleRuns(List<CustomScheduleRunsBean> customScheduleRuns)
    {
        _customScheduleRuns = customScheduleRuns;
    }

    public String getAnchorDateVersion()
    {
        return _anchorDateVersion;
    }

    public void setAnchorDateVersion(String anchorDateVersion)
    {
        _anchorDateVersion = anchorDateVersion;
    }

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
