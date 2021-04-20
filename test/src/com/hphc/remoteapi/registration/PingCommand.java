package com.hphc.remoteapi.registration;

import org.labkey.remoteapi.CommandResponse;

public class PingCommand extends RegistrationCommand<CommandResponse>
{
    public PingCommand()
    {
        super("ping", null, null);
    }
}
