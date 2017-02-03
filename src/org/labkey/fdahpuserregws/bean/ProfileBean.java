package org.labkey.fdahpuserregws.bean;

/**
 * Created by Ravinder on 2/3/2017.
 */
public class ProfileBean
{
    private String _firstName;
    private String _lastName;
    private String _emailId;

    public String getFirstName()
    {
        return _firstName;
    }

    public void setFirstName(String firstName)
    {
        _firstName = firstName;
    }

    public String getLastName()
    {
        return _lastName;
    }

    public void setLastName(String lastName)
    {
        _lastName = lastName;
    }

    public String getEmailId()
    {
        return _emailId;
    }

    public void setEmailId(String emailId)
    {
        _emailId = emailId;
    }
}
