package org.labkey.test.tests.registration;

import com.hphc.remoteapi.registration.NoCsrfConnection;
import com.hphc.remoteapi.registration.PingCommand;
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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@Category({})
public class MyStudiesRegistrationTest extends BaseWebDriverTest
{
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

    public Connection getRegistrationConnection()
    {
        return new NoCsrfConnection(WebTestHelper.getBaseURL());
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
