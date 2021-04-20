package org.labkey.test.tests.registration;

import com.hphc.remoteapi.registration.AppPropertiesUpdateCommand;
import com.hphc.remoteapi.registration.ForgotPasswordCommand;
import com.hphc.remoteapi.registration.LoginCommand;
import com.hphc.remoteapi.registration.LoginResponse;
import com.hphc.remoteapi.registration.LogoutCommand;
import com.hphc.remoteapi.registration.NoCsrfConnection;
import com.hphc.remoteapi.registration.PingCommand;
import com.hphc.remoteapi.registration.RegisterCommand;
import com.hphc.remoteapi.registration.RegistrationCommand;
import com.hphc.remoteapi.registration.ResendConfirmationCommand;
import com.hphc.remoteapi.registration.VerifyCommand;
import com.hphc.remoteapi.registration.params.AppPropertiesDetails;
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

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
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
    private static final Pattern forgotPasswordPattern = Pattern.compile("Temporary Password: (\\w+)");

    @BeforeClass
    public static void setupProject() throws Exception
    {
        MyStudiesRegistrationTest init = (MyStudiesRegistrationTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup() throws Exception
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
    public void testRegisterNewUser() throws Exception
    {
        Map.Entry<String, String> emailPassword = generateEmailPassword("regNewUser@mystudies.registration.test");
        String email = emailPassword.getKey();
        String password = emailPassword.getValue();
        String appId = "testRegisterNewUser";
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

            var expected = executeBadRequest(registerCommand);
            checker().verifyThat("Response text", expected.getResponseText(), containsString("This email has already been used"));
        }

        String code = getVerificationCode();
        String resentCode;

        TestLogger.log("Resend verification code");
        {
            enableEmailRecorder(); // Clear out initial verification email
            var resendConfirmationCommand = new ResendConfirmationCommand(ORG_ID, appId, email);

            CommandResponse resendResponse = executeRegistrationCommand(resendConfirmationCommand);
            Map<String, Object> parsedData = resendResponse.getParsedData();
            assertEquals("success", parsedData.get("message"));

            resentCode = getVerificationCode();
            Assert.assertNotEquals("Same verification code received from 'ResendConfirmation'", code, resentCode);
        }

        TestLogger.log("Verify new user");
        {
            var verifyCommand = new VerifyCommand(ORG_ID, appId, email, code);

            TestLogger.log("Attempt to verify with invalid/replaced verification code");
            CommandException expected = executeBadRequest(verifyCommand);
            checker().verifyThat("Response text", expected.getResponseText(), containsString("Invalid code"));

            verifyCommand.setParameters(Map.of("emailId", email, "code", resentCode));

            var verifyResponse = executeRegistrationCommand(verifyCommand);
            var parsedData = verifyResponse.getParsedData();
            assertEquals("success", parsedData.get("message"));
        }

        TestLogger.log("Attempt to re-register verified user");
        {
            RegisterCommand registerCommand = new RegisterCommand(ORG_ID, appId);
            registerCommand.setParameters(Map.of("emailId", email, "password", password));

            CommandException expected = executeBadRequest(registerCommand);
            checker().verifyThat("Response text", expected.getResponseText(), containsString("This email has already been used"));
        }
    }

    /**
     * Test custom email template for new user registration (including resending notification)
     *
     * @throws IOException from LabKey remote API commands
     * @throws CommandException from LabKey remote API commands
     */
    @Test
    public void testRegEmail() throws Exception
    {
        Map.Entry<String, String> emailPassword = generateEmailPassword("regEmail@mystudies.registration.test");
        String email = emailPassword.getKey();
        String password = emailPassword.getValue();
        String appId = "testRegEmail";
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

        EmailRecordTable.EmailMessage verificationEmail = getNotificationEmail();
        checker().verifyEquals("Registration email to.", Arrays.asList(email), Arrays.asList(emailPassword.getKey()));
        checker().verifyThat("Registration email body.", verificationEmail.getBody(), containsString("Custom Email Body"));
        checker().verifyThat("Registration email body.", verificationEmail.getBody(), not(containsString("<<<")));
        checker().verifyThat("Registration email subject.", verificationEmail.getSubject(), containsString("Custom Email Subject"));
        checker().verifyThat("Registration email subject.", verificationEmail.getSubject(), containsString("<<<"));
        checker().screenShotIfNewError("confirmationEmail");

        enableEmailRecorder(); // Clear out initial verification email
        var resendConfirmationCommand = new ResendConfirmationCommand(ORG_ID, appId, email);

        CommandResponse resendResponse = executeRegistrationCommand(resendConfirmationCommand);
        parsedData = resendResponse.getParsedData();
        assertEquals("success", parsedData.get("message"));

        verificationEmail = getNotificationEmail();
        checker().verifyEquals("Registration email to.", Arrays.asList(email), Arrays.asList(emailPassword.getKey()));
        checker().verifyThat("Resent registration email body.", verificationEmail.getBody(), containsString("Custom Email Body"));
        checker().verifyThat("Resent registration email body.", verificationEmail.getBody(), not(containsString("<<<")));
        checker().verifyThat("Resent registration email subject.", verificationEmail.getSubject(), containsString("Custom Email Subject"));
        checker().verifyThat("Resent registration email subject.", verificationEmail.getSubject(), containsString("<<<"));
        checker().screenShotIfNewError("resentConfirmationEmail");
    }

    @Test
    public void testForgotPassEmail() throws Exception
    {
        String baseEmail = "forgotpassemail@mystudies.registration.test";
        String appId = "testForgotPassEmail";

        Map.Entry<String, String> emailPassword = createAppAndUser(baseEmail, appId);

        AppPropertiesDetails appProperties = new AppPropertiesDetails();
        appProperties.setOrgId(ORG_ID);
        appProperties.setAppId(appId);
        appProperties.setForgotPassEmailSubject("Custom Forgot Password Subject <<< TOKEN HERE >>>");
        appProperties.setForgotPassEmailBody("Custom Forgot Password Body <<< TOKEN HERE >>>");
        updateAppProperties(appProperties);

        executeRegistrationCommand(new ForgotPasswordCommand(ORG_ID, appId, emailPassword.getKey()));

        var verificationEmail = getNotificationEmail();
        checker().verifyEquals("Forgot password email to.", Arrays.asList(emailPassword.getKey()), Arrays.asList(verificationEmail.getTo()));
        checker().verifyThat("Forgot password email body.", verificationEmail.getBody(), containsString("Custom Forgot Password Body"));
        checker().verifyThat("Forgot password email body.", verificationEmail.getBody(), not(containsString("<<<")));
        checker().verifyEquals("Forgot password email subject.", "Custom Forgot Password Subject <<< TOKEN HERE >>>", verificationEmail.getSubject());
        checker().screenShotIfNewError("forgotPasswordEmail");
    }

    @Test
    public void testForgotPasswordAndReset() throws Exception
    {
        String baseEmail = "forgotpassword@mystudies.registration.test";
        String appId = "testForgotPasswordAndReset";

        Map.Entry<String, String> emailPassword = createAppAndUser(baseEmail, appId);
        String email = emailPassword.getKey();
        String password = emailPassword.getValue();

        CommandResponse commandResponse;
        Map<String, Object> parsedData;

        commandResponse = executeRegistrationCommand(new ForgotPasswordCommand(ORG_ID, appId, email));
        parsedData = commandResponse.getParsedData();
        checker().verifyEquals("Forgot password", Map.of("message", "success"), parsedData);
        var temporaryPassword = getTemporaryPassword();

        commandResponse = executeRegistrationCommand(new LoginCommand(ORG_ID, appId, email, temporaryPassword));
        parsedData = commandResponse.getParsedData();
        checker().verifyEquals("Login with temporary password", "success", parsedData.get("message"));
        String auth = (String) parsedData.get("auth");

    }

    @Test
    public void testLoginInvalidatesTemporaryPassword() throws Exception
    {
        String baseEmail = "forgotpassword2@mystudies.registration.test";
        String appId = "testLoginInvalidatesTemporaryPassword";

        Map.Entry<String, String> emailPassword = createAppAndUser(baseEmail, appId);
        String email = emailPassword.getKey();
        String password = emailPassword.getValue();

        CommandResponse commandResponse;
        Map<String, Object> parsedData;

        commandResponse = executeRegistrationCommand(new ForgotPasswordCommand(ORG_ID, appId, email));
        parsedData = commandResponse.getParsedData();
        checker().verifyEquals("Forgot password", Map.of("message", "success"), parsedData);
        var temporaryPassword = getTemporaryPassword();

        LoginResponse loginResponse = executeRegistrationCommand(new LoginCommand(ORG_ID, appId, email, password));
        parsedData = loginResponse.getParsedData();
        checker().verifyEquals("Login with initial password after forgot password", "success", parsedData.get("message"));
        checker().verifyEquals("Login with initial password after forgot password", true, parsedData.get("verified"));
        var auth = loginResponse.getAuth();

        commandResponse = executeRegistrationCommand(new LogoutCommand(auth));
        parsedData = commandResponse.getParsedData();
        checker().verifyEquals("Logout initial password session", "success", parsedData.get("message"));

        try
        {
            executeRegistrationCommand(new LoginCommand(ORG_ID, appId, email, temporaryPassword));
            Assert.fail("Temporary password did not expire after logging in with existing password");
        }
        catch (CommandException e)
        {
            checker().verifyEquals("Wrong response using invalid temporary password", SC_UNAUTHORIZED, e.getStatusCode());
        }
    }

    @Test
    public void testSingleUseTemporaryPassword() throws Exception
    {
        String baseEmail = "forgotpassword3@mystudies.registration.test";
        String appId = "testSingleUseTemporaryPassword";

        Map.Entry<String, String> emailPassword = createAppAndUser(baseEmail, appId);
        String email = emailPassword.getKey();
        String password = emailPassword.getValue();

        CommandResponse commandResponse;
        Map<String, Object> parsedData;

        commandResponse = executeRegistrationCommand(new ForgotPasswordCommand(ORG_ID, appId, email));
        parsedData = commandResponse.getParsedData();
        checker().verifyEquals("Forgot password", Map.of("message", "success"), parsedData);
        var temporaryPassword = getTemporaryPassword();

        LoginResponse loginResponse = executeRegistrationCommand(new LoginCommand(ORG_ID, appId, email, temporaryPassword));
        parsedData = loginResponse.getParsedData();
        checker().verifyEquals("Login with temporary password", "success", parsedData.get("message"));
        var auth = loginResponse.getAuth();

        commandResponse = executeRegistrationCommand(new LogoutCommand(auth));
        parsedData = commandResponse.getParsedData();
        checker().verifyEquals("Logout temporary password session", "success", parsedData.get("message"));

        try
        {
            executeRegistrationCommand(new LoginCommand(ORG_ID, appId, email, temporaryPassword));
            Assert.fail("Temporary password did not expire after a single use");
        }
        catch (CommandException e)
        {
            checker().verifyEquals("Wrong response after reusing temporary password", SC_UNAUTHORIZED, e.getStatusCode());
        }
    }

    @Test
    public void testPasswordReset() throws Exception
    {

    }

    @Test
    public void testChangePassword() throws Exception
    {

    }

    @Test
    public void testFeedback() throws Exception
    {
        String baseEmail = "feedback@mystudies.registration.test";
        String appId = "FeedbackTestApplication";

        Map.Entry<String, String> emailPassword = createAppAndUser(baseEmail, appId);
        String email = emailPassword.getKey();
        String password = emailPassword.getValue();
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

        var verifyCommand = new VerifyCommand(ORG_ID, appId, email, code);

        var verifyResponse = executeRegistrationCommand(verifyCommand);
        assertEquals("success", verifyResponse.getParsedData().get("message"));

        return emailPassword;
    }

    private <T extends CommandResponse> T executeRegistrationCommand(RegistrationCommand<T> command) throws IOException, CommandException
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

    private CommandException executeBadRequest(RegistrationCommand<?> command) throws IOException
    {
        try
        {
            CommandResponse response = executeRegistrationCommand(command);
            throw new AssertionError(String.format("%s should have returned an error. Received: \"%s\"",
                    command.getClass().getSimpleName(), response.getText()));
        }
        catch (CommandException e)
        {
            Assert.assertEquals(String.format("%s should have returned an error. Received: \"%s\"", command.getClass().getSimpleName(), e.getResponseText()),
                    SC_BAD_REQUEST, e.getStatusCode());
            return e;
        }
    }

    protected Connection getRegistrationConnection()
    {
        return new NoCsrfConnection(WebTestHelper.getBaseURL());
    }

    protected String getVerificationCode()
    {
        EmailRecordTable.EmailMessage message = getNotificationEmail();
        Matcher matcher = verificationCodePattern.matcher(message.getBody());
        matcher.find();
        return matcher.group(1);
    }

    protected String getTemporaryPassword()
    {
        EmailRecordTable.EmailMessage message = getNotificationEmail();
        Matcher matcher = forgotPasswordPattern.matcher(message.getBody());
        matcher.find();
        return matcher.group(1);
    }

    @NotNull
    private EmailRecordTable.EmailMessage getNotificationEmail()
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
