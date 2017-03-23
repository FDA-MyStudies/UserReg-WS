package org.labkey.fdahpuserregws.model;

import org.labkey.api.data.Entity;

/**
 * Created by Ravinder on 1/31/2017.
 */
public class ParticipantStudies extends Entity
{
    private Integer _Id;
    private String _StudyId;
    private String _Status;
    private Boolean _Bookmark=false;
    private Boolean _Eligbibility=false;
    private Boolean _ConsentStatus=false;
    private String _EnrolledDate;
    private String _ParticipantId;
    private String _UserId;

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

    public String getStatus()
    {
        return _Status;
    }

    public void setStatus(String status)
    {
        _Status = status;
    }

    public Boolean getBookmark()
    {
        return _Bookmark;
    }

    public void setBookmark(Boolean bookmark)
    {
        _Bookmark = bookmark;
    }

    public Boolean getEligbibility()
    {
        return _Eligbibility;
    }

    public void setEligbibility(Boolean eligbibility)
    {
        _Eligbibility = eligbibility;
    }

    public Boolean getConsentStatus()
    {
        return _ConsentStatus;
    }

    public void setConsentStatus(Boolean consentStatus)
    {
        _ConsentStatus = consentStatus;
    }

    public String getEnrolledDate()
    {
        return _EnrolledDate;
    }

    public void setEnrolledDate(String enrolledDate)
    {
        _EnrolledDate = enrolledDate;
    }

    public String getParticipantId()
    {
        return _ParticipantId;
    }

    public void setParticipantId(String participantId)
    {
        _ParticipantId = participantId;
    }

    public String getUserId()
    {
        return _UserId;
    }

    public void setUserId(String userId)
    {
        _UserId = userId;
    }
}
