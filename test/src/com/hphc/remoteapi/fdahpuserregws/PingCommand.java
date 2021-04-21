package com.hphc.remoteapi.fdahpuserregws;

import org.labkey.remoteapi.CommandResponse;

public class PingCommand extends FdahpUserRegWSCommand<CommandResponse>
{
    public PingCommand()
    {
        super("ping", null, null);
    }
}
