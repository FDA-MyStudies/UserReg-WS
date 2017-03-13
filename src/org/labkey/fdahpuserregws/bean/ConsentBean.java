package org.labkey.fdahpuserregws.bean;

/**
 * Created by Ravinder on 3/13/2017.
 */
public class ConsentBean
{
    private String _version;
    private String _status;
    private  String _pdf;

    public String getVersion()
    {
        return _version;
    }

    public void setVersion(String version)
    {
        _version = version;
    }

    public String getStatus()
    {
        return _status;
    }

    public void setStatus(String status)
    {
        _status = status;
    }

    public String getPdf()
    {
        return _pdf;
    }

    public void setPdf(String pdf)
    {
        _pdf = pdf;
    }
}
