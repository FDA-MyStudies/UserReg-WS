package com.hphc.remoteapi.fdahpuserregws;

import org.labkey.remoteapi.CommandResponse;

public class PingCommand extends BaseRegistrationCommand<CommandResponse>
{
    public PingCommand()
    {
        super("ping", null, null);
    }
}
