package com.hphc.remoteapi.fdahpuserregws;

import com.hphc.remoteapi.fdahpuserregws.params.AppPropertiesDetails;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.CommandResponse;

public class AppPropertiesUpdateCommand extends FdahpUserRegWSCommand<CommandResponse>
{
    private final AppPropertiesDetails _appProperties;

    public AppPropertiesUpdateCommand(AppPropertiesDetails appProperties)
    {
        super("appPropertiesUpdate", null, null);
        _appProperties = appProperties;
    }

    @Override
    public JSONObject getJsonObject()
    {
        return _appProperties.toJSONObject();
    }

    @Override
    protected String getRequestType()
    {
        return "POST";
    }
}
