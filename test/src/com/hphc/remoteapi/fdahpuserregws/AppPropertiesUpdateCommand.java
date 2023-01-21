package com.hphc.remoteapi.fdahpuserregws;

import com.hphc.remoteapi.fdahpuserregws.params.AppPropertiesDetails;
import org.json.JSONObject;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;

public class AppPropertiesUpdateCommand extends PostCommand<CommandResponse>
{
    private final JSONObject _json;

    public AppPropertiesUpdateCommand(AppPropertiesDetails appProperties)
    {
        super(BaseRegistrationCommand.CONTROLLER, "appPropertiesUpdate");
        _json = appProperties.toJSONObject();
    }

    @Override
    public JSONObject getJsonObject()
    {
        return _json;
    }
}
