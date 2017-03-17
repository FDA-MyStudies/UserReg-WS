package org.labkey.fdahpuserregws.model;

/**
 * Created by Ravinder on 3/15/2017.
 */
import org.labkey.api.data.Entity;
import java.util.Date;
public class PasswordHistory extends Entity
{
    private Integer _Id;
    private String _Password;
    private String _UserId;

    public Integer getId()
    {
        return _Id;
    }

    public void setId(Integer id)
    {
        _Id = id;
    }

    public String getPassword()
    {
        return _Password;
    }

    public void setPassword(String password)
    {
        _Password = password;
    }

    public String getUserId()
    {
        return _UserId;
    }

    public void setUserId(String userId)
    {
        _UserId = userId;
    }

    @Override
    public Date getCreated()
    {
        return super.getCreated();
    }

    @Override
    public void setCreated(Date created)
    {
        super.setCreated(created);
    }
}
