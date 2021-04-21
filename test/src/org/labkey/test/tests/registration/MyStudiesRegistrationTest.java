package org.labkey.test.tests.registration;

import com.hphc.remoteapi.fdahpuserregws.AppPropertiesUpdateCommand;
import com.hphc.remoteapi.fdahpuserregws.ChangePasswordCommand;
import com.hphc.remoteapi.fdahpuserregws.FdahpUserRegWSCommand;
import com.hphc.remoteapi.fdahpuserregws.FeedbackCommand;
import com.hphc.remoteapi.fdahpuserregws.ForgotPasswordCommand;
import com.hphc.remoteapi.fdahpuserregws.LoginCommand;
import com.hphc.remoteapi.fdahpuserregws.LoginResponse;
import com.hphc.remoteapi.fdahpuserregws.LogoutCommand;
import com.hphc.remoteapi.fdahpuserregws.NoCsrfConnection;
import com.hphc.remoteapi.fdahpuserregws.PingCommand;
import com.hphc.remoteapi.fdahpuserregws.RegisterCommand;
import com.hphc.remoteapi.fdahpuserregws.ResendConfirmationCommand;
import com.hphc.remoteapi.fdahpuserregws.VerifyCommand;
import com.hphc.remoteapi.fdahpuserregws.params.AppPropertiesDetails;
import com.hphc.remoteapi.fdahpuserregws.params.RegistrationSession;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
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
import org.labkey.test.util.LoggedParam;
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
    private static final String SHARED_APP = "Shared App";
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
        createAppFolder(new AppPropertiesDetails(ORG_ID, SHARED_APP));
    }

    @Before
    public void preTest() throws IOException, CommandException
    {
        updateAppProperties(new AppPropertiesDetails(ORG_ID, SHARED_APP)); // Reset default app properties
    }

    /**
     * Verify ping action
     * @throws IOException from LabKey remote API commands
     * @throws CommandException from LabKey remote API commands
     */
    @Test
    public void testPing() throws Exception
    {
        CommandResponse response = executeRegistrationCommand(new PingCommand());

        Map<String, Object> parsedData = response.getParsedData();
        assertEquals(true, parsedData.get("success"));
    }

    /**
     * Test registration and validation of a new user
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
        AppPropertiesDetails appProperties = new AppPropertiesDetails(ORG_ID, appId);

        enableEmailRecorder();

        createAppFolder(appProperties);

        TestLogger.log("Register new user");
        {
            RegisterCommand registerCommand = new RegisterCommand(ORG_ID, appId, email, password);

            CommandResponse registrationResponse = executeRegistrationCommand(registerCommand);
            Map<String, Object> parsedData = registrationResponse.getParsedData();
            assertEquals("success", parsedData.get("message"));
        }

        TestLogger.log("Attempt to re-register unverified user");
        {
            RegisterCommand registerCommand = new RegisterCommand(ORG_ID, appId, email, password + "_bad");

            var expected = executeBadRequest("Attempt to re-register unverified user", registerCommand, SC_BAD_REQUEST);
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

        // Verify new user
        {
            var verifyCommand = new VerifyCommand(ORG_ID, appId, email, code);

            CommandException expected = executeBadRequest("Verify with invalid/replaced verification code", verifyCommand, SC_BAD_REQUEST);
            checker().verifyThat("Response text", expected.getResponseText(), containsString("Invalid code"));

            verifyCommand.setParameters(Map.of("emailId", email, "code", resentCode));

            var verifyResponse = executeRegistrationCommand(verifyCommand);
            var parsedData = verifyResponse.getParsedData();
            assertEquals("success", parsedData.get("message"));
        }

        // Re-register
        {
            RegisterCommand registerCommand = new RegisterCommand(ORG_ID, appId, email, password);

            CommandException expected = executeBadRequest("Attempt to re-register verified user", registerCommand, SC_BAD_REQUEST);
            checker().verifyThat("Response text", expected.getResponseText(), containsString("This email has already been used"));
        }
    }

    /**
     * Test custom email template for new user registration (including resending notification)
     * 1. Customize register email templates for app
     * 2. Register a new user
     * 3. Verify email subject, body, including verification code
     * 4. Trigger ResendConfirmationAction for registered user
     * 3. Verify email subject, body, including verification code
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
        String appId = "testCustomRegistrationEmail";
        AppPropertiesDetails appProperties = new AppPropertiesDetails(ORG_ID, appId);
        appProperties.setRegisterEmailSubject("Custom Email Subject <<< TOKEN HERE >>>");
        appProperties.setRegisterEmailBody("Custom Email Body <<< TOKEN HERE >>>");

        enableEmailRecorder();

        createAppFolder(appProperties);

        RegisterCommand registerCommand = new RegisterCommand(ORG_ID, appId, email, password);

        CommandResponse registrationResponse = executeRegistrationCommand(registerCommand);
        Map<String, Object> parsedData = registrationResponse.getParsedData();
        assertEquals("success", parsedData.get("message"));

        EmailRecordTable.EmailMessage verificationEmail = getNotificationEmail();
        checker().verifyEquals("Registration email to.", Arrays.asList(email), Arrays.asList(verificationEmail.getTo()));
        checker().verifyThat("Registration email body.", verificationEmail.getBody(), containsString("Custom Email Body"));
        checker().verifyThat("Registration email body.", verificationEmail.getBody(), not(containsString("<<<")));
        checker().verifyEquals("Registration email subject.", "Custom Email Subject <<< TOKEN HERE >>>", verificationEmail.getSubject());
        checker().screenShotIfNewError("confirmationEmail");

        enableEmailRecorder(); // Clear out initial verification email
        var resendConfirmationCommand = new ResendConfirmationCommand(ORG_ID, appId, email);

        CommandResponse resendResponse = executeRegistrationCommand(resendConfirmationCommand);
        parsedData = resendResponse.getParsedData();
        assertEquals("success", parsedData.get("message"));

        verificationEmail = getNotificationEmail();
        checker().verifyEquals("Registration email to.", Arrays.asList(email), Arrays.asList(verificationEmail.getTo()));
        checker().verifyThat("Resent registration email body.", verificationEmail.getBody(), containsString("Custom Email Body"));
        checker().verifyThat("Resent registration email body.", verificationEmail.getBody(), not(containsString("<<<")));
        checker().verifyEquals("Resent registration email subject.", "Custom Email Subject <<< TOKEN HERE >>>", verificationEmail.getSubject());
        checker().screenShotIfNewError("resentConfirmationEmail");
    }

    /**
     * Test custom email template for forgotten password
     * 1. Create app with custom forgot password template
     * 2. Verify email body contains temporary password
     * 3. Verify email subject
     *
     * @throws IOException from LabKey remote API commands
     * @throws CommandException from LabKey remote API commands
     */
    @Test
    public void testForgotPassEmail() throws Exception
    {
        String email = "forgotpassemail@mystudies.registration.test";
        String appId = "testForgotPassEmail";
        createAppAndUser(email, appId);

        AppPropertiesDetails appProperties = new AppPropertiesDetails(ORG_ID, appId);
        appProperties.setForgotPassEmailSubject("Custom Forgot Password Subject <<< TOKEN HERE >>>");
        appProperties.setForgotPassEmailBody("Custom Forgot Password Body <<< TOKEN HERE >>>");
        updateAppProperties(appProperties);

        executeRegistrationCommand(new ForgotPasswordCommand(ORG_ID, appId, email));

        var verificationEmail = getNotificationEmail();
        checker().verifyEquals("Forgot password email to.", Arrays.asList(email), Arrays.asList(verificationEmail.getTo()));
        checker().verifyThat("Forgot password email body.", verificationEmail.getBody(), containsString("Custom Forgot Password Body"));
        checker().verifyThat("Forgot password email body.", verificationEmail.getBody(), not(containsString("<<<")));
        checker().verifyEquals("Forgot password email subject.", "Custom Forgot Password Subject <<< TOKEN HERE >>>", verificationEmail.getSubject());
        checker().screenShotIfNewError("forgotPasswordEmail");
    }

    /**
     * Verified that the temporary password generated by 'ForgotPasswordAction' is invalidated after a successful login.
     *
     * @throws IOException from LabKey remote API commands
     * @throws CommandException from LabKey remote API commands
     */
    @Test
    public void testLoginInvalidatesTemporaryPassword() throws Exception
    {
        String baseEmail = "forgotpassword2@mystudies.registration.test";
        String appId = SHARED_APP;

        Map.Entry<String, String> emailPassword = createUser(baseEmail, appId);
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
        checker().verifyEquals("Login with initial password after forgot password", true, parsedData.get("verified"));
        var session = loginResponse.getSession();

        commandResponse = executeRegistrationCommand(new LogoutCommand(session));
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

    /**
     * Test that temporary password only works once, as indicated by default email generated by 'ForgotPasswordAction'
     * 1. Generate temporary password
     * 2. Login with temporary password
     * 3. Logout
     *
     * @throws IOException from LabKey remote API commands
     * @throws CommandException from LabKey remote API commands
     */
    @Test @Ignore("TODO: Is this a valid scenario? The current behavior does not match.")
    public void testSingleUseTemporaryPassword() throws Exception
    {
        String baseEmail = "forgotpassword3@mystudies.registration.test";
        String appId = SHARED_APP;

        Map.Entry<String, String> emailPassword = createUser(baseEmail, appId);
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
        checker().verifyEquals("Login with temporary password", true, parsedData.get("verified"));
        var session = loginResponse.getSession();

        commandResponse = executeRegistrationCommand(new LogoutCommand(session));
        parsedData = commandResponse.getParsedData();
        checker().verifyEquals("Logout temporary password session", "success", parsedData.get("message"));

        executeBadRequest("Temporary password did not expire after a single use.",
                new LoginCommand(ORG_ID, appId, email, temporaryPassword), SC_UNAUTHORIZED);
    }

    /**
     * Verify 'ChangePasswordAction'
     * 1. Login
     * 2. Change password
     * 3. Attempt to log in with initial password (rejected)
     * 4. Log in with new password
     *
     * @throws IOException from LabKey remote API commands
     * @throws CommandException from LabKey remote API commands
     */
    @Test
    public void testChangePassword() throws Exception
    {
        String baseEmail = "changepassword@mystudies.registration.test";
        String appId = SHARED_APP;

        Map.Entry<String, String> emailPassword = createUser(baseEmail, appId);
        String email = emailPassword.getKey();
        String password = emailPassword.getValue();

        LoginResponse loginResponse = executeRegistrationCommand(new LoginCommand(ORG_ID, appId, email, password));
        checker().verifyEquals("Login", true, loginResponse.getParsedData().get("verified"));
        RegistrationSession session = loginResponse.getSession();

        String newPassword = "changed" + password;
        CommandResponse changePasswordResponse = executeRegistrationCommand(new ChangePasswordCommand(session, password, newPassword));
        checker().verifyEquals("Reset password", Map.of("message", "success"), changePasswordResponse.getParsedData());

        // TODO?: Verify that pre-password session is closed

        executeBadRequest("Attempt to login with old password", new LoginCommand(ORG_ID, appId, email, password), SC_UNAUTHORIZED);

        executeRegistrationCommand(new LoginCommand(ORG_ID, appId, email, newPassword));
        checker().verifyEquals("Login with new password", true, loginResponse.getParsedData().get("verified"));

    }

    /**
     * Verify 'ChangePasswordAction' works with the temporary password generated by 'ForgotPasswordAction'
     * 1. Generate temporary password
     * 2. Login with temporary password
     * 3. Change password
     * 4. Login with new password
     * 5. Attempt to login with "forgotten" password (rejected)
     * 6. Attempt to login with temporary password (rejected)
     *
     * @throws IOException from LabKey remote API commands
     * @throws CommandException from LabKey remote API commands
     */
    @Test
    public void testChangeForgottenPassword() throws Exception
    {
        String baseEmail = "changeforgottenpassword@mystudies.registration.test";
        String appId = SHARED_APP;

        Map.Entry<String, String> emailPassword = createUser(baseEmail, appId);
        String email = emailPassword.getKey();
        String password = emailPassword.getValue();

        var commandResponse = executeRegistrationCommand(new ForgotPasswordCommand(ORG_ID, appId, email));
        checker().verifyEquals("Forgot password", Map.of("message", "success"), commandResponse.getParsedData());
        var temporaryPassword = getTemporaryPassword();

        LoginResponse loginResponse = executeRegistrationCommand(new LoginCommand(ORG_ID, appId, email, temporaryPassword));
        checker().verifyEquals("Login with temporary password", true, loginResponse.getParsedData().get("verified"));
        RegistrationSession session = loginResponse.getSession();

        String newPassword = "changed" + temporaryPassword;
        CommandResponse changePasswordResponse = executeRegistrationCommand(new ChangePasswordCommand(session, temporaryPassword, newPassword));
        checker().verifyEquals("Reset password", Map.of("message", "success"), changePasswordResponse.getParsedData());

        executeRegistrationCommand(new LoginCommand(ORG_ID, appId, email, newPassword));
        checker().verifyEquals("Login with new password", true, loginResponse.getParsedData().get("verified"));

        executeBadRequest("Attempt to login with old password", new LoginCommand(ORG_ID, appId, email, password), SC_UNAUTHORIZED);

        executeBadRequest("Attempt to login with old temporary password", new LoginCommand(ORG_ID, appId, email, temporaryPassword), SC_UNAUTHORIZED);
    }

    /**
     * Test 'FeedbackAction'
     * 1. Create app and define feedback email recipient
     * 2. Submit feedback
     * 3. Verify feedback is sent to the correct recipient
     *
     * @throws IOException from LabKey remote API commands
     * @throws CommandException from LabKey remote API commands
     */
    @Test
    public void testFeedback() throws Exception
    {
        String email = "feedback@mystudies.registration.test";
        String appId = "testFeedback";
        AppPropertiesDetails appProperties = new AppPropertiesDetails(ORG_ID, appId);
        appProperties.setFeedbackEmail(email);

        createAppFolder(appProperties);

        String feedback = "Feedback line 1\nFeedback line2";
        executeRegistrationCommand(new FeedbackCommand(ORG_ID, appId, feedback));
        var feedbackNotification = getNotificationEmail();
        checker().verifyEquals("Feedback recipient.", Arrays.asList(email), Arrays.asList(feedbackNotification.getTo()));
        checker().verifyThat("Forgot password email body.", feedbackNotification.getBody(), containsString(feedback.replace("\n", " ")));
    }

    // TODO: Test more actions
    // ConfirmRegistrationAction
    // UserProfileAction
    // UpdateUserProfileAction
    // UpdatePreferencesAction
    // UserPreferencesAction
    // UpdateEligibilityConsentStatusAction
    // ActivityStateAction
    // UpdateActivityStateAction
    // WithdrawAction (DELETE)
    // ConsentPDFAction
    // DeleteAccountAction
    // DeactivateAction (DELETE)
    // UpdateStudyStateAction
    // StudyStateAction
    // SendNotificationAction
    // GenerateTheFileAction
    // RefreshTokenAction
    // ContactUsAction

    /**
     * Create and configure an app folder under the default test Organization (ORG_ID)
     * Follows <a href="https://www.labkey.org/FDAMyStudiesHelp/wiki-page.view?name=setupInstructions">App setup instructions</a> from labkey.org
     * @param appProperties properties for the app to create
     * @throws IOException from LabKey remote API commands
     * @throws CommandException from LabKey remote API commands
     */
    @LogMethod
    private void createAppFolder(AppPropertiesDetails appProperties) throws IOException, CommandException
    {
        String appId = appProperties.getAppId();
        _containerHelper.createSubfolder(getProjectName(), appId);
        String containerPath = getProjectName() + "/" + appId;
        new SaveModulePropertiesCommand(List.of(new ModuleProperty(MODULE_NAME, containerPath, "StudyId", appId)))
                .execute(createDefaultConnection(), containerPath);
        updateAppProperties(appProperties);
        InsertExternalSchemaCommand.Params params = new InsertExternalSchemaCommand.Params("fdahpuserregws", "fdahpuserregws")
                .setTables(List.of("apppropertiesdetails", "authinfo", "loginattempts", "passwordhistory", "userappdetails", "userdetails"));
        CommandResponse response = new InsertExternalSchemaCommand(params)
                .execute(createDefaultConnection(), containerPath);
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
        createAppFolder(new AppPropertiesDetails(ORG_ID, appId));
        return createUser(baseEmail, appId);
    }

    /**
     * Register and verify a new user in the specified app
     * @param baseEmail A unique email will be generated based on this
     * @param appId Application ID under the default test Org (ORG_ID)
     * @return password for the created user
     * @throws IOException from LabKey remote API commands
     * @throws CommandException from LabKey remote API commands
     */
    private Map.Entry<String, String> createUser(String baseEmail, String appId) throws IOException, CommandException
    {
        Map.Entry<String, String> emailPassword = generateEmailPassword(baseEmail);
        String email = emailPassword.getKey();
        String password = emailPassword.getValue();

        enableEmailRecorder();

        RegisterCommand registerCommand = new RegisterCommand(ORG_ID, appId, email, password);

        CommandResponse registrationResponse = executeRegistrationCommand(registerCommand);
        Map<String, Object> parsedData = registrationResponse.getParsedData();
        assertEquals("success", parsedData.get("message"));
        var code = getVerificationCode();

        var verifyCommand = new VerifyCommand(ORG_ID, appId, email, code);

        var verifyResponse = executeRegistrationCommand(verifyCommand);
        assertEquals("success", verifyResponse.getParsedData().get("message"));

        return emailPassword;
    }

    private <T extends CommandResponse> T executeRegistrationCommand(FdahpUserRegWSCommand<T> command) throws IOException, CommandException
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

    @LogMethod
    private CommandException executeBadRequest(@LoggedParam String message, FdahpUserRegWSCommand<?> command, int expectedStatusCode) throws IOException
    {
        try
        {
            CommandResponse response = executeRegistrationCommand(command);
            throw new AssertionError(String.format("%s\n%s should have returned an error. Received: \"%s\"", message,
                    command.getClass().getSimpleName(), response.getText()));
        }
        catch (CommandException e)
        {
            Assert.assertEquals(String.format("%s should have returned an error. Received: \"%s\"", command.getClass().getSimpleName(), e.getResponseText()),
                    expectedStatusCode, e.getStatusCode());
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

    private static String randString()
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

    private static Map.Entry<String,String> generateEmailPassword(String baseEmail)
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
