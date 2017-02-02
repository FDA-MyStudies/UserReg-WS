package org.labkey.fdahpuserregws.model;

import org.labkey.api.data.Entity;

/**
 * Created by Ravinder on 1/31/2017.
 */
public class ParticipantActivities extends Entity
{
    private int _Id;
    private int _ParticipantId;
    private int _StudyId;
    private int _ActivityId;
    private int _ActivityComplete;
    private String _ActivityType;
    private boolean _EligibilityStatus;
    private boolean _ConsentStatus;
    private String _Consent;

    public int getId()
    {
        return _Id;
    }

    public void setId(int id)
    {
        _Id = id;
    }

    public int getParticipantId()
    {
        return _ParticipantId;
    }

    public void setParticipantId(int participantId)
    {
        _ParticipantId = participantId;
    }

    public int getStudyId()
    {
        return _StudyId;
    }

    public void setStudyId(int studyId)
    {
        _StudyId = studyId;
    }

    public int getActivityId()
    {
        return _ActivityId;
    }

    public void setActivityId(int activityId)
    {
        _ActivityId = activityId;
    }

    public int getActivityComplete()
    {
        return _ActivityComplete;
    }

    public void setActivityComplete(int activityComplete)
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

    public boolean isEligibilityStatus()
    {
        return _EligibilityStatus;
    }

    public void setEligibilityStatus(boolean eligibilityStatus)
    {
        _EligibilityStatus = eligibilityStatus;
    }

    public boolean isConsentStatus()
    {
        return _ConsentStatus;
    }

    public void setConsentStatus(boolean consentStatus)
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
