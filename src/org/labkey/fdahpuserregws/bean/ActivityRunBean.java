package org.labkey.fdahpuserregws.bean;

/**
 * Created by Ravinder on 4/27/2017.
 */
public class ActivityRunBean
{
    private Integer _total;
    private Integer _completed;
    private Integer _missed;

    public Integer getTotal()
    {
        return _total;
    }

    public void setTotal(Integer total)
    {
        _total = total;
    }

    public Integer getCompleted()
    {
        return _completed;
    }

    public void setCompleted(Integer completed)
    {
        _completed = completed;
    }

    public Integer getMissed()
    {
        return _missed;
    }

    public void setMissed(Integer missed)
    {
        _missed = missed;
    }
}
