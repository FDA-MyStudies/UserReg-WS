package com.hphc.remoteapi.registration;

import com.hphc.remoteapi.registration.params.AppPropertiesDetails;
import org.labkey.remoteapi.CommandResponse;

public class AppPropertiesUpdateCommand extends FdahpUserRegWSPostCommand<CommandResponse>
{
    public AppPropertiesUpdateCommand(AppPropertiesDetails appProperties)
    {
        super("appPropertiesUpdate", null, null);
        setJsonObject(appProperties.toJSONObject());
    }
}
