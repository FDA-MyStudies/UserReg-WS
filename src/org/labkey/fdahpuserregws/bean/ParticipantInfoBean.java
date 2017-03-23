package org.labkey.fdahpuserregws.bean;

/**
 * Created by Ravinder on 2/3/2017.
 */
public class ParticipantInfoBean
{
    private String _studyId="";
    private String _participantId="";
    private String _enrolledDate="";

    public String getStudyId()
    {
        return _studyId;
    }

    public void setStudyId(String studyId)
    {
        _studyId = studyId;
    }

    public String getParticipantId()
    {
        return _participantId;
    }

    public void setParticipantId(String participantId)
    {
        _participantId = participantId;
    }

    public String getEnrolledDate()
    {
        return _enrolledDate;
    }

    public void setEnrolledDate(String enrolledDate)
    {
        _enrolledDate = enrolledDate;
    }
}
