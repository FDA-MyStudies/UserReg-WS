package org.labkey.fdahpuserregws.model;

/**
 * Created by Ravinder on 3/13/2017.
 */
import org.labkey.api.data.Entity;
public class StudyConsent extends Entity
{
    private Integer _Id;
    private Integer _UserId;
    private Integer _StudyId;
    private String _Status;
    private String _Version;
    private String _Pdf;

    public Integer getId()
    {
        return _Id;
    }

    public void setId(Integer id)
    {
        _Id = id;
    }

    public Integer getUserId()
    {
        return _UserId;
    }

    public void setUserId(Integer userId)
    {
        _UserId = userId;
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

    public String getVersion()
    {
        return _Version;
    }

    public void setVersion(String version)
    {
        _Version = version;
    }

    public String getPdf()
    {
        return _Pdf;
    }

    public void setPdf(String pdf)
    {
        _Pdf = pdf;
    }
}
