package com.hphc.remoteapi.fdahpuserregws;

import com.hphc.remoteapi.fdahpuserregws.params.AppPropertiesDetails;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;

public class AppPropertiesUpdateCommand extends PostCommand<CommandResponse>
{
    public AppPropertiesUpdateCommand(AppPropertiesDetails appProperties)
    {
        super(BaseRegistrationCommand.CONTROLLER, "appPropertiesUpdate");
        setJsonObject(appProperties.toJSONObject());
    }
}
