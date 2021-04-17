package org.labkey.test.tests.registration;

import com.hphc.remoteapi.registration.AppPropertiesUpdateCommand;
import com.hphc.remoteapi.registration.FdahpUserRegWSCommand;
import com.hphc.remoteapi.registration.NoCsrfConnection;
import com.hphc.remoteapi.registration.PingCommand;
import com.hphc.remoteapi.registration.RegisterCommand;
import com.hphc.remoteapi.registration.ResendConfirmationCommand;
import com.hphc.remoteapi.registration.VerifyCommand;
import com.hphc.remoteapi.registration.params.AppPropertiesDetails;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.core.SaveModulePropertiesCommand;
import org.labkey.remoteapi.query.InsertExternalSchemaCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.ModulePropertyValue;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.params.ModuleProperty;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.TestLogger;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;

@Category({})
public class MyStudiesRegistrationTest extends BaseWebDriverTest
{
    private static final String MODULE_NAME = "FdahpUserRegWS";
    private static final String ORG_ID = "MyStudies Test Organization";
    private static final String APP_ID = "Shared App";
    private static final Pattern verificationCodePattern = Pattern.compile("Verification Code:(\\w+)");

    @BeforeClass
    public static void setupProject() throws IOException, CommandException
    {
        MyStudiesRegistrationTest init = (MyStudiesRegistrationTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup() throws IOException, CommandException
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.enableModule("FdahpUserRegWS");
        new SaveModulePropertiesCommand(List.of(new ModulePropertyValue(MODULE_NAME, getProjectName(), "StudyId", getProjectName())))
            .execute(createDefaultConnection(), getProjectName());
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testPing() throws Exception
    {
        CommandResponse response = executeRegistrationCommand(new PingCommand());

        Map<String, Object> parsedData = response.getParsedData();
        assertEquals(true, parsedData.get("success"));
    }

    /**
     * 1. Register new user
     * 2. Attempt to register user again (rejected)
     * 3. Resend verification code
     * 4. Attempt to verify with first code (rejected)
     * 5. Verify new user
     * 6. Attempt to register verified user (rejected)
     *
     * @throws IOException from LabKey remote API commands
     * @throws CommandException from LabKey remote API commands
     */
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
            RegisterCommand registerCommand = new RegisterCommand(ORG_ID, appId);
            registerCommand.setParameters(Map.of("emailId", email, "password", password));

            CommandResponse registrationResponse = executeRegistrationCommand(registerCommand);
            Map<String, Object> parsedData = registrationResponse.getParsedData();
            assertEquals("success", parsedData.get("message"));
        }

        TestLogger.log("Attempt to re-register unverified user");
        {
            RegisterCommand registerCommand = new RegisterCommand(ORG_ID, appId);
            registerCommand.setParameters(Map.of("emailId", email, "password", password + "_bad"));

            try
            {
                CommandResponse registrationResponse = executeRegistrationCommand(registerCommand);
                Assert.fail("Re-registering an existing user should have returned an error.\n" + registrationResponse.getText());
            }
            catch (CommandException expected)
            {
                checker().verifyEquals("Wrong status code", HttpStatus.SC_BAD_REQUEST, expected.getStatusCode());
                checker().verifyThat("Response text", expected.getResponseText(), containsString("This email has already been used"));
            }
        }

        String code = getVerificationCode();
        String resentCode;

        TestLogger.log("Resend verification code");
        {
            enableEmailRecorder(); // Clear out initial verification email
            var resendConfirmationCommand = new ResendConfirmationCommand(ORG_ID, appId);
            resendConfirmationCommand.setParameters(Map.of("emailId", email));

            CommandResponse resendResponse = executeRegistrationCommand(resendConfirmationCommand);
            Map<String, Object> parsedData = resendResponse.getParsedData();
            assertEquals("success", parsedData.get("message"));

            resentCode = getVerificationCode();
            Assert.assertNotEquals("Same verification code received from 'ResendConfirmation'", code, resentCode);
        }

        TestLogger.log("Verify new user");
        {
            var verifyCommand = new VerifyCommand(ORG_ID, appId);
            verifyCommand.setParameters(Map.of("emailId", email, "code", code));

            TestLogger.log("Attempt to verify with invalid/replaced verification code");
            try
            {
                CommandResponse verifyResponse = executeRegistrationCommand(verifyCommand);
                Assert.fail("Verifying with an invalid code should have returned an error.\n" + verifyResponse.getText());
            }
            catch (CommandException expected)
            {
                checker().verifyEquals("Wrong status code", HttpStatus.SC_BAD_REQUEST, expected.getStatusCode());
                checker().verifyThat("Response text", expected.getResponseText(), containsString("Invalid code"));
            }

            verifyCommand.setParameters(Map.of("emailId", email, "code", resentCode));

            var verifyResponse = executeRegistrationCommand(verifyCommand);
            var parsedData = verifyResponse.getParsedData();
            assertEquals("success", parsedData.get("message"));
        }

        TestLogger.log("Attempt to re-register verified user");
        {
            RegisterCommand registerCommand = new RegisterCommand(ORG_ID, appId);
            registerCommand.setParameters(Map.of("emailId", email, "password", password));

            try
            {
                CommandResponse registrationResponse = executeRegistrationCommand(registerCommand);
                Assert.fail("Re-registering an existing user should have returned an error.\n" + registrationResponse.getText());
            }
            catch (CommandException expected)
            {
                checker().verifyEquals("Wrong status code", HttpStatus.SC_BAD_REQUEST, expected.getStatusCode());
                checker().verifyThat("Response text", expected.getResponseText(), containsString("This email has already been used"));
            }
        }
    }

