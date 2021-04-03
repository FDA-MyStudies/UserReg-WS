package com.hphc.remoteapi.registration;

import com.hphc.remoteapi.registration.params.AppPropertiesDetails;

public class AppPropertiesUpdateCommand extends FdahpUserRegWSPostCommand
{
    public AppPropertiesUpdateCommand(AppPropertiesDetails appProperties)
    {
        super("appPropertiesUpdate");
        setJsonObject(appProperties.toJSONObject());
    }
}
