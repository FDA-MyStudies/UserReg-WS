package org.labkey.fdahpuserregws.model;

import org.labkey.api.data.Entity;

/**
 * Created by Ravinder on 1/31/2017.
 */
public class ParticipantStudies extends Entity
{
    private Integer _Id;
    private Integer _ParticipantId;
    private Integer _StudyId;
    private String _Status;
    private Boolean _Bookmark=false;
    private Boolean _Eligbibility;
    private Boolean _ConsentStatus=false;
    private String _Consent;

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

    public String getConsent()
    {
        return _Consent;
    }

    public void setConsent(String consent)
    {
        _Consent = consent;
    }
}
