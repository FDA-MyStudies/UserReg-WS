package org.labkey.test.tests.registration;

import com.hphc.remoteapi.registration.NoCsrfConnection;
import com.hphc.remoteapi.registration.PingCommand;
import com.hphc.remoteapi.registration.RegisterCommand;
import com.hphc.remoteapi.registration.VerifyCommand;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.util.TestLogger;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.*;

@Category({})
public class MyStudiesRegistrationTest extends BaseWebDriverTest
{
    private static final String APP_ID = "MyApplication";
    private static final String ORG_ID = "MyOrganization";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        MyStudiesRegistrationTest init = (MyStudiesRegistrationTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testPing() throws Exception
    {
        CommandResponse response = new PingCommand().execute(getRegistrationConnection(), null);

        Map<String, Object> parsedData = response.getParsedData();
        assertEquals(true, parsedData.get("success"));

    }

    @Test
    public void testRegisterNewUser() throws IOException, CommandException
    {
        Map.Entry<String, String> emailPassword = generateEmailPassword("email@mystudies.registration.test");
        String email = emailPassword.getKey();
        String password = emailPassword.getValue();

        enableEmailRecorder();

        RegisterCommand registerCommand = new RegisterCommand();
        registerCommand.setOrgId(ORG_ID);
        registerCommand.setApplicationId(APP_ID);
        registerCommand.setParameters(Map.of("emailId", email, "password", password));

        CommandResponse response = executeRegistrationCommand(registerCommand);
        Map<String, Object> parsedData = response.getParsedData();
        assertEquals(true, parsedData.get("success"));

        var code = getVerificationCode();

        var verifyCommand = new VerifyCommand();
        verifyCommand.setOrgId(ORG_ID);
        verifyCommand.setApplicationId(APP_ID);
        verifyCommand.setParameters(Map.of("emailId", email, "code", code));

        response = verifyCommand.execute(getRegistrationConnection(), null);
        parsedData = response.getParsedData();
        assertEquals(true, parsedData.get("success"));

    }

    private CommandResponse executeRegistrationCommand(RegisterCommand registerCommand) throws IOException, CommandException
    {
        try
        {
            return registerCommand.execute(getRegistrationConnection(), null);
        }
        catch (CommandException e)
        {
            TestLogger.error(e.getResponseText());
            throw e;
        }
    }

    protected Connection getRegistrationConnection()
    {
        return new NoCsrfConnection(WebTestHelper.getBaseURL());
    }

    protected String getVerificationCode()
    {
        EmailRecordTable emailRecordTable = goToEmailRecord();
        return null;
    }

    String randString()
    {
        StringBuilder s = new StringBuilder();
        while (s.length() < 6)
        {
            int r = WebTestHelper.RANDOM.nextInt(54);
            int ch;
            if (r < 10)
                ch = 48 + r;
            else
            {
                r -= 10;
                if (r < 26)
                    ch = 65 + r;
                else
                {
                    r -= 26;
                    ch = 97 + r;
                }
            }
            s.append((char)ch);
        }
        return s.toString();
    }

    Map.Entry<String,String> generateEmailPassword(String baseEmail)
    {
        int at = baseEmail.indexOf('@');
        String name = baseEmail.substring(0,at);
        String domain = baseEmail.substring(at);
        if (-1 != name.indexOf('+'))
            name = name.substring(0,name.indexOf('+'));
        return new AbstractMap.SimpleEntry<>(
                name + "+" + randString().toLowerCase() + domain,
                randString()
        );
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "MyStudiesRegistrationTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
