package org.labkey.fdahpuserregws.bean;

/**
 * Created by Ravinder on 2/7/2017.
 */
public class StudiesBean
{
    private String _studyId="";
    private String _status="";
    private Boolean bookmarked;
    private String _enrolledDate="";
    private Integer _completion;
    private Integer _adherence;
    private String _participantId;

    public String getStudyId()
    {
        return _studyId;
    }

    public void setStudyId(String studyId)
    {
        _studyId = studyId;
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
        return bookmarked;
    }

    public void setBookmarked(Boolean bookmarked)
    {
        this.bookmarked = bookmarked;
    }

    public String getEnrolledDate()
    {
        return _enrolledDate;
    }

    public void setEnrolledDate(String enrolledDate)
    {
        _enrolledDate = enrolledDate;
    }

    public Integer getCompletion()
    {
        return _completion;
    }

    public void setCompletion(Integer completion)
    {
        _completion = completion;
    }

    public Integer getAdherence()
    {
        return _adherence;
    }

    public void setAdherence(Integer adherence)
    {
        _adherence = adherence;
    }

    public String getParticipantId()
    {
        return _participantId;
    }

    public void setParticipantId(String participantId)
    {
        _participantId = participantId;
    }
}
