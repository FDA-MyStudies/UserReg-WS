package com.hphc.remoteapi.registration;

import org.labkey.remoteapi.CommandResponse;

public class PingCommand extends FdahpUserRegWSCommand<CommandResponse>
{
    public PingCommand()
    {
        super("ping", null, null);
    }
}
