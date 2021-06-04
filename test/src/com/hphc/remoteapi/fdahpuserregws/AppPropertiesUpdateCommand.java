package com.hphc.remoteapi.fdahpuserregws;

import com.hphc.remoteapi.fdahpuserregws.params.AppPropertiesDetails;
import org.labkey.remoteapi.CommandResponse;

public class AppPropertiesUpdateCommand extends BaseRegistrationCommand<CommandResponse>
{
    public AppPropertiesUpdateCommand(AppPropertiesDetails appProperties)
    {
        super("appPropertiesUpdate", null, null);
        setJsonObject(appProperties.toJSONObject());
    }

    @Override
    protected String getRequestType()
    {
        return "POST";
    }
}
