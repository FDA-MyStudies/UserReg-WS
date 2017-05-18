package org.labkey.fdahpuserregws.model;

import java.util.Date;
import org.labkey.api.data.Entity;

/**
 * Created by Ravinder on 5/18/2017.
 */
public class LoginAttempts extends Entity
{
    private Integer _Id;
    private String _Email;
    private Date _LastModified;
    private Integer _Attempts;

    public Integer getId()
    {
        return _Id;
    }

    public void setId(Integer id)
    {
        _Id = id;
    }

    public String getEmail()
    {
        return _Email;
    }

    public void setEmail(String email)
    {
        _Email = email;
    }

    public Date getLastModified()
    {
        return _LastModified;
    }

    public void setLastModified(Date lastModified)
    {
        _LastModified = lastModified;
    }

    public Integer getAttempts()
    {
        return _Attempts;
    }

    public void setAttempts(Integer attempts)
    {
        _Attempts = attempts;
    }
}
