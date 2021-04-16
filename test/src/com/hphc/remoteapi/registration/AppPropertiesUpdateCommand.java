package com.hphc.remoteapi.registration;

import com.hphc.remoteapi.registration.params.AppPropertiesDetails;

public class AppPropertiesUpdateCommand extends FdahpUserRegWSPostCommand
{
    public AppPropertiesUpdateCommand(AppPropertiesDetails appProperties)
    {
        super("appPropertiesUpdate", null, null);
        setJsonObject(appProperties.toJSONObject());
    }
}
