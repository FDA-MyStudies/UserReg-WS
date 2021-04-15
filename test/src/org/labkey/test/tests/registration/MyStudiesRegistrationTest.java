package org.labkey.test.tests.registration;

import com.hphc.remoteapi.registration.AppPropertiesUpdateCommand;
import com.hphc.remoteapi.registration.FdahpUserRegWSCommand;
import com.hphc.remoteapi.registration.NoCsrfConnection;
import com.hphc.remoteapi.registration.PingCommand;
import com.hphc.remoteapi.registration.RegisterCommand;
import com.hphc.remoteapi.registration.VerifyCommand;
import com.hphc.remoteapi.registration.params.AppPropertiesDetails;
import org.apache.http.HttpStatus;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.InsertExternalSchemaCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.ModulePropertyValue;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.TestLogger;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

@Category({})
public class MyStudiesRegistrationTest extends BaseWebDriverTest
{
    private static final String MODULE_NAME = "FdahpUserRegWS";
    private static final String ORG_ID = "MyStudies Test Organization";
    private static final Pattern verificationCodePattern = Pattern.compile("Verification Code:(\\w+)");

    @BeforeClass
    public static void setupProject()
    {
        MyStudiesRegistrationTest init = (MyStudiesRegistrationTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.enableModule("FdahpUserRegWS");
        setModuleProperties(List.of(new ModulePropertyValue(MODULE_NAME, getProjectName(), "StudyId", getProjectName())));
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
        Map.Entry<String, String> emailPassword = generateEmailPassword("regNewUser@mystudies.registration.test");
        String email = emailPassword.getKey();
        String password = emailPassword.getValue();
        String appId = "RegisterUserApplication";
        AppPropertiesDetails appProperties = new AppPropertiesDetails();
        appProperties.setOrgId(ORG_ID);
        appProperties.setAppId(appId);

        enableEmailRecorder();

        createAppFolder(appId, appProperties);

        TestLogger.log("Register new user");
        {
            RegisterCommand registerCommand = new RegisterCommand();
            registerCommand.setOrgId(ORG_ID);
            registerCommand.setApplicationId(appId);
            registerCommand.setParameters(Map.of("emailId", email, "password", password));

            CommandResponse registrationResponse = executeRegistrationCommand(registerCommand);
            Map<String, Object> parsedData = registrationResponse.getParsedData();
            assertEquals("success", parsedData.get("message"));
        }

        TestLogger.log("Attempt to re-register unverified user");
        {
            RegisterCommand registerCommand = new RegisterCommand();
            registerCommand.setOrgId(ORG_ID);
            registerCommand.setApplicationId(appId);
            registerCommand.setParameters(Map.of("emailId", email, "password", password));

            try
            {
                CommandResponse registrationResponse = executeRegistrationCommand(registerCommand);
                Assert.fail("Re-registering an existing user should have returned an error.\n" + registrationResponse.getText());
            }
            catch (CommandException expected)
            {
                checker().verifyEquals("Wrong status code", HttpStatus.SC_BAD_REQUEST, expected.getStatusCode());
                checker().verifyThat("Response text", expected.getResponseText(), CoreMatchers.containsString("This email has already been used"));
            }
        }

        TestLogger.log("Verify new user");
        {
            var code = getVerificationCode();

            var verifyCommand = new VerifyCommand();
            verifyCommand.setOrgId(ORG_ID);
            verifyCommand.setApplicationId(appId);
            verifyCommand.setParameters(Map.of("emailId", email, "code", code));

            CommandResponse verifyResponse = verifyCommand.execute(getRegistrationConnection(), null);
            Map<String, Object> parsedData = verifyResponse.getParsedData();
            assertEquals("success", parsedData.get("message"));
        }

        TestLogger.log("Attempt to re-register verified user");
        {
            RegisterCommand registerCommand = new RegisterCommand();
            registerCommand.setOrgId(ORG_ID);
            registerCommand.setApplicationId(appId);
            registerCommand.setParameters(Map.of("emailId", email, "password", password));

            try
            {
                CommandResponse registrationResponse = executeRegistrationCommand(registerCommand);
                Assert.fail("Re-registering an existing user should have returned an error.\n" + registrationResponse.getText());
            }
            catch (CommandException expected)
            {
                checker().verifyEquals("Wrong status code", HttpStatus.SC_BAD_REQUEST, expected.getStatusCode());
                checker().verifyThat("Response text", expected.getResponseText(), CoreMatchers.containsString("This email has already been used"));
            }
        }
    }

    @Test
    public void testRegEmail() throws IOException, CommandException
    {
        Map.Entry<String, String> emailPassword = generateEmailPassword("regEmail@mystudies.registration.test");
        String email = emailPassword.getKey();
        String password = emailPassword.getValue();
        String appId = "RegisterNotificationApplication";
        AppPropertiesDetails appProperties = new AppPropertiesDetails();
        appProperties.setOrgId(ORG_ID);
        appProperties.setAppId(appId);
        appProperties.setRegEmailSub("Custom Email Subject <<< TOKEN HERE >>>");
        appProperties.setRegEmailBody("Custom Email Body <<< TOKEN HERE >>>");

        enableEmailRecorder();

        createAppFolder(appId, appProperties);

        RegisterCommand registerCommand = new RegisterCommand();
        registerCommand.setOrgId(ORG_ID);
        registerCommand.setApplicationId(appId);
        registerCommand.setParameters(Map.of("emailId", email, "password", password));

        CommandResponse registrationResponse = executeRegistrationCommand(registerCommand);
        Map<String, Object> parsedData = registrationResponse.getParsedData();
        assertEquals("success", parsedData.get("message"));

        goToEmailRecord();
        var code = getVerificationCode();
    }

    @Test
    public void testForgotPassEmail()
    {

    }

    @Test
    public void testFeedback()
    {

    }

    @Test
    public void testPasswordReset()
    {

    }

    @LogMethod
    private void createAppFolder(String appId, AppPropertiesDetails appProperties) throws IOException, CommandException
    {
        _containerHelper.createSubfolder(getProjectName(), appId);
        String containerPath = getProjectName() + "/" + appId;
        setModuleProperties(List.of(new ModulePropertyValue(MODULE_NAME, containerPath, "StudyId", appId)));
        updateAppProperties(appProperties);
        InsertExternalSchemaCommand.Params params = new InsertExternalSchemaCommand.Params("fdahpuserregws", "fdahpuserregws")
                .setTables(List.of("apppropertiesdetails", "authinfo", "loginattempts", "passwordhistory", "userappdetails", "userdetails"));
        CommandResponse response = new InsertExternalSchemaCommand(params)
                .execute(createDefaultConnection(), containerPath);
        response.getParsedData();
    }

    private CommandResponse updateAppProperties(AppPropertiesDetails appProperties) throws IOException, CommandException
    {
        return executeRegistrationCommand(new AppPropertiesUpdateCommand(appProperties));
    }

    @LogMethod
    private void createStudyFolder(String appId, String studyId) throws IOException, CommandException
    {
        _containerHelper.createSubfolder(getProjectName() + "/" + appId, studyId);
        String containerPath = getProjectName() + "/" + appId + "/" + studyId;
        setModuleProperties(List.of(new ModulePropertyValue(MODULE_NAME, containerPath, "StudyId", studyId)));
        InsertExternalSchemaCommand.Params params = new InsertExternalSchemaCommand.Params("fdahpuserregws", "fdahpuserregws")
                .setTables(List.of("participantactivities", "participantstudies", "studyconsent"));
        CommandResponse response = new InsertExternalSchemaCommand(params)
                .execute(createDefaultConnection(), containerPath);
        response.getParsedData();
    }

    private CommandResponse executeRegistrationCommand(FdahpUserRegWSCommand command) throws IOException, CommandException
    {
        try
        {
            return command.execute(getRegistrationConnection(), null);
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
        EmailRecordTable.EmailMessage message = emailRecordTable.getEmailAtTableIndex(3);
        emailRecordTable.clickMessage(message);
        Matcher matcher = verificationCodePattern.matcher(message.getBody());
        matcher.find();
        return matcher.group(1);
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
        return ORG_ID;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
