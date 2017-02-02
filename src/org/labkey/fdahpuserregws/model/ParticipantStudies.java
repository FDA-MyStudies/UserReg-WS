package org.labkey.fdahpuserregws.model;

import org.labkey.api.data.Entity;

/**
 * Created by Ravinder on 1/31/2017.
 */
public class ParticipantStudies extends Entity
{
    private int _Id;
    private int _ParticipantId;
    private  int _StudyId;
    private boolean _Status;
    private boolean _Bookmark;

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

    public boolean isStatus()
    {
        return _Status;
    }

    public void setStatus(boolean status)
    {
        _Status = status;
    }

    public boolean isBookmark()
    {
        return _Bookmark;
    }

    public void setBookmark(boolean bookmark)
    {
        _Bookmark = bookmark;
    }
}
