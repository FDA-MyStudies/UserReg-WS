package org.labkey.fdahpuserregws.bean;

/**
 * Created by Ravinder on 2/3/2017.
 */
public class ParticipantInfoBean
{
    private String _studyId="";
    private String _appToken="";

    public String getStudyId()
    {
        return _studyId;
    }

    public void setStudyId(String studyId)
    {
        _studyId = studyId;
    }

    public String getAppToken()
    {
        return _appToken;
    }

    public void setAppToken(String appToken)
    {
        _appToken = appToken;
    }
}