    /**
     * Test custom email template for new user registration (including resending notification)
     *
     * @throws IOException from LabKey remote API commands
     * @throws CommandException from LabKey remote API commands
     */
    @Test
    public void testRegEmail() throws IOException, CommandException
    {
        Map.Entry<String, String> emailPassword = generateEmailPassword("regEmail@mystudies.registration.test");
        String email = emailPassword.getKey();
        String password = emailPassword.getValue();
        String appId = "RegisterNotificationApp";
        AppPropertiesDetails appProperties = new AppPropertiesDetails();
        appProperties.setOrgId(ORG_ID);
        appProperties.setAppId(appId);
        appProperties.setRegisterEmailSubject("Custom Email Subject <<< TOKEN HERE >>>");
        appProperties.setRegisterEmailBody("Custom Email Body <<< TOKEN HERE >>>");

        enableEmailRecorder();

        createAppFolder(appId, appProperties);

        RegisterCommand registerCommand = new RegisterCommand(ORG_ID, appId);
        registerCommand.setParameters(Map.of("emailId", email, "password", password));

        CommandResponse registrationResponse = executeRegistrationCommand(registerCommand);
        Map<String, Object> parsedData = registrationResponse.getParsedData();
        assertEquals("success", parsedData.get("message"));

        EmailRecordTable.EmailMessage verificationEmail = getVerificationEmail();
        checker().verifyThat("Registration email body.", verificationEmail.getBody(), containsString("Custom Email Body"));
        checker().verifyThat("Registration email body.", verificationEmail.getBody(), not(containsString("<<<")));
        checker().verifyThat("Registration email body.", verificationEmail.getSubject(), containsString("Custom Email Subject"));
        checker().verifyThat("Registration email body.", verificationEmail.getSubject(), containsString("<<<"));
        checker().screenShotIfNewError("confirmationEmail");

        enableEmailRecorder(); // Clear out initial verification email
        var resendConfirmationCommand = new ResendConfirmationCommand(ORG_ID, appId);
        resendConfirmationCommand.setParameters(Map.of("emailId", email));

        CommandResponse resendResponse = executeRegistrationCommand(resendConfirmationCommand);
        parsedData = resendResponse.getParsedData();
        assertEquals("success", parsedData.get("message"));

        verificationEmail = getVerificationEmail();
        checker().verifyThat("Resent registration email body.", verificationEmail.getBody(), containsString("Custom Email Body"));
        checker().verifyThat("Resent registration email body.", verificationEmail.getBody(), not(containsString("<<<")));
        checker().verifyThat("Resent registration email body.", verificationEmail.getSubject(), containsString("Custom Email Subject"));
        checker().verifyThat("Resent registration email body.", verificationEmail.getSubject(), containsString("<<<"));
        checker().screenShotIfNewError("resentConfirmationEmail");
    }

    @Test
    public void testForgotPassEmail() throws IOException, CommandException
    {
        String baseEmail = "forgotpass@mystudies.registration.test";
        String appId = "RegisterNotificationApplication";
        Map.Entry<String, String> emailPassword = createAppAndUser(baseEmail, appId);
        String email = emailPassword.getKey();
        String password = emailPassword.getValue();
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
        new SaveModulePropertiesCommand(List.of(new ModuleProperty(MODULE_NAME, containerPath, "StudyId", appId)))
                .execute(createDefaultConnection(), containerPath);
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
        new SaveModulePropertiesCommand(List.of(new ModuleProperty(MODULE_NAME, containerPath, "StudyId", studyId)))
                .execute(createDefaultConnection(), containerPath);
        InsertExternalSchemaCommand.Params params = new InsertExternalSchemaCommand.Params("fdahpuserregws", "fdahpuserregws")
                .setTables(List.of("participantactivities", "participantstudies", "studyconsent"));
        CommandResponse response = new InsertExternalSchemaCommand(params)
                .execute(createDefaultConnection(), containerPath);
        response.getParsedData();
    }

    private Map.Entry<String, String> createAppAndUser(String baseEmail, String appId) throws IOException, CommandException
    {
        Map.Entry<String, String> emailPassword = generateEmailPassword(baseEmail);
        String email = emailPassword.getKey();
        String password = emailPassword.getValue();
        AppPropertiesDetails appProperties = new AppPropertiesDetails();
        appProperties.setOrgId(ORG_ID);
        appProperties.setAppId(appId);

        enableEmailRecorder();

        createAppFolder(appId, appProperties);

        RegisterCommand registerCommand = new RegisterCommand(ORG_ID, appId);
        registerCommand.setParameters(Map.of("emailId", email, "password", password));

        CommandResponse registrationResponse = executeRegistrationCommand(registerCommand);
        Map<String, Object> parsedData = registrationResponse.getParsedData();
        assertEquals("success", parsedData.get("message"));
        var code = getVerificationCode();

        var verifyCommand = new VerifyCommand(ORG_ID, appId);
        verifyCommand.setParameters(Map.of("emailId", email, "code", code));

        var verifyResponse = executeRegistrationCommand(verifyCommand);
        assertEquals("success", verifyResponse.getParsedData().get("message"));

        return emailPassword;
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
        EmailRecordTable.EmailMessage message = getVerificationEmail();
        Matcher matcher = verificationCodePattern.matcher(message.getBody());
        matcher.find();
        return matcher.group(1);
    }

    @NotNull
    private EmailRecordTable.EmailMessage getVerificationEmail()
    {
        EmailRecordTable emailRecordTable = goToEmailRecord();
        EmailRecordTable.EmailMessage message = emailRecordTable.getEmailAtTableIndex(3);
        emailRecordTable.clickMessage(message);
        return message;
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
