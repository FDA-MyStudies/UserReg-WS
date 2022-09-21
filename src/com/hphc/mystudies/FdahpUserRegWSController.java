/*
 * Copyright © 2017-2019 Harvard Pilgrim Health Care Institute (HPHCI) and its Contributors. Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * Funding Source: Food and Drug Administration (“Funding Agency”) effective 18 September 2014 as Contract no. HHSF22320140030I/HHSF22301006T (the “Prime Contract”).
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package com.hphc.mystudies;

import com.hphc.mystudies.bean.ActivitiesBean;
import com.hphc.mystudies.bean.AppPropertiesDetailsBean;
import com.hphc.mystudies.bean.ConsentBean;
import com.hphc.mystudies.bean.CustomScheduleRunsBean;
import com.hphc.mystudies.bean.FeedbackAndContactUsBean;
import com.hphc.mystudies.bean.InfoBean;
import com.hphc.mystudies.bean.NotificationBean;
import com.hphc.mystudies.bean.ParticipantForm;
import com.hphc.mystudies.bean.ParticipantInfoBean;
import com.hphc.mystudies.bean.ProfileBean;
import com.hphc.mystudies.bean.SettingsBean;
import com.hphc.mystudies.bean.StudiesBean;
import com.hphc.mystudies.model.AppPropertiesDetails;
import com.hphc.mystudies.model.AuthInfo;
import com.hphc.mystudies.model.CustomScheduleRuns;
import com.hphc.mystudies.model.FdahpUserRegUtil;
import com.hphc.mystudies.model.LoginAttempts;
import com.hphc.mystudies.model.ParticipantActivities;
import com.hphc.mystudies.model.ParticipantStudies;
import com.hphc.mystudies.model.PasswordHistory;
import com.hphc.mystudies.model.StudyConsent;
import com.hphc.mystudies.model.UserAppDetails;
import com.hphc.mystudies.model.UserDetails;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.json.old.JSONArray;
import org.json.old.JSONObject;
import org.labkey.api.action.ApiResponse;
import org.labkey.api.action.ApiSimpleResponse;
import org.labkey.api.action.Marshal;
import org.labkey.api.action.Marshaller;
import org.labkey.api.action.MutatingApiAction;
import org.labkey.api.action.ReadOnlyApiAction;
import org.labkey.api.action.ReturnUrlForm;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.admin.AdminUrls;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.files.FileContentService;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.module.ModuleProperty;
import org.labkey.api.security.CSRF;
import org.labkey.api.security.MethodsAllowed;
import org.labkey.api.security.RequiresNoPermission;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.AdminOperationsPermission;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.util.HttpUtil;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

public class FdahpUserRegWSController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(FdahpUserRegWSController.class);
    public static final String NAME = "fdahpuserregws";
    public static final String LANGUAGE_EN = "en";
    public static final String LANGUAGE_SP = "es";
    public static final String DEFAULT_LANGUAGE = LANGUAGE_EN;
    String language = DEFAULT_LANGUAGE;

    public FdahpUserRegWSController()
    {
        setActionResolver(_actionResolver);
    }

    private static final Logger _log = LogManager.getLogger(FdahpUserRegWSController.class);

    Properties configProp = FdahpUserRegUtil.getProperties();

    /**
     * Check the status of the application
     */
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class PingAction extends ReadOnlyApiAction<Object>
    {
        @Override
        public ApiResponse execute(Object o, BindException errors) throws Exception
        {
            Module module = ModuleLoader.getInstance().getModule(FdahpUserRegWSModule.NAME);

            if (module == null)
                throw new NotFoundException(FdahpUserRegWSModule.NAME + " was not found!");

            ApiSimpleResponse apiSimpleResponse = new ApiSimpleResponse();
            apiSimpleResponse.put("response", "FdahpUserRegWebServices " + module.getReleaseVersion() + " Works!");
            apiSimpleResponse.put(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase(), true);
            return apiSimpleResponse;
        }
    }

    /**
     * Signup of an user
     */
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class RegisterAction extends MutatingApiAction<ParticipantForm>
    {
        @Override
        public Object execute(ParticipantForm participantForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            String applicationId = getViewContext().getRequest().getHeader("applicationId");
            language = getViewContext().getRequest().getHeader("language");
            String orgId = getViewContext().getRequest().getHeader("orgId");
            UserDetails userParticipantDetails = null;
            try
            {
                if ((participantForm.getEmailId() != null && StringUtils.isNotEmpty(participantForm.getEmailId())) && (participantForm.getPassword() != null && StringUtils.isNotEmpty(participantForm.getPassword()))
                        && (applicationId != null && StringUtils.isNotEmpty(applicationId))
                        && (orgId != null && StringUtils.isNotEmpty(orgId)))
                {
                    List<UserDetails> participantDetails = FdahpUserRegWSManager.get(language).getParticipantDetailsListByEmail(participantForm.getEmailId(), applicationId, orgId);
                    if (participantDetails != null && participantDetails.size() > 0)
                    {
                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.EMAIL_EXISTS.getValue(language), getViewContext().getResponse());
                        return null;
                    }
                    else
                    {
                        userParticipantDetails = FdahpUserRegWSManager.get(language).saveParticipant(getParticipant(participantForm, applicationId, orgId));
                        if (userParticipantDetails != null)
                        {
                            addUser(response, userParticipantDetails, applicationId, orgId, language);
                        }
                        else
                        {
                            FdahpUserRegWSManager.addAuditEvent(null, "User Registration Failure", "User Registration Failure  with  email " + userParticipantDetails.getEmail() + ".", "FdaUserAuditEvent", getContainer_AppID(applicationId, orgId).getId());
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(language), getViewContext().getResponse());
                            return null;
                        }
                    }
                }
                else
                {
                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                    if (StringUtils.trimToNull(participantForm.getEmailId()) == null)
                        errors.rejectValue("emailId", ERROR_MSG, "email is required.");
                    if (StringUtils.trimToNull(participantForm.getPassword()) == null)
                        errors.rejectValue("password", ERROR_MSG, "password is required.");
                    if (StringUtils.trimToNull(applicationId) == null)
                        errors.rejectValue("applicationId", ERROR_MSG, "applicationId is required.");
                    if (StringUtils.trimToNull(orgId) == null)
                        errors.rejectValue("orgId", ERROR_MSG, "orgId is required.");
                }
            }
            catch (Exception e)
            {
                _log.error("register action:", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;

            }
            return response;
        }
    }

    private void addUser(ApiSimpleResponse response, UserDetails userParticipantDetails, String applicationId, String orgId, String language)
    {
        response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase());
        response.put("userId", userParticipantDetails.getUserId());
        if (userParticipantDetails.getStatus() != null)
            if (userParticipantDetails.getStatus() == 2)
                response.put("verified", false);
        if (userParticipantDetails.getStatus() == 1)
            response.put("verified", true);
        if (userParticipantDetails.getId() != null)
        {
            AuthInfo authInfo = FdahpUserRegWSManager.get(language).saveAuthInfo(userParticipantDetails.getUserId(), true, applicationId, orgId);
            if (authInfo != null)
            {
                response.put("auth", authInfo.getAuthKey());
                response.put("refreshToken", authInfo.getRefreshToken());
            }

            //save orgid and appid for user start
            UserAppDetails userAppDetails = new UserAppDetails();
            userAppDetails.setUserId(userParticipantDetails.getUserId());
            userAppDetails.setOrgId(orgId);
            userAppDetails.setApplicationId(applicationId);
            userAppDetails.setCreatedOn(new Date());
            String message = FdahpUserRegWSManager.get(language).saveUserAppDetails(userAppDetails);
            //save orgId and appid for user end
        }
        AppPropertiesDetails appPropertiesDetails = FdahpUserRegWSManager.get(language).getAppPropertiesDetailsByAppId(applicationId, orgId);
        String message;
        String subject;
        if (appPropertiesDetails == null || appPropertiesDetails.getRegEmailSub() == null || appPropertiesDetails.getRegEmailBody() == null || appPropertiesDetails.getRegEmailBody().equalsIgnoreCase("") || appPropertiesDetails.getRegEmailSub().equalsIgnoreCase(""))
        {
            message = "<html>" +
                    "<body>" +
                    "<div style='margin:20px;padding:10px;font-family: sans-serif;font-size: 14px;'>" +
                    "<span>Hi,</span><br/><br/>" +
                    "<span>Thank you for registering with us! We look forward to having you on board and actively taking part in<br/>research studies conducted by the FDA and its partners.</span><br/><br/>" +
                    "<span>Your sign-up process is almost complete. Please use the verification code provided below to<br/>complete the Verification step in the mobile app. </span><br/><br/>" +
                    "<span><strong>Verification Code:</strong>" + userParticipantDetails.getSecurityToken() + "</span><br/><br/>" +
                    "<span>This code can be used only once and is valid for a period of 48 hours only.</span><br/><br/>" +
                    "<span>Please note that  registration (or sign up) for the app  is requested only to provide you with a <br/>seamless experience of using the app. Your registration information does not become part of <br/>the data collected for any study housed in the app. Each study has its own consent process <br/> and no data for any study will not be collected unless and until you provide an informed consent<br/> prior to joining the study </span><br/><br/>" +
                    "<span>For any questions or assistance, please write to <a>FDAMyStudiesContact@harvardpilgrim.org</a> </span><br/><br/>" +
                    "<span style='font-size:15px;'>Thanks,</span><br/><span>The FDA MyStudies Platform Team</span>" +
                    "<br/><span>----------------------------------------------------</span><br/>" +
                    "<span style='font-size:10px;'>PS - This is an auto-generated email. Please do not reply.</span>" +
                    "</div>" +
                    "</body>" +
                    "</html>";
            subject = "Welcome to the FDA MyStudies App!";
            //FdahpUserRegUtil.sendMessage("Welcome to the FDA MyStudies App!", message, userParticipantDetails.getEmail());
        }
        else
        {
            if (language != null && language.equalsIgnoreCase(LANGUAGE_SP))
            {
                message = appPropertiesDetails.getRegEmailBodySpanish().replace("<<< TOKEN HERE >>>", userParticipantDetails.getSecurityToken());
                subject = appPropertiesDetails.getRegEmailSubSpanish();
            }
            else
            {
                message = appPropertiesDetails.getRegEmailBody().replace("<<< TOKEN HERE >>>", userParticipantDetails.getSecurityToken());
                subject = appPropertiesDetails.getRegEmailSub();
            }
        }
        FdahpUserRegUtil.sendMessage(subject, message, userParticipantDetails.getEmail(), appPropertiesDetails);
        FdahpUserRegWSManager.addAuditEvent(userParticipantDetails.getUserId(), "User Registration Success", "User Registration Success  with  email " + userParticipantDetails.getEmail() + ".", "FdaUserAuditEvent", getContainer_AppID(applicationId, orgId).getId());
        response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase());
    }

    public static class UserForm
    {
        public String _userId;
        public String _reason;

        public String getUserId()
        {
            return _userId;
        }

        public void setUserId(String userId)
        {
            _userId = userId;
        }

        public String getReason()
        {
            return _reason;
        }

        public void setReason(String reason)
        {
            _reason = reason;
        }
    }

    /**
     * Checking for email is verified or not
     */
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class ConfirmRegistrationAction extends ReadOnlyApiAction
    {
        @Override
        public Object execute(Object o, BindException errors) throws Exception
        {
            language = getViewContext().getRequest().getHeader("language");
            ApiSimpleResponse response = new ApiSimpleResponse();
            try
            {
                String userId = getViewContext().getRequest().getHeader("userId");
                String auth = getViewContext().getRequest().getHeader("auth");
                String applicationId = getViewContext().getRequest().getHeader("applicationId");
                String orgId = getViewContext().getRequest().getHeader("orgId");

                boolean isAuthenticated = false;
                if (auth != null && StringUtils.isNotEmpty(auth) && applicationId != null && StringUtils.isNotEmpty(applicationId)
                        && orgId != null && StringUtils.isNotEmpty(orgId))
                {
                    isAuthenticated = FdahpUserRegWSManager.get(language).validatedAuthKey(auth, applicationId, orgId);
                    if (isAuthenticated)
                    {
                        if (userId != null && StringUtils.isNotEmpty(userId))
                        {
                            UserDetails participantDetails = FdahpUserRegWSManager.get(language).getParticipantDetails(userId, applicationId, orgId);
                            if (participantDetails != null)
                            {
                                if (participantDetails.getStatus() == 2)
                                    response.put("verified", false);
                                if (participantDetails.getStatus() == 1)
                                    response.put("verified", true);
                                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase());

                            }
                            else
                            {
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.NO_DATA_AVAILABLE.getValue(language), FdahpUserRegUtil.ErrorCodes.NO_DATA_AVAILABLE.getValue(language), getViewContext().getResponse());
                                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(language).toLowerCase());
                            }
                        }
                        else
                        {
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                            return null;
                        }
                    }
                    else
                    {
                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }
                }

            }
            catch (Exception e)
            {
                _log.error("ConfirmRegistration action:", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }

    public static class VerificationForm
    {
        public String _emailId;
        public String _code;

        public String getEmailId()
        {
            return _emailId;
        }

        public void setEmailId(String emailId)
        {
            _emailId = emailId;
        }

        public String getCode()
        {
            return _code;
        }

        public void setCode(String code)
        {
            _code = code;
        }
    }

    /**
     * Verification  of an register email
     */
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class VerifyAction extends MutatingApiAction<VerificationForm>
    {
        @Override
        public Object execute(VerificationForm verificationForm, BindException errors) throws Exception
        {
            language = getViewContext().getRequest().getHeader("language");
            ApiSimpleResponse response = new ApiSimpleResponse();
            try
            {
                String applicationId = getViewContext().getRequest().getHeader("applicationId");
                String orgId = getViewContext().getRequest().getHeader("orgId");
                if (verificationForm != null)
                {
                    if ((verificationForm.getEmailId() != null && StringUtils.isNotEmpty(verificationForm.getEmailId())) &&
                            (verificationForm.getCode() != null && StringUtils.isNotEmpty(verificationForm.getCode()))
                            && applicationId != null && StringUtils.isNotEmpty(applicationId) && orgId != null && StringUtils.isNotEmpty(orgId))
                    {

                        String message = FdahpUserRegWSManager.get(language).validatedUserAppDetailsByAllApi("", verificationForm.getEmailId(), applicationId, orgId);
                        if (StringUtils.isNotEmpty(message) && message.equals(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                        {

                            UserDetails participantDetails = FdahpUserRegWSManager.get(language).getParticipantDetailsByEmail(verificationForm.getEmailId(), applicationId, orgId);
                            if (null != participantDetails)
                            {
                                if (participantDetails.getSecurityToken() != null && participantDetails.getSecurityToken().equalsIgnoreCase(verificationForm.getCode()))
                                {
                                    if (participantDetails.getStatus() == 2)
                                    {
                                        String hours = (String) configProp.get("verification.expiration.in.hour");
                                        Date validateDate = FdahpUserRegUtil.addHours(FdahpUserRegUtil.getCurrentDateTime(), Integer.parseInt(hours));
                                        if (participantDetails.getVerificationDate().before(validateDate) || participantDetails.getVerificationDate().equals(validateDate))
                                        {
                                            participantDetails.setStatus(1);
                                            participantDetails.setVerificationDate(FdahpUserRegUtil.getCurrentUtilDateTime());
                                            UserDetails updateParticipantDetails = FdahpUserRegWSManager.get(language).saveParticipant(participantDetails);
                                            if (null != updateParticipantDetails)
                                            {
                                                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase());
                                                FdahpUserRegWSManager.addAuditEvent(participantDetails.getUserId(), "User Verification", "User has confirmed registration through email " + verificationForm.getEmailId() + ".", "FdaUserAuditEvent", getContainer_AppID(applicationId, orgId).getId());
                                            }
                                            else
                                            {
                                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                                                return null;
                                            }
                                        }
                                        else
                                        {
                                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_103.getValue(language), FdahpUserRegUtil.ErrorCodes.CODE_EXPIRED.getValue(language), FdahpUserRegUtil.ErrorCodes.CODE_EXPIRED.getValue(language), getViewContext().getResponse());
                                            return null;
                                        }
                                    }
                                    else
                                    {
                                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_103.getValue(language), FdahpUserRegUtil.ErrorCodes.USER_ALREADY_VERIFIED.getValue(language), FdahpUserRegUtil.ErrorCodes.USER_ALREADY_VERIFIED.getValue(language), getViewContext().getResponse());
                                        return null;
                                    }

                                }
                                else
                                {
                                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_CODE.getValue(language), getViewContext().getResponse());
                                    return null;
                                }
                            }
                            else
                            {
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.EMAIL_NOT_EXISTS.getValue(language), FdahpUserRegUtil.ErrorCodes.EMAIL_NOT_EXISTS.getValue(language), getViewContext().getResponse());
                                return null;
                            }
                        }
                        else
                        {
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                            return null;
                        }
                    }
                    else
                    {
                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }
                }
                else
                {
                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                    return null;
                }
            }
            catch (Exception e)
            {
                _log.error("ConfirmRegistration action:", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }

    /**
     * login into application
     */
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class LoginAction extends MutatingApiAction<LoginForm>
    {
        @Override
        public ApiResponse execute(LoginForm loginForm, BindException errors) throws Exception
        {
            language = getViewContext().getRequest().getHeader("language");
            ApiSimpleResponse response = new ApiSimpleResponse();
            UserDetails participantDetails = null;
            String applicationId = getViewContext().getRequest().getHeader("applicationId");
            String orgId = getViewContext().getRequest().getHeader("orgId");
            int maxAttemptsCount = Integer.valueOf((String) configProp.get("max.login.attempts"));
            try
            {
                if (loginForm != null)
                {
                    if ((loginForm.getEmailId() != null && StringUtils.isNotEmpty(loginForm.getEmailId())) && (loginForm.getPassword() != null && StringUtils.isNotEmpty(loginForm.getPassword()))
                            && (applicationId != null && StringUtils.isNotEmpty(applicationId))
                            && (orgId != null && StringUtils.isNotEmpty(orgId)))
                    {
                        participantDetails = FdahpUserRegWSManager.get(language).getParticipantDetailsByEmail(loginForm.getEmailId(), applicationId, orgId);
                        if (null != participantDetails)
                        {
                            //check the org id exist or not if there allow same app or differnt app , not then throw the error
                            String errorMessage = FdahpUserRegWSManager.get(language).validatedUserAppDetailsByAllApi(participantDetails.getUserId(), participantDetails.getEmail(), applicationId, orgId);
                            if (StringUtils.isNotEmpty(errorMessage) && !errorMessage.equals(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                            {
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), errorMessage, getViewContext().getResponse());
                                return null;

                            }

                            LoginAttempts loginAttempts = FdahpUserRegWSManager.get(language).getLoginAttempts(loginForm.getEmailId());

                            if (loginAttempts != null && loginAttempts.getAttempts() == maxAttemptsCount)
                            {
                                int count = Integer.valueOf((String) configProp.get("expiration.login.attempts.minute"));
                                Date attemptsExpireDate = FdahpUserRegUtil.addMinutes(loginAttempts.getLastModified().toString(), count);
                                if (attemptsExpireDate.before(FdahpUserRegUtil.getCurrentUtilDateTime()) || attemptsExpireDate.equals(FdahpUserRegUtil.getCurrentUtilDateTime()))
                                {
                                    FdahpUserRegWSManager.get(language).resetLoginAttempts(loginForm.getEmailId());
                                    response = getLoginInformation(participantDetails, loginForm.getEmailId(), loginForm.getPassword(), maxAttemptsCount, applicationId, orgId, language);
                                }
                                else
                                {
                                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.ACCOUNT_LOCKED.name(), FdahpUserRegUtil.ErrorCodes.ACCOUNT_LOCKED.getValue(language), getViewContext().getResponse());
                                    return null;
                                }

                            }
                            else
                            {
                                response = getLoginInformation(participantDetails, loginForm.getEmailId(), loginForm.getPassword(), maxAttemptsCount, applicationId, orgId, language);
                            }
                        }
                        else
                        {
                            FdahpUserRegWSManager.addAuditEvent(null, "FAILED SIGN IN", "Wrong information entered in email " + loginForm.getEmailId() + ". Which is not existed.", "FdaUserAuditEvent", getContainer_AppID(applicationId, orgId).getId());
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_CREDENTIALS.name(), FdahpUserRegUtil.ErrorCodes.INVALID_CREDENTIALS.getValue(language), getViewContext().getResponse());
                            return null;
                        }
                    }
                    else
                    {
                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.name(), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }
                }
                else
                {
                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.name(), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                    return null;
                }
            }
            catch (Exception e)
            {
                _log.error("Login Action:", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.name(), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }

    /**
     * Get the Login information of registered user
     *
     * @param participantDetails
     * @param email
     * @param password
     * @param maxAttemptsCount
     * @return
     */
    public ApiSimpleResponse getLoginInformation(UserDetails participantDetails, String email, String password, int maxAttemptsCount, String applicationId, String orgId, String language)
    {
        ApiSimpleResponse response = new ApiSimpleResponse();
        if (participantDetails.getPassword() != null && participantDetails.getPassword().equalsIgnoreCase(FdahpUserRegUtil.getEncryptedString(password)))
        {
            if (participantDetails.getTempPassword())
            {
                participantDetails.setResetPassword(null);
                participantDetails.setTempPassword(false);
                participantDetails.setTempPasswordDate(FdahpUserRegUtil.getCurrentUtilDateTime());
                FdahpUserRegWSManager.get(language).saveParticipant(participantDetails);
            }
            AuthInfo authInfo = FdahpUserRegWSManager.get(language).saveAuthInfo(participantDetails.getUserId(), true, applicationId, orgId);
            if (authInfo != null)
            {
                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase());
                response.put("userId", participantDetails.getUserId());
                response.put("auth", authInfo.getAuthKey());
                response.put("refreshToken", authInfo.getRefreshToken());
                if (participantDetails.getStatus() == 2)
                {
                    response.put("verified", false);
                }
                if (participantDetails.getStatus() == 1)
                {
                    response.put("verified", true);
                }
                if (participantDetails.getPasswordUpdatedDate() != null)
                {
                    String days = (String) configProp.get("password.expiration.in.day");
                    Date expiredDate = FdahpUserRegUtil.addDays(FdahpUserRegUtil.getCurrentDateTime(), Integer.parseInt(days));
                    _log.info("expiredDate:" + expiredDate + "participantDetails.getPasswordUpdatedDate():" + participantDetails.getPasswordUpdatedDate());
                    if (expiredDate.before(participantDetails.getPasswordUpdatedDate()) || expiredDate.equals(participantDetails.getPasswordUpdatedDate()))
                    {
                        response.put("resetPassword", true);
                    }
                }
                FdahpUserRegWSManager.get(language).resetLoginAttempts(email);
                FdahpUserRegWSManager.addAuditEvent(participantDetails.getUserId(), "SIGN IN", "User Signed In.(User ID =  " + participantDetails.getUserId() + ").", "FdaUserAuditEvent", getContainer_AppID(applicationId, orgId).getId());
            }
            else
            {
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.name(), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;
            }

        }
        else if (participantDetails.getResetPassword() != null && participantDetails.getResetPassword().equalsIgnoreCase(FdahpUserRegUtil.getEncryptedString(password)))
        {
            if (participantDetails.getTempPassword())
            {
                String hours = (String) configProp.get("verification.expiration.in.hour");
                Date validateDate = FdahpUserRegUtil.addHours(FdahpUserRegUtil.getCurrentDateTime(), Integer.parseInt(hours));
                if (participantDetails.getTempPasswordDate().before(validateDate) || participantDetails.getTempPasswordDate().equals(validateDate))
                {
                    AuthInfo authInfo = FdahpUserRegWSManager.get(language).saveAuthInfo(participantDetails.getUserId(), true, applicationId, orgId);
                    if (authInfo != null)
                    {
                        response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase());
                        response.put("userId", participantDetails.getUserId());
                        response.put("auth", authInfo.getAuthKey());
                        response.put("refreshToken", authInfo.getRefreshToken());
                        if (participantDetails.getStatus() == 2)
                        {
                            response.put("verified", false);
                        }
                        if (participantDetails.getStatus() == 1)
                        {
                            response.put("verified", true);
                        }
                        response.put("resetPassword", participantDetails.getTempPassword());
                        FdahpUserRegWSManager.get(language).resetLoginAttempts(email);
                        FdahpUserRegWSManager.addAuditEvent(participantDetails.getUserId(), "SIGN IN", "User Signed In.(User ID =  " + participantDetails.getUserId() + ") with temp password.", "FdaUserAuditEvent", getContainer_AppID(applicationId, orgId).getId());
                    }
                    else
                    {
                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.name(), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }
                }
                else
                {
                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_103.getValue(language), FdahpUserRegUtil.ErrorCodes.CODE_EXPIRED.getValue(language), FdahpUserRegUtil.ErrorCodes.CODE_EXPIRED.getValue(language), getViewContext().getResponse());
                    return null;
                }
            }
        }
        else
        {
            FdahpUserRegWSManager.addAuditEvent(participantDetails.getUserId(), "FAILED SIGN IN", "User Sign-In Failed. (User ID = " + participantDetails.getUserId() + ")", "FdaUserAuditEvent", getContainer_AppID(applicationId, orgId).getId());
            LoginAttempts failAttempts = FdahpUserRegWSManager.get(language).updateLoginFailureAttempts(email, applicationId, orgId);
            _log.info("maxAttemptsCount:" + maxAttemptsCount);
            if (failAttempts != null && failAttempts.getAttempts() == maxAttemptsCount)
            {
                _log.info("failAttempts:" + failAttempts.getAttempts() + "maxAttemptsCount:" + maxAttemptsCount);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.ACCOUNT_LOCKED.name(), FdahpUserRegUtil.ErrorCodes.ACCOUNT_LOCKED.getValue(language), getViewContext().getResponse());
                return null;
            }
            else
            {
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_USERNAME_PASSWORD_MSG.name(), FdahpUserRegUtil.ErrorCodes.INVALID_USERNAME_PASSWORD_MSG.getValue(language), getViewContext().getResponse());
                return null;
            }
        }
        return response;
    }

    /**
     * Get the UserDetails based on the userId
     *
     * @param form
     * @return UserDetails
     */
    private UserDetails getParticipant(ParticipantForm form, String applicationId, String orgId)
    {
        UserDetails participantDetails = null;
        if (null != form.getUserId())
        {
            participantDetails = FdahpUserRegWSManager.get(language).getParticipantDetails(form.getUserId(), applicationId, orgId);
        }
        if (participantDetails == null)
        {
            participantDetails = new UserDetails();
            participantDetails.setStatus(2);
            String token = RandomStringUtils.randomAlphanumeric(6);
            participantDetails.setSecurityToken(token);
            participantDetails.setVerificationDate(FdahpUserRegUtil.getCurrentUtilDateTime());
            participantDetails.setPasswordUpdatedDate(FdahpUserRegUtil.getCurrentUtilDateTime());
            String userId = UUID.randomUUID().toString();
            participantDetails.setUserId(userId);
        }
        if (form.getFirstName() != null)
            participantDetails.setFirstName(form.getFirstName());
        if (form.getLastName() != null)
            participantDetails.setLastName(form.getLastName());
        if (form.getEmailId() != null)
            participantDetails.setEmail(form.getEmailId());
        if (form.getPassword() != null)
            participantDetails.setPassword(FdahpUserRegUtil.getEncryptedString(form.getPassword()));
        if (form.getUsePassCode() != null)
            participantDetails.setUsePassCode(form.getUsePassCode());
        if (form.getLocalNotification() != null)
            participantDetails.setLocalNotificationFlag(form.getLocalNotification());
        if (form.getRemoteNotification() != null)
            participantDetails.setRemoteNotificationFlag(form.getRemoteNotification());
        if (form.getTouchId() != null)
            participantDetails.setTouchId(form.getTouchId());

        participantDetails.setApplicationId(applicationId);
        participantDetails.setOrgId(orgId);
        return participantDetails;
    }

    /**
     * Forgot password of an register email
     */
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class ForgotPasswordAction extends MutatingApiAction<LoginForm>
    {
        @Override
        public ApiResponse execute(LoginForm loginForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            int maxAttemptsCount = Integer.valueOf((String) configProp.get("max.login.attempts"));
            String applicationId = getViewContext().getRequest().getHeader("applicationId");
            String orgId = getViewContext().getRequest().getHeader("orgId");
            language = getViewContext().getRequest().getHeader("language");
            try
            {
                if (loginForm != null && loginForm.getEmailId() != null && StringUtils.isNotEmpty(loginForm.getEmailId())
                        && applicationId != null && StringUtils.isNotEmpty(applicationId) && orgId != null && StringUtils.isNotEmpty(orgId))
                {

                    String validAppMessage = FdahpUserRegWSManager.get(language).validatedUserAppDetailsByAllApi("", loginForm.getEmailId(), applicationId, orgId);
                    if (StringUtils.isNotEmpty(validAppMessage) && validAppMessage.equals(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                    {
                        UserDetails participantDetails = FdahpUserRegWSManager.get(language).getParticipantDetailsByEmail(loginForm.getEmailId(), applicationId, orgId);
                        if (participantDetails != null)
                        {
                            if (participantDetails.getStatus() == 1)
                            {
                                boolean isValid = true;
                                LoginAttempts loginAttempts = FdahpUserRegWSManager.get(language).getLoginAttempts(loginForm.getEmailId());
                                if (loginAttempts != null && loginAttempts.getAttempts() == maxAttemptsCount)
                                {
                                    int count = Integer.valueOf((String) configProp.get("expiration.login.attempts.minute"));
                                    Date attemptsExpireDate = FdahpUserRegUtil.addMinutes(loginAttempts.getLastModified().toString(), count);
                                    if (attemptsExpireDate.before(FdahpUserRegUtil.getCurrentUtilDateTime()) || attemptsExpireDate.equals(FdahpUserRegUtil.getCurrentUtilDateTime()))
                                    {
                                        isValid = true;
                                    }
                                    else
                                    {
                                        isValid = false;
                                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.ACCOUNT_TEMP_LOCKED.name(), FdahpUserRegUtil.ErrorCodes.ACCOUNT_TEMP_LOCKED.getValue(language), getViewContext().getResponse());
                                        return null;
                                    }
                                }
                                else
                                {
                                    isValid = true;
                                }
                                UserDetails upParticipantDetails = null;
                                String tempPassword = RandomStringUtils.randomAlphanumeric(6);
                                if (isValid)
                                {
                                    participantDetails.setTempPassword(true);
                                    participantDetails.setResetPassword(FdahpUserRegUtil.getEncryptedString(tempPassword));
                                    participantDetails.setTempPasswordDate(FdahpUserRegUtil.getCurrentUtilDateTime());
                                    upParticipantDetails = FdahpUserRegWSManager.get(language).saveParticipant(participantDetails);
                                }
                                if (upParticipantDetails != null)
                                {
                                    AppPropertiesDetails appPropertiesDetails = FdahpUserRegWSManager.get(language).getAppPropertiesDetailsByAppId(applicationId, orgId);
                                    String message = "", subject = "";
                                    if (appPropertiesDetails == null || appPropertiesDetails.getForgotEmailSub() == null || appPropertiesDetails.getForgotEmailBody() == null || appPropertiesDetails.getForgotEmailBody().equalsIgnoreCase("") || appPropertiesDetails.getForgotEmailSub().equalsIgnoreCase(""))
                                    {
                                        message = "<html>" +
                                                "<body>" +
                                                "<div style='margin:20px;padding:10px;font-family: sans-serif;font-size: 14px;'>" +
                                                "<span>Hi,</span><br/><br/>" +
                                                "<span>Thank you for reaching out for password help.</span><br/><br/>" +
                                                "<span>Here is a temporary password which you can use to sign in to the FDA MyStudies App.<br/> You will be required to set up a new password after signing in.</span><br/><br/>" +
                                                "<span><strong>Temporary Password:</strong> " + tempPassword + "</span><br/><br/>" +
                                                "<span>Please note that this temporary password can be used only once and is valid for a period of 48 hours only.</span><br/><br/>" +
                                                "<span>For any questions or assistance, please write to <a>FDAMyStudiesContact@harvardpilgrim.org</a> </span><br/><br/>" +
                                                "<span style='font-size:15px;'>Thanks,</span><br/><span>The FDA MyStudies Platform Team</span>" +
                                                "<br/><span>----------------------------------------------------</span><br/>" +
                                                "<span style='font-size:10px;'>PS - This is an auto-generated email. Please do not reply. In case you did not request password help, please visit the app and change your password as a precautionary measure.</span>" +
                                                "</div>" +
                                                "</body>" +
                                                "</html>";
                                        subject = "Password Help - FDA MyStudies App!";
                                        //FdahpUserRegUtil.sendMessage("Password Help - FDA MyStudies App!", message, participantDetails.getEmail());
                                    }
                                    else
                                    {
                                        if (language != null && language.equalsIgnoreCase(LANGUAGE_SP))
                                        {
                                            message = appPropertiesDetails.getForgotEmailBodySpanish().replace("<<< TOKEN HERE >>>", tempPassword);
                                            subject = appPropertiesDetails.getForgotEmailSubSpanish();
                                        }
                                        else
                                        {
                                            message = appPropertiesDetails.getForgotEmailBody().replace("<<< TOKEN HERE >>>", tempPassword);
                                            subject = appPropertiesDetails.getForgotEmailSub();
                                        }
                                    }
                                    FdahpUserRegUtil.sendMessage(subject, message, participantDetails.getEmail(), appPropertiesDetails);
                                    response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase());
                                    FdahpUserRegWSManager.get(language).resetLoginAttempts(loginForm.getEmailId());
                                    FdahpUserRegWSManager.addAuditEvent(participantDetails.getUserId(), "PASSWORD HELP", "Password Help sent to user.(User ID = " + participantDetails.getUserId() + ")", "FdaUserAuditEvent", getContainer_AppID(applicationId, orgId).getId());
                                }
                                else
                                {
                                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.EMAIL_NOT_VERIFIED.getValue(language), FdahpUserRegUtil.ErrorCodes.EMAIL_NOT_VERIFIED.getValue(language), getViewContext().getResponse());
                                    return null;
                                }
                            }
                            else
                            {
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_103.getValue(language), FdahpUserRegUtil.ErrorCodes.EMAIL_NOT_VERIFIED.getValue(language), FdahpUserRegUtil.ErrorCodes.EMAIL_NOT_VERIFIED.getValue(language), getViewContext().getResponse());
                                return null;
                            }


                        }
                        else
                        {
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_CREDENTIALS.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_CREDENTIALS.getValue(language), getViewContext().getResponse());
                            errors.rejectValue("emailId", ERROR_MSG, FdahpUserRegUtil.ErrorCodes.INVALID_CREDENTIALS.getValue(language));
                        }
                    }
                    else
                    {
                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }

                }
                else
                {
                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                    return null;
                }
            }
            catch (Exception e)
            {
                _log.error("ForgotPassword Action Error:", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }

    /**
     * Resending the confirmation email
     */
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class ResendConfirmationAction extends MutatingApiAction<LoginForm>
    {

        @Override
        public ApiResponse execute(LoginForm loginForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            String auth = getViewContext().getRequest().getHeader("auth");
            boolean isAuthenticated = false;
            String code = "";
            String applicationId = getViewContext().getRequest().getHeader("applicationId");
            String orgId = getViewContext().getRequest().getHeader("orgId");
            language = getViewContext().getRequest().getHeader("language");
            try
            {
                if (loginForm != null && loginForm.getEmailId() != null && StringUtils.isNotEmpty(loginForm.getEmailId())
                        && applicationId != null && StringUtils.isNotEmpty(applicationId) && orgId != null && StringUtils.isNotEmpty(orgId))
                {

                    String isValidAppMsg = FdahpUserRegWSManager.get(language).validatedUserAppDetailsByAllApi("", loginForm.getEmailId(), applicationId, orgId);

                    if (StringUtils.isNotEmpty(isValidAppMsg) && isValidAppMsg.equals(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                    {

                        UserDetails participantDetails = FdahpUserRegWSManager.get(language).getParticipantDetailsByEmail(loginForm.getEmailId(), applicationId, orgId);
                        if (participantDetails != null)
                        {
                            if (participantDetails.getStatus() == 2)
                            {
                                AppPropertiesDetails appPropertiesDetails = FdahpUserRegWSManager.get(language).getAppPropertiesDetailsByAppId(applicationId, orgId);
                                code = RandomStringUtils.randomAlphanumeric(6);
                                participantDetails.setSecurityToken(code);
                                participantDetails.setVerificationDate(FdahpUserRegUtil.getCurrentUtilDateTime());
                                FdahpUserRegWSManager.get(language).saveParticipant(participantDetails);

                                String message;
                                String subject;
                                if (appPropertiesDetails == null || appPropertiesDetails.getRegEmailSub() == null || appPropertiesDetails.getRegEmailBody() == null || appPropertiesDetails.getRegEmailBody().equalsIgnoreCase("") || appPropertiesDetails.getRegEmailSub().equalsIgnoreCase(""))
                                {
                                    message = "<html>" +
                                            "<body>" +
                                            "<div style='margin:20px;padding:10px;font-family: sans-serif;font-size: 14px;'>" +
                                            "<span>Hi,</span><br/><br/>" +
                                            "<span>Thank you for registering with us! We look forward to having you on board and actively taking part in<br/>research studies conducted by the FDA and its partners.</span><br/><br/>" +
                                            "<span>Your sign-up process is almost complete. Please use the verification code provided below to<br/>complete the Verification step in the mobile app.</span><br/><br/>" +
                                            "<span><strong>Verification Code:</strong>" + participantDetails.getSecurityToken() + "</span><br/><br/>" +
                                            "<span>This code can be used only once and is valid for a period of 48 hours only.</span><br/><br/>" +
                                            "<span>Please note that  registration (or sign up) for the app  is requested only to provide you with a <br/>seamless experience of using the app. Your registration information does not become part of <br/>the data collected for any study housed in the app.Each study has its own consent process, <br/>and no data for any study will be collected unless and until you provide an informed consent<br/> prior to joining the study. </span><br/><br/>" +
                                            "<span>For any questions or assistance, please write to <a>FDAMyStudiesContact@harvardpilgrim.org</a> </span><br/><br/>" +
                                            "<span style='font-size:15px;'>Thanks,</span><br/><span>The FDA MyStudies Platform Team</span>" +
                                            "<br/><span>----------------------------------------------------</span><br/>" +
                                            "<span style='font-size:10px;'>PS - This is an auto-generated email. Please do not reply.</span>" +
                                            "</div>" +
                                            "</body>" +
                                            "</html>";
                                    subject = "Welcome to the FDA MyStudies App!";
                                    //FdahpUserRegUtil.sendMessage("Welcome to the FDA MyStudies App!", message, participantDetails.getEmail());
                                }
                                else
                                {
                                    if(language!=null && language.equalsIgnoreCase(LANGUAGE_SP)){
                                        message = appPropertiesDetails.getRegEmailBodySpanish().replace("<<< TOKEN HERE >>>", participantDetails.getSecurityToken());
                                        subject = appPropertiesDetails.getRegEmailSubSpanish();
                                    }else{
                                        message = appPropertiesDetails.getRegEmailBody().replace("<<< TOKEN HERE >>>", participantDetails.getSecurityToken());
                                        subject = appPropertiesDetails.getRegEmailSub();
                                    }

                                }
                                FdahpUserRegUtil.sendMessage(subject, message, participantDetails.getEmail(), appPropertiesDetails);
                                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase());
                                FdahpUserRegWSManager.addAuditEvent(participantDetails.getUserId(), "Requested Confirmation mail", "Confirmation mail has been sent again to" + participantDetails.getEmail() + ".", "FdaUserAuditEvent", getContainer_AppID(applicationId, orgId).getId());
                            }
                            else
                            {
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_103.getValue(language), FdahpUserRegUtil.ErrorCodes.USER_ALREADY_VERIFIED.getValue(language), FdahpUserRegUtil.ErrorCodes.USER_ALREADY_VERIFIED.getValue(language), getViewContext().getResponse());
                                return null;
                            }

                        }
                        else
                        {
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.EMAIL_NOT_EXISTS.getValue(language), FdahpUserRegUtil.ErrorCodes.EMAIL_NOT_EXISTS.getValue(language), getViewContext().getResponse());
                            return null;
                        }

                    }
                    else
                    {
                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }

                }
                else
                {
                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                    return null;
                }

            }
            catch (Exception e)
            {
                _log.error("ResendConfirmationAction Action Error:", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }

    /**
     * Updating the user password
     */
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class ChangePasswordAction extends MutatingApiAction<ChangePasswordForm>
    {
        @Override
        public ApiResponse execute(ChangePasswordForm form, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            String auth = getViewContext().getRequest().getHeader("auth");
            boolean isAuthenticated = false;
            List<PasswordHistory> passwordHistories = null;
            Boolean isValidPassword = true;
            String applicationId = getViewContext().getRequest().getHeader("applicationId");
            String orgId = getViewContext().getRequest().getHeader("orgId");
            String userId = getViewContext().getRequest().getHeader("userId");
            language = getViewContext().getRequest().getHeader("language");
            try
            {
                if (auth != null && StringUtils.isNotEmpty(auth) && applicationId != null && StringUtils.isNotEmpty(applicationId)
                        && orgId != null && StringUtils.isNotEmpty(orgId))
                {
                    String appMessage = FdahpUserRegWSManager.get(language).validatedUserAppDetailsByAllApi(userId, "", applicationId, orgId);
                    if (StringUtils.isNotEmpty(appMessage) && appMessage.equals(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                    {

                        isAuthenticated = FdahpUserRegWSManager.get(language).validatedAuthKey(auth, applicationId, orgId);
                        if (isAuthenticated)
                        {
                            String oldPassword = form.getCurrentPassword();
                            String newPassword = form.getNewPassword();

                            if ((oldPassword != null && StringUtils.isNotEmpty(oldPassword)) && (newPassword != null && StringUtils.isNotEmpty(newPassword)))
                            {
                                UserDetails participantDetails = FdahpUserRegWSManager.get(language).getParticipantDetails(userId, applicationId, orgId);
                                if (participantDetails != null)
                                {
                                    if ((participantDetails.getPassword() != null && participantDetails.getPassword().equalsIgnoreCase(FdahpUserRegUtil.getEncryptedString(oldPassword))) || (participantDetails.getResetPassword() != null && participantDetails.getResetPassword().equalsIgnoreCase(FdahpUserRegUtil.getEncryptedString(oldPassword))))
                                    {
                                        if (!oldPassword.equals(newPassword))
                                        {
                                            passwordHistories = FdahpUserRegWSManager.get(language).getPasswordHistoryList(userId);
                                            if (passwordHistories != null && !passwordHistories.isEmpty())
                                            {
                                                for (PasswordHistory userPasswordHistory : passwordHistories)
                                                {
                                                    if (FdahpUserRegUtil.getEncryptedString(newPassword).equalsIgnoreCase(userPasswordHistory.getPassword()))
                                                    {
                                                        isValidPassword = false;
                                                        break;
                                                    }
                                                }
                                            }
                                            if (isValidPassword)
                                            {
                                                participantDetails.setPassword(FdahpUserRegUtil.getEncryptedString(newPassword));
                                                if (participantDetails.getTempPassword())
                                                    participantDetails.setTempPassword(false);
                                                participantDetails.setResetPassword(null);
                                                participantDetails.setTempPasswordDate(FdahpUserRegUtil.getCurrentUtilDateTime());
                                                participantDetails.setPasswordUpdatedDate(FdahpUserRegUtil.getCurrentUtilDateTime());
                                                UserDetails updParticipantDetails = FdahpUserRegWSManager.get(language).saveParticipant(participantDetails);
                                                if (updParticipantDetails != null && !participantDetails.getTempPassword())
                                                {
                                                    String message = FdahpUserRegWSManager.get(language).savePasswordHistory(userId, FdahpUserRegUtil.getEncryptedString(newPassword), applicationId, orgId);
                                                    if (message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                                                        response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase());
                                                    FdahpUserRegWSManager.addAuditEvent(participantDetails.getUserId(), "Change Password", "User password changed successfully " + participantDetails.getEmail() + ".", "FdaUserAuditEvent", getContainer_AppID(applicationId, orgId).getId());

                                                }

                                            }
                                            else
                                            {
                                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.NEW_PASSWORD_NOT_SAME_LAST_PASSWORD.getValue(language), FdahpUserRegUtil.ErrorCodes.NEW_PASSWORD_NOT_SAME_LAST_PASSWORD.getValue(language), getViewContext().getResponse());
                                                errors.rejectValue("currentPassword", ERROR_MSG, FdahpUserRegUtil.ErrorCodes.NEW_PASSWORD_NOT_SAME_LAST_PASSWORD.getValue(language));
                                            }

                                        }
                                        else
                                        {
                                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.OLD_PASSWORD_AND_NEW_PASSWORD_NOT_SAME.getValue(language), getViewContext().getResponse());
                                            errors.rejectValue("currentPassword", ERROR_MSG, FdahpUserRegUtil.ErrorCodes.OLD_PASSWORD_AND_NEW_PASSWORD_NOT_SAME.getValue(language));
                                        }

                                    }
                                    else
                                    {
                                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.OLD_PASSWORD_NOT_EXISTS.getValue(language), getViewContext().getResponse());
                                        errors.rejectValue("currentPassword", ERROR_MSG, FdahpUserRegUtil.ErrorCodes.OLD_PASSWORD_NOT_EXISTS.getValue(language));
                                    }
                                }
                                else
                                {
                                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                                    return null;
                                }

                            }
                            else
                            {
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                                return null;
                            }
                        }
                        else
                        {
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(language), getViewContext().getResponse());
                            return null;
                        }
                    }
                    else
                    {
                        if (appMessage.equals(FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language)))
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        else
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }

                }
                else
                {
                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                    return null;
                }
            }
            catch (Exception e)
            {
                _log.error("ChangePassword Action Error", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }

    public static class ChangePasswordForm extends ReturnUrlForm
    {
        private String _currentPassword;
        private String _newPassword;
        private String _userId;

        public String getCurrentPassword()
        {
            return _currentPassword;
        }

        public void setCurrentPassword(String currentPassword)
        {
            _currentPassword = currentPassword;
        }

        public String getNewPassword()
        {
            return _newPassword;
        }

        public void setNewPassword(String newPassword)
        {
            _newPassword = newPassword;
        }

        public String getUserId()
        {
            return _userId;
        }

        public void setUserId(String userId)
        {
            _userId = userId;
        }
    }

    /**
     * Logout from app
     */
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    @MethodsAllowed(HttpUtil.Method.DELETE)
    public class LogoutAction extends MutatingApiAction<UserForm>
    {

        @Override
        public Object execute(UserForm userForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            boolean isAuthenticated = false;
            language = getViewContext().getRequest().getHeader("language");
            String message = FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(language);
            try
            {
                if (isDelete())
                {
                    String auth = getViewContext().getRequest().getHeader("auth");
                    String userId = getViewContext().getRequest().getHeader("userId");
                    String applicationId = getViewContext().getRequest().getHeader("applicationId");
                    String orgId = getViewContext().getRequest().getHeader("orgId");

                    if (auth != null && StringUtils.isNotEmpty(auth) &&
                            applicationId != null && StringUtils.isNotEmpty(applicationId) && orgId != null && StringUtils.isNotEmpty(orgId))
                    {
                        String appMessage = FdahpUserRegWSManager.get(language).validatedUserAppDetailsByAllApi(userId, "", applicationId, orgId);
                        if (StringUtils.isNotEmpty(appMessage) && appMessage.equals(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                        {

                            isAuthenticated = FdahpUserRegWSManager.get(language).validatedAuthKey(auth, applicationId, orgId);
                            if (isAuthenticated)
                            {
                                if (null != userId && StringUtils.isNotEmpty(userId))
                                {
                                    message = FdahpUserRegWSManager.get(language).signout(userId, applicationId, orgId);
                                    if (message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                                    {
                                        response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase());
                                        FdahpUserRegWSManager.addAuditEvent(userId, "SIGN OUT", "User Signed Out. (User ID = " + userId + ") ", "FdaUserAuditEvent", getContainer_AppID(applicationId, orgId).getId());
                                    }
                                    else
                                    {
                                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(language), getViewContext().getResponse());
                                        return null;
                                    }
                                }
                                else
                                {
                                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                                    return null;
                                }
                            }
                            else
                            {
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(language), getViewContext().getResponse());
                                return null;
                            }
                        }
                        else
                        {
                            if (appMessage.equals(FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language)))
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language), getViewContext().getResponse());
                            else
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                            return null;
                        }

                    }
                    else
                    {
                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }
                }
                else
                {
                    getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the DELETE method when calling this action.");
                    return null;
                }

            }
            catch (Exception e)
            {
                _log.error("Logout Action Error:", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }

    /**
     * Get the User Profile information
     */
    @Marshal(Marshaller.Jackson)
    @RequiresNoPermission
    public class UserProfileAction extends ReadOnlyApiAction<Object>
    {
        @Override
        public ApiResponse execute(Object object, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            boolean isAuthenticated = false;
            language = getViewContext().getRequest().getHeader("language");
            try
            {
                String auth = getViewContext().getRequest().getHeader("auth");
                String userId = getViewContext().getRequest().getHeader("userId");
                String applicationId = getViewContext().getRequest().getHeader("applicationId");
                String orgId = getViewContext().getRequest().getHeader("orgId");

                if (auth != null && StringUtils.isNotEmpty(auth)
                        && applicationId != null && StringUtils.isNotEmpty(applicationId) && orgId != null && StringUtils.isNotEmpty(orgId)
                        && null != userId && StringUtils.isNotEmpty(userId))
                {
                    String message = FdahpUserRegWSManager.get(language).validatedUserAppDetailsByAllApi(userId, "", applicationId, orgId);
                    if (StringUtils.isNotEmpty(message) && message.equals(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                    {

                        isAuthenticated = FdahpUserRegWSManager.get(language).validatedAuthKey(auth, applicationId, orgId);
                        if (isAuthenticated)
                        {
                            response = FdahpUserRegWSManager.get(language).getParticipantInfoDetails(userId, applicationId, orgId);
                        }
                        else
                        {
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(language), getViewContext().getResponse());
                            return null;
                        }
                    }
                    else if (message.equals(FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language)))
                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language), getViewContext().getResponse());
                    else
                    {

                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }
                }
                else
                {
                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                    return null;
                }
            }
            catch (Exception e)
            {
                _log.error("User Profile Action", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }

    /**
     * Update the user profile section
     */
    @Marshal(Marshaller.Jackson)
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class UpdateUserProfileAction extends MutatingApiAction<ProfileForm>
    {
        @Override
        public ApiResponse execute(ProfileForm profileForm, BindException errors) throws Exception
        {
            language = getViewContext().getRequest().getHeader("language");
            ApiSimpleResponse response = new ApiSimpleResponse();
            List<ParticipantStudies> addParticipantStudiesList = new ArrayList<ParticipantStudies>();
            String message = FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(language);
            AuthInfo updaAuthInfo = null;
            try
            {
                String auth = getViewContext().getRequest().getHeader("auth");
                String userId = getViewContext().getRequest().getHeader("userId");
                String applicationId = getViewContext().getRequest().getHeader("applicationId");
                String orgId = getViewContext().getRequest().getHeader("orgId");

                boolean isAuthenticated = false;
                if (auth != null && StringUtils.isNotEmpty(auth) && applicationId != null && StringUtils.isNotEmpty(applicationId)
                        && orgId != null && StringUtils.isNotEmpty(orgId) && userId != null && StringUtils.isNotEmpty(userId))
                {
                    String appMessage = FdahpUserRegWSManager.get(language).validatedUserAppDetailsByAllApi(userId, "", applicationId, orgId);
                    if (StringUtils.isNotEmpty(appMessage) && appMessage.equals(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                    {

                        isAuthenticated = FdahpUserRegWSManager.get(language).validatedAuthKey(auth, applicationId, orgId);
                        if (isAuthenticated)
                        {
                            if (profileForm != null)
                            {
                                UserDetails participantDetails = FdahpUserRegWSManager.get(language).getParticipantDetails(userId, applicationId, orgId);
                                if (participantDetails != null)
                                {
                                    if (profileForm.getSettings() != null)
                                    {
                                        if (profileForm.getSettings().getRemoteNotifications() != null)
                                        {
                                            participantDetails.setRemoteNotificationFlag(profileForm.getSettings().getRemoteNotifications());
                                            AuthInfo authInfo = FdahpUserRegWSManager.get(language).getAuthInfo(auth, userId, applicationId, orgId);
                                            if (authInfo != null)
                                            {
                                                authInfo.setRemoteNotificationFlag(profileForm.getSettings().getRemoteNotifications());
                                                FdahpUserRegWSManager.get(language).updateAuthInfo(authInfo);
                                            }
                                        }
                                        if (profileForm.getSettings().getLocalNotifications() != null)
                                            participantDetails.setLocalNotificationFlag(profileForm.getSettings().getLocalNotifications());
                                        if (profileForm.getSettings().getPasscode() != null)
                                            participantDetails.setUsePassCode(profileForm.getSettings().getPasscode());
                                        if (profileForm.getSettings().getTouchId() != null)
                                            participantDetails.setTouchId(profileForm.getSettings().getTouchId());
                                        if (profileForm.getSettings().getReminderLeadTime() != null && StringUtils.isNotEmpty(profileForm.getSettings().getReminderLeadTime()))
                                            participantDetails.setReminderLeadTime(profileForm.getSettings().getReminderLeadTime());

                                        if (profileForm.getSettings().getLocale() != null && StringUtils.isNotEmpty(profileForm.getSettings().getLocale()))
                                            participantDetails.setLocale(profileForm.getSettings().getLocale());
                                    }
                                    if (profileForm.getInfo() != null)
                                    {
                                        AuthInfo authInfo = FdahpUserRegWSManager.get(language).getAuthInfo(auth, userId, applicationId, orgId);
                                        if (authInfo != null)
                                        {
                                            if (profileForm.getInfo().getOs() != null && StringUtils.isNotEmpty(profileForm.getInfo().getOs()))
                                            {
                                                authInfo.setDeviceType(profileForm.getInfo().getOs());
                                            }
                                            if (profileForm.getInfo().getOs() != null && StringUtils.isNotEmpty(profileForm.getInfo().getOs()) && (profileForm.getInfo().getOs().equalsIgnoreCase("IOS") || profileForm.getInfo().getOs().equalsIgnoreCase("I")))
                                            {
                                                authInfo.setIosAppVersion(profileForm.getInfo().getAppVersion());
                                            }
                                            else
                                            {
                                                authInfo.setAndroidAppVersion(profileForm.getInfo().getAppVersion());
                                            }
                                            if (profileForm.getInfo().getDeviceToken() != null && StringUtils.isNotEmpty(profileForm.getInfo().getDeviceToken()))
                                            {
                                                authInfo.setDeviceToken(profileForm.getInfo().getDeviceToken());
                                            }
                                            updaAuthInfo = FdahpUserRegWSManager.get(language).updateAuthInfo(authInfo);
                                        }
                                    }
                                    UserDetails updateParticipantDetails = FdahpUserRegWSManager.get(language).saveParticipant(participantDetails);
                                    if (updateParticipantDetails != null || message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)) || updaAuthInfo != null)
                                    {
                                        response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase());
                                        FdahpUserRegWSManager.addAuditEvent(participantDetails.getUserId(), "PROFILE UPDATE", "User Profile/Preferences updated.  (User ID = " + participantDetails.getUserId() + ")", "FdaUserAuditEvent", getContainer_AppID(applicationId, orgId).getId());
                                    }
                                }
                                else
                                {
                                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                                    return null;
                                }
                            }
                            else
                            {
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                                return null;
                            }
                        }
                        else
                        {
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(language), getViewContext().getResponse());
                            return null;
                        }

                    }
                    else
                    {
                        if (appMessage.equals(FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language)))
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        else
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }
                }
                else
                {
                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                    return null;
                }
            }
            catch (Exception e)
            {
                _log.error("UpdateUSerProfile Action Error :", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }

    public static class ProfileForm
    {
        public ProfileBean _profile;
        public SettingsBean _settings;
        public InfoBean _info;
        public List<ParticipantInfoBean> _participantInfo;

        public ProfileBean getProfile()
        {
            return _profile;
        }

        public void setProfile(ProfileBean profile)
        {
            _profile = profile;
        }

        public SettingsBean getSettings()
        {
            return _settings;
        }

        public void setSettings(SettingsBean settings)
        {
            _settings = settings;
        }

        public InfoBean getInfo()
        {
            return _info;
        }

        public void setInfo(InfoBean info)
        {
            _info = info;
        }

        public List<ParticipantInfoBean> getParticipantInfo()
        {
            return _participantInfo;
        }

        public void setParticipantInfo(List<ParticipantInfoBean> participantInfo)
        {
            _participantInfo = participantInfo;
        }


    }

    /**
     * Update the user preferences
     */
    @Marshal(Marshaller.Jackson)
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class UpdatePreferencesAction extends MutatingApiAction<PreferencesForm>
    {
        @Override
        public ApiResponse execute(PreferencesForm preferencesForm, BindException errors) throws Exception
        {
            language = getViewContext().getRequest().getHeader("language");
            ApiSimpleResponse response = new ApiSimpleResponse();
            List<ParticipantStudies> addParticipantStudiesList = new ArrayList<ParticipantStudies>();
            List<ParticipantActivities> participantActivitiesList = new ArrayList<ParticipantActivities>();
            try
            {
                String auth = getViewContext().getRequest().getHeader("auth");
                String userId = getViewContext().getRequest().getHeader("userId");
                String applicationId = getViewContext().getRequest().getHeader("applicationId");
                String orgId = getViewContext().getRequest().getHeader("orgId");

                boolean isAuthenticated = false;
                if (auth != null && StringUtils.isNotEmpty(auth) && userId != null && StringUtils.isNotEmpty(userId) &&
                        applicationId != null && StringUtils.isNotEmpty(applicationId) && orgId != null && StringUtils.isNotEmpty(orgId))
                {
                    String message = FdahpUserRegWSManager.get(language).validatedUserAppDetailsByAllApi(userId, "", applicationId, orgId);
                    if (message.equals(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                    {
                        isAuthenticated = FdahpUserRegWSManager.get(language).validatedAuthKey(auth, applicationId, orgId);
                        if (isAuthenticated)
                        {
                            if (preferencesForm != null)
                            {
                                if (preferencesForm.getStudies() != null && preferencesForm.getStudies().size() > 0)
                                {
                                    List<StudiesBean> studiesBeenList = preferencesForm.getStudies();
                                    List<ParticipantStudies> existParticipantStudies = FdahpUserRegWSManager.get(language).getParticipantStudiesList(userId, applicationId, orgId);

                                    for (int i = 0; i < studiesBeenList.size(); i++)
                                    {
                                        StudiesBean studiesBean = studiesBeenList.get(i);
                                        boolean isExists = false;
                                        if (existParticipantStudies != null && existParticipantStudies.size() > 0)
                                        {
                                            for (ParticipantStudies participantStudies : existParticipantStudies)
                                            {

                                                if (studiesBean.getStudyId().equalsIgnoreCase(participantStudies.getStudyId()))
                                                {
                                                    isExists = true;
                                                    if (studiesBean.getStatus() != null && StringUtils.isNotEmpty(studiesBean.getStatus()))
                                                        participantStudies.setStatus(studiesBean.getStatus());
                                                    if (studiesBean.getBookmarked() != null)
                                                        participantStudies.setBookmark(studiesBean.getBookmarked());
                                                    addParticipantStudiesList.add(participantStudies);
                                                }
                                            }
                                        }
                                        if (!isExists)
                                        {
                                            ParticipantStudies participantStudies = new ParticipantStudies();
                                            if (studiesBean.getStudyId() != null && StringUtils.isNotEmpty(studiesBean.getStudyId()))
                                                participantStudies.setStudyId(studiesBean.getStudyId());
                                            if (studiesBean.getStatus() != null && StringUtils.isNotEmpty(studiesBean.getStatus()))
                                            {
                                                participantStudies.setStatus(studiesBean.getStatus());
                                            }
                                            else
                                            {
                                                participantStudies.setStatus(FdahpUserRegUtil.ErrorCodes.YET_TO_JOIN.getValue(language));
                                            }
                                            if (studiesBean.getBookmarked() != null)
                                                participantStudies.setBookmark(studiesBean.getBookmarked());
                                            if (userId != null && StringUtils.isNotEmpty(userId))
                                                participantStudies.setUserId(userId);
                                            addParticipantStudiesList.add(participantStudies);
                                        }
                                    }
                                    FdahpUserRegWSManager.get(language).saveParticipantStudies(addParticipantStudiesList);
                                }

                                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase());
                            }
                            else
                            {
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                                return null;
                            }
                        }
                        else
                        {
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(language), getViewContext().getResponse());
                            return null;
                        }

                    }
                    else
                    {
                        if (message.equals(FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language)))
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        else
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }
                }
                else
                {
                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                    return null;
                }
            }
            catch (Exception e)
            {
                _log.error("UpdatePreferences Action Error :", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }

    public static class PreferencesForm
    {
        public List<StudiesBean> _studies;
        public List<ActivitiesBean> _activity;
        public String _studyId;

        public List<StudiesBean> getStudies()
        {
            return _studies;
        }

        public void setStudies(List<StudiesBean> studies)
        {
            _studies = studies;
        }

        public List<ActivitiesBean> getActivity()
        {
            return _activity;
        }

        public void setActivity(List<ActivitiesBean> activity)
        {
            _activity = activity;
        }

        public String getStudyId()
        {
            return _studyId;
        }

        public void setStudyId(String studyId)
        {
            _studyId = studyId;
        }
    }


    public static class LoginForm
    {

        public String _emailId;
        public String _password;

        public String getEmailId()
        {
            return _emailId;
        }

        public void setEmailId(String emailId)
        {
            _emailId = emailId;
        }

        public String getPassword()
        {
            return _password;
        }

        public void setPassword(String password)
        {
            _password = password;
        }
    }

    /**
     * Get the user preferences
     */
    @Marshal(Marshaller.Jackson)
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class UserPreferencesAction extends ReadOnlyApiAction<UserForm>
    {

        @Override
        public ApiResponse execute(UserForm userForm, BindException errors) throws Exception
        {
            language = getViewContext().getRequest().getHeader("language");
            ApiSimpleResponse response = new ApiSimpleResponse();
            boolean isAuthenticated = false;
            try
            {
                String auth = getViewContext().getRequest().getHeader("auth");
                String userId = getViewContext().getRequest().getHeader("userId");
                String applicationId = getViewContext().getRequest().getHeader("applicationId");
                String orgId = getViewContext().getRequest().getHeader("orgId");

                if (auth != null && StringUtils.isNotEmpty(auth) &&
                        applicationId != null && StringUtils.isNotEmpty(applicationId) && orgId != null && StringUtils.isNotEmpty(orgId))
                {
                    String message = FdahpUserRegWSManager.get(language).validatedUserAppDetailsByAllApi(userId, "", applicationId, orgId);
                    if (StringUtils.isNotEmpty(message) && message.equals(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                    {
                        isAuthenticated = FdahpUserRegWSManager.get(language).validatedAuthKey(auth, applicationId, orgId);
                        if (isAuthenticated)
                        {
                            if (userId != null && StringUtils.isNotEmpty(userId))
                            {
                                response = FdahpUserRegWSManager.get(language).getPreferences(userId, applicationId, orgId);

                            }
                            else
                            {
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                                return null;
                            }
                        }
                        else
                        {
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(language), getViewContext().getResponse());
                            return null;
                        }
                    }
                    else
                    {
                        if (message.equals(FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language)))
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        else
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }
                }
                else
                {
                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                    return null;
                }
            }
            catch (Exception e)
            {
                _log.error("UserPreferencesAction Action Error", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;

            }
            return response;
        }
    }


    public static class ConsentStatusForm
    {

        private String _studyId;
        private Boolean _eligibility;
        private ConsentBean _consent;
        private String _sharing;

        public String getStudyId()
        {
            return _studyId;
        }

        public void setStudyId(String studyId)
        {
            _studyId = studyId;
        }

        public Boolean getEligibility()
        {
            return _eligibility;
        }

        public void setEligibility(Boolean eligibility)
        {
            _eligibility = eligibility;
        }

        public ConsentBean getConsent()
        {
            return _consent;
        }

        public void setConsent(ConsentBean consent)
        {
            _consent = consent;
        }

        public String getSharing()
        {
            return _sharing;
        }

        public void setSharing(String sharing)
        {
            _sharing = sharing;
        }
    }

    /**
     * Updating the user eligibility consent status
     */
    @Marshal(Marshaller.Jackson)
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class UpdateEligibilityConsentStatusAction extends MutatingApiAction<ConsentStatusForm>
    {

        @Override
        public ApiResponse execute(ConsentStatusForm consentStatusForm, BindException errors) throws Exception
        {
            language = getViewContext().getRequest().getHeader("language");
            ApiSimpleResponse response = new ApiSimpleResponse();
            boolean isAuthenticated = false;
            StudyConsent updateConsent = null;
            try
            {
                String auth = getViewContext().getRequest().getHeader("auth");
                String userId = getViewContext().getRequest().getHeader("userId");
                String applicationId = getViewContext().getRequest().getHeader("applicationId");
                String orgId = getViewContext().getRequest().getHeader("orgId");

                if (auth != null && StringUtils.isNotEmpty(auth)
                        && applicationId != null && StringUtils.isNotEmpty(applicationId) && orgId != null && StringUtils.isNotEmpty(orgId))
                {
                    String isValidAppmessage = FdahpUserRegWSManager.get(language).validatedUserAppDetailsByAllApi(userId, "", applicationId, orgId);
                    if (StringUtils.isNotEmpty(isValidAppmessage) && isValidAppmessage.equals(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                    {
                        isAuthenticated = FdahpUserRegWSManager.get(language).validatedAuthKey(auth, applicationId, orgId);
                        if (isAuthenticated)
                        {
                            if (consentStatusForm != null && consentStatusForm.getConsent() != null)
                            {
                                if (consentStatusForm.getStudyId() != null && StringUtils.isNotEmpty(consentStatusForm.getStudyId()) && userId != null && StringUtils.isNotEmpty(userId))
                                {
                                    ParticipantStudies participantStudies = FdahpUserRegWSManager.get(language).getParticipantStudies(consentStatusForm.getStudyId(), userId, applicationId, orgId);
                                    if (participantStudies != null)
                                    {
                                        if (consentStatusForm.getEligibility() != null)
                                        {
                                            participantStudies.setEligbibility(consentStatusForm.getEligibility());
                                        }
                                        if (consentStatusForm.getSharing() != null && StringUtils.isNotEmpty(consentStatusForm.getSharing()))
                                        {
                                            participantStudies.setSharing(consentStatusForm.getSharing());
                                        }
                                        List<ParticipantStudies> participantStudiesList = new ArrayList<ParticipantStudies>();
                                        participantStudiesList.add(participantStudies);
                                        String message = FdahpUserRegWSManager.get(language).saveParticipantStudies(participantStudiesList);
                                        if (consentStatusForm.getConsent() != null)
                                        {
                                            StudyConsent consent = null;
                                            if (consentStatusForm.getConsent().getVersion() != null && StringUtils.isNotEmpty(consentStatusForm.getConsent().getVersion()))
                                            {
                                                consent = FdahpUserRegWSManager.get(language).getStudyConsent(userId, consentStatusForm.getStudyId(), consentStatusForm.getConsent().getVersion(), applicationId, orgId);
                                                if (consent != null)
                                                {
                                                    if (consentStatusForm.getConsent().getVersion() != null && StringUtils.isNotEmpty(consentStatusForm.getConsent().getVersion()))
                                                        consent.setVersion(consentStatusForm.getConsent().getVersion());
                                                    if (consentStatusForm.getConsent().getStatus() != null && StringUtils.isNotEmpty(consentStatusForm.getConsent().getStatus()))
                                                        consent.setStatus(consentStatusForm.getConsent().getStatus());
                                                    if (consentStatusForm.getConsent().getPdf() != null && StringUtils.isNotEmpty(consentStatusForm.getConsent().getPdf()))
                                                    {
                                                        consent.setPdf(consentStatusForm.getConsent().getPdf());
                                                        String pdfPath = saveStudyConsentDocument(consent);
                                                        consent.setPdfPath(pdfPath);
                                                    }
                                                    consent.setUserId(userId);
                                                    consent.setStudyId(consentStatusForm.getStudyId());
                                                }
                                                else
                                                {
                                                    consent = new StudyConsent();
                                                    consent.setUserId(userId);
                                                    consent.setStudyId(consentStatusForm.getStudyId());
                                                    consent.setApplicationId(applicationId);
                                                    consent.setOrgId(orgId);
                                                    consent.setStatus(consentStatusForm.getConsent().getStatus());
                                                    consent.setVersion(consentStatusForm.getConsent().getVersion());
                                                    consent.setPdf(consentStatusForm.getConsent().getPdf());
                                                    if (consentStatusForm.getConsent().getPdf() != null && StringUtils.isNotEmpty(consentStatusForm.getConsent().getPdf()))
                                                    {
                                                        String pdfPath = saveStudyConsentDocument(consent);
                                                        consent.setPdfPath(pdfPath);
                                                    }
                                                }
                                                updateConsent = FdahpUserRegWSManager.get(language).saveStudyConsent(consent);
                                                if (updateConsent != null && message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                                                {
                                                    response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), message);
                                                    FdahpUserRegWSManager.addAuditEvent(userId, "Update eligibility consent status", "Eligibility consent has been updated for study " + consentStatusForm.getStudyId() + ".", "FdaStudyAuditEvent", getContainer_AppID(applicationId, orgId).getId());
                                                }
                                                else
                                                {
                                                    response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(language));
                                                }
                                            }
                                            else
                                            {
                                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.CONSENT_VERSION_REQUIRED.getValue(language), FdahpUserRegUtil.ErrorCodes.CONSENT_VERSION_REQUIRED.getValue(language), getViewContext().getResponse());
                                                return null;
                                            }

                                        }

                                    }
                                    else
                                    {
                                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.NO_DATA_AVAILABLE.getValue(language), FdahpUserRegUtil.ErrorCodes.NO_DATA_AVAILABLE.getValue(language), getViewContext().getResponse());
                                        return null;
                                    }
                                }
                                else
                                {
                                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                                    return null;
                                }
                            }
                            else
                            {
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                                return null;
                            }
                        }
                        else
                        {
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(language), getViewContext().getResponse());
                            return null;
                        }
                    }
                    else
                    {
                        if (isValidAppmessage.equals(FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language)))
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        else
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }
                }
                else
                {
                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                    return null;
                }
            }
            catch (Exception e)
            {
                _log.error("UpdateEligibilityConsentStatusAction Action Error", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }

    public static class ActivityForm
    {

        public String _userId;
        public String _studyId;
        public String _consentVersion;
        public ActivityStateForm _activity;

        public String getUserId()
        {
            return _userId;
        }

        public void setUserId(String userId)
        {
            _userId = userId;
        }

        public String getStudyId()
        {
            return _studyId;
        }

        public void setStudyId(String studyId)
        {
            _studyId = studyId;
        }

        public String getConsentVersion()
        {
            return _consentVersion;
        }

        public void setConsentVersion(String consentVersion)
        {
            _consentVersion = consentVersion;
        }

        public ActivityStateForm getActivity()
        {
            return _activity;
        }

        public void setActivity(ActivityStateForm activity)
        {
            _activity = activity;
        }
    }

    /**
     * Get activity state of an study to the user
     */
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class ActivityStateAction extends ReadOnlyApiAction<ActivityForm>
    {

        @Override
        public ApiResponse execute(ActivityForm activityForm, BindException errors) throws Exception
        {
            language = getViewContext().getRequest().getHeader("language");
            ApiSimpleResponse response = new ApiSimpleResponse();
            boolean isAuthenticated = false;
            try
            {
                String userId = getViewContext().getRequest().getHeader("userId");
                String auth = getViewContext().getRequest().getHeader("auth");
                String applicationId = getViewContext().getRequest().getHeader("applicationId");
                String orgId = getViewContext().getRequest().getHeader("orgId");

                if (auth != null && StringUtils.isNotEmpty(auth) && applicationId != null && StringUtils.isNotEmpty(applicationId)
                        && orgId != null && StringUtils.isNotEmpty(orgId))
                {
                    isAuthenticated = FdahpUserRegWSManager.get(language).validatedAuthKey(auth, applicationId, orgId);
                    if (isAuthenticated)
                    {
                        String studyId = getViewContext().getRequest().getParameter("studyId");
                        if (studyId != null && StringUtils.isNotEmpty(studyId) && userId != null && StringUtils.isNotEmpty(userId))
                        {
                            List<ParticipantActivities> participantActivitiesList = FdahpUserRegWSManager.get(language).getParticipantActivitiesList(studyId, userId, applicationId, orgId);
                            JSONArray jsonArray = new JSONArray();
                            if (participantActivitiesList != null && participantActivitiesList.size() > 0)
                            {
                                for (ParticipantActivities participantActivities : participantActivitiesList)
                                {
                                    JSONObject jsonObject = new JSONObject();
                                    if (participantActivities.getActivityId() != null && StringUtils.isNotEmpty(participantActivities.getActivityId()))
                                        jsonObject.put("activityId", participantActivities.getActivityId());
                                    if (participantActivities.getActivityVersion() != null && StringUtils.isNotEmpty(participantActivities.getActivityVersion()))
                                        jsonObject.put("activityVersion", participantActivities.getActivityVersion());
                                    if (participantActivities.getActivityState() != null && StringUtils.isNotEmpty(participantActivities.getActivityState()))
                                        jsonObject.put("activityState", participantActivities.getActivityState());
                                    if (participantActivities.getActivityRunId() != null && StringUtils.isNotEmpty(participantActivities.getActivityRunId()))
                                        jsonObject.put("activityRunId", participantActivities.getActivityRunId());
                                    if (participantActivities.getBookmark() != null)
                                        jsonObject.put("bookmarked", participantActivities.getBookmark());

                                    if (participantActivities.getActivityStartDate() != null)
                                        jsonObject.put("activityStartDate", participantActivities.getActivityStartDate());
                                    if (participantActivities.getActivityEndDate() != null)
                                        jsonObject.put("activityEndDate", participantActivities.getActivityEndDate());
                                    if (participantActivities.getAnchorDateVersion() != null)
                                        jsonObject.put("anchorDateVersion", participantActivities.getAnchorDateVersion());
                                    if (participantActivities.getAnchorDateCreatedDate() != null)
                                        jsonObject.put("anchorDatecreatedDate", participantActivities.getAnchorDateCreatedDate());
                                    if (participantActivities.getLastModifiedDate() != null)
                                        jsonObject.put("lastModifiedDate", participantActivities.getLastModifiedDate());

                                    List<CustomScheduleRunsBean> customScheduleRunsList = FdahpUserRegWSManager.get(language).getCustomScheduleRun(participantActivities.getStudyId(), participantActivities.getActivityId());
                                    if (customScheduleRunsList != null && !customScheduleRunsList.isEmpty())
                                    {
                                        JSONArray customRunjsonArray = new JSONArray();
                                        JSONObject customRun = null;
                                        for (CustomScheduleRunsBean customScheduleRuns : customScheduleRunsList)
                                        {
                                            customRun = new JSONObject();
                                            customRun.put("runStartDate", customScheduleRuns.getRunStartDate());
                                            customRun.put("runEndDate", customScheduleRuns.getRunEndDate());

                                            customRunjsonArray.put(customRun);
                                        }
                                        jsonObject.put("customScheduleRuns", customRunjsonArray);
                                    }

                                    JSONObject runObject = new JSONObject();
                                    if (participantActivities.getTotal() != null)
                                        runObject.put("total", participantActivities.getTotal());
                                    if (participantActivities.getCompleted() != null)
                                        runObject.put("completed", participantActivities.getCompleted());
                                    if (participantActivities.getMissed() != null)
                                        runObject.put("missed", participantActivities.getMissed());
                                    jsonObject.put("activityRun", runObject);
                                    jsonArray.put(jsonObject);
                                }
                            }
                            response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase());
                            response.put(FdahpUserRegUtil.ErrorCodes.ACTIVITIES.getValue(language), jsonArray);
                        }
                        else
                        {
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                            return null;
                        }
                    }
                    else
                    {
                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }
                }
                else
                {
                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                    return null;
                }
            }
            catch (Exception e)
            {
                _log.error("ActivityStateAction Action Error", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;

            }
            return response;
        }
    }

    public static class ActivityStateForm
    {


        private String _activityId;
        private String _activityVersion;
        private String _activityState;
        private String _activityRunId;


        public String getActivityId()
        {
            return _activityId;
        }

        public void setActivityId(String activityId)
        {
            _activityId = activityId;
        }

        public String getActivityVersion()
        {
            return _activityVersion;
        }

        public void setActivityVersion(String activityVersion)
        {
            _activityVersion = activityVersion;
        }

        public String getActivityState()
        {
            return _activityState;
        }

        public void setActivityState(String activityState)
        {
            _activityState = activityState;
        }

        public String getActivityRunId()
        {
            return _activityRunId;
        }

        public void setActivityRunId(String activityRunId)
        {
            _activityRunId = activityRunId;
        }
    }


    /**
     * update the activity state of an study
     */
    @Marshal(Marshaller.Jackson)
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class UpdateActivityStateAction extends MutatingApiAction<PreferencesForm>
    {

        @Override
        public ApiResponse execute(PreferencesForm preferencesForm, BindException errors) throws Exception
        {
            language = getViewContext().getRequest().getHeader("language");
            ApiSimpleResponse response = new ApiSimpleResponse();
            boolean isAuthenticated = false;
            try
            {
                String auth = getViewContext().getRequest().getHeader("auth");
                String userId = getViewContext().getRequest().getHeader("userId");
                String applicationId = getViewContext().getRequest().getHeader("applicationId");
                String orgId = getViewContext().getRequest().getHeader("orgId");

                List<ParticipantActivities> addParticipantActivitiesList = new ArrayList<ParticipantActivities>();
                if (auth != null && StringUtils.isNotEmpty(auth) && applicationId != null && StringUtils.isNotEmpty(applicationId)
                        && orgId != null && StringUtils.isNotEmpty(orgId))
                {
                    String validMsg = FdahpUserRegWSManager.get(language).validatedUserAppDetailsByAllApi(userId, "", applicationId, orgId);
                    if (StringUtils.isNotEmpty(validMsg) && validMsg.equals(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                    {
                        isAuthenticated = FdahpUserRegWSManager.get(language).validatedAuthKey(auth, applicationId, orgId);
                        if (isAuthenticated)
                        {
                            if (preferencesForm != null && userId != null && StringUtils.isNotEmpty(userId))
                            {
                                if ((preferencesForm.getStudyId() != null && StringUtils.isNotEmpty(preferencesForm.getStudyId())) && (preferencesForm.getActivity() != null && preferencesForm.getActivity().size() > 0))
                                {
                                    List<ActivitiesBean> activitiesBeanList = preferencesForm.getActivity();
                                    List<CustomScheduleRuns> customScheduleRunsList;
                                    List<ParticipantActivities> participantActivitiesList = FdahpUserRegWSManager.get(language).getParticipantActivitiesList(preferencesForm.getStudyId(), userId, applicationId, orgId);
                                    for (int i = 0; i < activitiesBeanList.size(); i++)
                                    {
                                        ActivitiesBean activitiesBean = activitiesBeanList.get(i);
                                        boolean isExists = false;
                                        if (participantActivitiesList != null && participantActivitiesList.size() > 0)
                                        {
                                            for (ParticipantActivities participantActivities : participantActivitiesList)
                                            {
                                                if (participantActivities.getActivityId().equalsIgnoreCase(activitiesBean.getActivityId()))
                                                {
                                                    isExists = true;
                                                    if (activitiesBean.getActivityVersion() != null && StringUtils.isNotEmpty(activitiesBean.getActivityVersion()))
                                                        participantActivities.setActivityVersion(activitiesBean.getActivityVersion());
                                                    if (activitiesBean.getActivityState() != null && StringUtils.isNotEmpty(activitiesBean.getActivityState()))
                                                        participantActivities.setActivityState(activitiesBean.getActivityState());
                                                    if (activitiesBean.getActivityRunId() != null && StringUtils.isNotEmpty(activitiesBean.getActivityRunId()))
                                                        participantActivities.setActivityRunId(activitiesBean.getActivityRunId());
                                                    if (activitiesBean.getBookmarked() != null)
                                                        participantActivities.setBookmark(activitiesBean.getBookmarked());

                                                    if (activitiesBean.getActivityStartDate() != null && StringUtils.isNotEmpty(activitiesBean.getActivityStartDate()))
                                                        participantActivities.setActivityStartDate(activitiesBean.getActivityStartDate());
                                                    if (activitiesBean.getActivityEndDate() != null && StringUtils.isNotEmpty(activitiesBean.getActivityEndDate()))
                                                        participantActivities.setActivityEndDate(activitiesBean.getActivityEndDate());
                                                    if (activitiesBean.getAnchorDateVersion() != null && StringUtils.isNotEmpty(activitiesBean.getAnchorDateVersion()))
                                                        participantActivities.setAnchorDateVersion(activitiesBean.getAnchorDateVersion());
                                                    if (activitiesBean.getAnchorDatecreatedDate() != null && StringUtils.isNotEmpty(activitiesBean.getAnchorDatecreatedDate()))
                                                        participantActivities.setAnchorDateCreatedDate(activitiesBean.getAnchorDatecreatedDate());
                                                    if (activitiesBean.getLastModifiedDate() != null && StringUtils.isNotEmpty(activitiesBean.getLastModifiedDate()))
                                                        participantActivities.setLastModifiedDate(activitiesBean.getLastModifiedDate());

                                                    if (activitiesBean.getCustomScheduleRuns() != null && !activitiesBean.getCustomScheduleRuns().isEmpty())
                                                    {
                                                        customScheduleRunsList = new ArrayList<>();
                                                        CustomScheduleRuns customScheduleRuns;
                                                        for (CustomScheduleRunsBean customScheduleRunsItem : activitiesBean.getCustomScheduleRuns())
                                                        {
                                                            customScheduleRuns = new CustomScheduleRuns();
                                                            customScheduleRuns.setStudyId(participantActivities.getStudyId());
                                                            customScheduleRuns.setActivityId(participantActivities.getActivityId());
                                                            customScheduleRuns.setRunStartDate(customScheduleRunsItem.getRunStartDate());
                                                            customScheduleRuns.setRunEndDate(customScheduleRunsItem.getRunEndDate());
                                                            customScheduleRuns.setApplicationId(applicationId);
                                                            customScheduleRuns.setOrgId(orgId);
                                                            customScheduleRunsList.add(customScheduleRuns);
                                                        }
                                                        FdahpUserRegWSManager.get(language).saveCustomScheduleRun(customScheduleRunsList, participantActivities.getStudyId(), participantActivities.getActivityId(), applicationId, orgId);
                                                    }
//                                                    else
//                                                    {
//                                                        FdahpUserRegWSManager.get(language).deleteExistingCustomRuns(participantActivities.getStudyId(), participantActivities.getActivityId(), applicationId, orgId);
//                                                    }

                                                    if (activitiesBean.getActivityRun() != null)
                                                    {
                                                        if (activitiesBean.getActivityRun().getTotal() != null)
                                                            participantActivities.setTotal(activitiesBean.getActivityRun().getTotal());
                                                        if (activitiesBean.getActivityRun().getCompleted() != null)
                                                            participantActivities.setCompleted(activitiesBean.getActivityRun().getCompleted());
                                                        if (activitiesBean.getActivityRun().getMissed() != null)
                                                            participantActivities.setMissed(activitiesBean.getActivityRun().getMissed());
                                                    }
                                                    addParticipantActivitiesList.add(participantActivities);
                                                }
                                            }

                                        }
                                        if (!isExists)
                                        {
                                            ParticipantActivities addParticipantActivities = new ParticipantActivities();
                                            if (activitiesBean != null && StringUtils.isNotEmpty(activitiesBean.getActivityState()))
                                                addParticipantActivities.setActivityState(activitiesBean.getActivityState());
                                            if (activitiesBean.getActivityVersion() != null && StringUtils.isNotEmpty(activitiesBean.getActivityVersion()))
                                                addParticipantActivities.setActivityVersion(activitiesBean.getActivityVersion());
                                            if (activitiesBean.getActivityId() != null && StringUtils.isNotEmpty(activitiesBean.getActivityId()))
                                                addParticipantActivities.setActivityId(activitiesBean.getActivityId());
                                            if (activitiesBean.getActivityRunId() != null && StringUtils.isNotEmpty(activitiesBean.getActivityRunId()))
                                                addParticipantActivities.setActivityRunId(activitiesBean.getActivityRunId());
                                            if (preferencesForm.getStudyId() != null && StringUtils.isNotEmpty(preferencesForm.getStudyId()))
                                                addParticipantActivities.setStudyId(preferencesForm.getStudyId());
                                            if (userId != null && StringUtils.isNotEmpty(userId))
                                                addParticipantActivities.setParticipantId(userId);
                                            if (activitiesBean.getBookmarked() != null)
                                                addParticipantActivities.setBookmark(activitiesBean.getBookmarked());

                                            if (activitiesBean.getActivityStartDate() != null && StringUtils.isNotEmpty(activitiesBean.getActivityStartDate()))
                                                addParticipantActivities.setActivityStartDate(activitiesBean.getActivityStartDate());
                                            if (activitiesBean.getActivityEndDate() != null && StringUtils.isNotEmpty(activitiesBean.getActivityEndDate()))
                                                addParticipantActivities.setActivityEndDate(activitiesBean.getActivityEndDate());
                                            if (activitiesBean.getAnchorDateVersion() != null && StringUtils.isNotEmpty(activitiesBean.getAnchorDateVersion()))
                                                addParticipantActivities.setAnchorDateVersion(activitiesBean.getAnchorDateVersion());
                                            if (activitiesBean.getAnchorDatecreatedDate() != null && StringUtils.isNotEmpty(activitiesBean.getAnchorDatecreatedDate()))
                                                addParticipantActivities.setAnchorDateCreatedDate(activitiesBean.getAnchorDatecreatedDate());
                                            if (activitiesBean.getLastModifiedDate() != null && StringUtils.isNotEmpty(activitiesBean.getLastModifiedDate()))
                                                addParticipantActivities.setLastModifiedDate(activitiesBean.getLastModifiedDate());


                                            if (activitiesBean.getCustomScheduleRuns() != null && !activitiesBean.getCustomScheduleRuns().isEmpty())
                                            {
                                                customScheduleRunsList = new ArrayList<>();
                                                CustomScheduleRuns customScheduleRuns;
                                                for (CustomScheduleRunsBean customScheduleRunsItem : activitiesBean.getCustomScheduleRuns())
                                                {
                                                    customScheduleRuns = new CustomScheduleRuns();
                                                    customScheduleRuns.setStudyId(preferencesForm.getStudyId());
                                                    customScheduleRuns.setActivityId(activitiesBean.getActivityId());
                                                    customScheduleRuns.setRunStartDate(customScheduleRunsItem.getRunStartDate());
                                                    customScheduleRuns.setRunEndDate(customScheduleRunsItem.getRunEndDate());
                                                    customScheduleRuns.setApplicationId(applicationId);
                                                    customScheduleRuns.setOrgId(orgId);
                                                    customScheduleRunsList.add(customScheduleRuns);
                                                }
                                                FdahpUserRegWSManager.get(language).saveCustomScheduleRun(customScheduleRunsList, preferencesForm.getStudyId(), activitiesBean.getActivityId(), applicationId, orgId);
                                            }


                                            if (activitiesBean.getActivityRun() != null)
                                            {
                                                if (activitiesBean.getActivityRun().getTotal() != null)
                                                    addParticipantActivities.setTotal(activitiesBean.getActivityRun().getTotal());
                                                if (activitiesBean.getActivityRun().getCompleted() != null)
                                                    addParticipantActivities.setCompleted(activitiesBean.getActivityRun().getCompleted());
                                                if (activitiesBean.getActivityRun().getMissed() != null)
                                                    addParticipantActivities.setMissed(activitiesBean.getActivityRun().getMissed());
                                            }
                                            if (applicationId != null && StringUtils.isNotEmpty(applicationId))
                                                addParticipantActivities.setApplicationId(applicationId);
                                            if (orgId != null && StringUtils.isNotEmpty(orgId))
                                                addParticipantActivities.setOrgId(orgId);
                                            addParticipantActivitiesList.add(addParticipantActivities);
                                        }
                                    }

                                    String message = FdahpUserRegWSManager.get(language).saveParticipantActivities(addParticipantActivitiesList);
                                    if (message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                                    {
                                        response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase());
                                    }
                                    else
                                    {
                                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(language), getViewContext().getResponse());
                                        return null;
                                    }
                                }
                                else
                                {
                                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                                    return null;
                                }

                            }
                            else
                            {
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                                return null;
                            }
                        }
                        else
                        {
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(language), getViewContext().getResponse());
                            return null;
                        }
                    }
                    else
                    {
                        if (validMsg.equals(FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language)))
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        else
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }
                }
                else
                {
                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                    return null;
                }
            }
            catch (Exception e)
            {
                _log.error("UpdateActivityStateAction Action Error", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }

    public static class WithDrawForm
    {

        public String _studyId;

        public Boolean _deleteData;

        public String getStudyId()
        {
            return _studyId;
        }

        public void setStudyId(String studyId)
        {
            _studyId = studyId;
        }

        public Boolean getDeleteData()
        {
            return _deleteData;
        }

        public void setDeleteData(Boolean deleteData)
        {
            _deleteData = deleteData;
        }
    }

    /**
     * With draw the user from study
     */
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    @MethodsAllowed(HttpUtil.Method.DELETE)
    public class WithdrawAction extends MutatingApiAction<WithDrawForm>
    {

        @Override
        public ApiResponse execute(WithDrawForm withDrawForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            boolean isAuthenticated = false;
            language = getViewContext().getRequest().getHeader("language");
            String message = FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(language);
            try
            {
                if (isDelete())
                {
                    String auth = getViewContext().getRequest().getHeader("auth");
                    String userId = getViewContext().getRequest().getHeader("userId");
                    String applicationId = getViewContext().getRequest().getHeader("applicationId");
                    String orgId = getViewContext().getRequest().getHeader("orgId");

                    if (auth != null && StringUtils.isNotEmpty(auth) &&
                            applicationId != null && StringUtils.isNotEmpty(applicationId) && orgId != null && StringUtils.isNotEmpty(orgId))
                    {
                        String isvalidMessage = FdahpUserRegWSManager.get(language).validatedUserAppDetailsByAllApi(userId, "", applicationId, orgId);
                        if (StringUtils.isNotEmpty(isvalidMessage) && isvalidMessage.equals(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                        {
                            isAuthenticated = FdahpUserRegWSManager.get(language).validatedAuthKey(auth, applicationId, orgId);
                            if (isAuthenticated)
                            {
                                if (withDrawForm != null && null != withDrawForm.getStudyId() && StringUtils.isNotEmpty(withDrawForm.getStudyId()) && null != userId && StringUtils.isNotEmpty(userId))
                                {
                                    ParticipantStudies participantStudies = FdahpUserRegWSManager.get(language).getParticipantStudies(withDrawForm.getStudyId(), userId, applicationId, orgId);
                                    if (participantStudies != null)
                                    {
                                        if (participantStudies.getStatus().equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.WITHDRAWN.getValue(language)))
                                        {
                                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.NO_DATA_AVAILABLE.getValue(language), FdahpUserRegUtil.ErrorCodes.WITHDRAWN_STUDY.getValue(language), getViewContext().getResponse());
                                            response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.WITHDRAWN_STUDY.getValue(language));
                                        }
                                        else
                                        {
                                            message = FdahpUserRegWSManager.get(language).withDrawStudy(withDrawForm.getStudyId(), userId, applicationId, orgId, withDrawForm.getDeleteData());
                                            if (message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                                            {
                                                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase());
                                                FdahpUserRegWSManager.addAuditEvent(userId, "With draw from study", "User withdrawn from study " + withDrawForm.getStudyId() + ".", "FdaStudyAuditEvent", getContainer_AppID(applicationId, orgId).getId());
                                            }
                                            else
                                            {
                                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(language), getViewContext().getResponse());
                                                return null;
                                            }
                                        }
                                    }
                                    else
                                    {
                                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(language), getViewContext().getResponse());
                                        return null;
                                    }
                                }
                                else
                                {
                                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                                    return null;
                                }

                            }
                            else
                            {
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(language), getViewContext().getResponse());
                                return null;
                            }
                        }
                        else
                        {
                            if (isvalidMessage.equals(FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language)))
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language), getViewContext().getResponse());
                            else
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                            return null;
                        }
                    }
                    else
                    {
                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }
                }
                else
                {
                    getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the DELETE method when calling this action.");
                    return null;
                }
            }
            catch (Exception e)
            {
                _log.error("Withdraw Action Error:", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }

    /**
     * Get the consent pdf of an user
     */
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class ConsentPDFAction extends ReadOnlyApiAction<ActivityForm>
    {

        @Override
        public ApiResponse execute(ActivityForm activityForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            language = getViewContext().getRequest().getHeader("language");
            boolean isAuthenticated = false;
            try
            {
                String auth = getViewContext().getRequest().getHeader("auth");
                String userId = getViewContext().getRequest().getHeader("userId");
                String consentVersion = getViewContext().getRequest().getParameter("consentVersion");
                String studyId = getViewContext().getRequest().getParameter("studyId");
                String applicationId = getViewContext().getRequest().getHeader("applicationId");
                String orgId = getViewContext().getRequest().getHeader("orgId");

                if (auth != null && StringUtils.isNotEmpty(auth)
                        && applicationId != null && StringUtils.isNotEmpty(applicationId) && orgId != null && StringUtils.isNotEmpty(orgId))
                {
                    String message = FdahpUserRegWSManager.get(language).validatedUserAppDetailsByAllApi(userId, "", applicationId, orgId);
                    if (StringUtils.isNotEmpty(message) && message.equals(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                    {

                        isAuthenticated = FdahpUserRegWSManager.get(language).validatedAuthKey(auth, applicationId, orgId);
                        if (isAuthenticated)
                        {
                            if (studyId != null && StringUtils.isNoneBlank(studyId) && userId != null && StringUtils.isNotEmpty(userId))
                            {
                                StudyConsent studyConsent = FdahpUserRegWSManager.get(language).getStudyConsent(userId, studyId, consentVersion, applicationId, orgId);
                                if (studyConsent != null)
                                {
                                    JSONObject jsonObject = new JSONObject();
                                    if (studyConsent.getVersion() != null)
                                        jsonObject.put("version", studyConsent.getVersion());
                                    if (studyConsent.getPdf() != null)
                                        jsonObject.put("content", studyConsent.getPdf());
                                    jsonObject.put("type", "application/pdf");
                                    response.put("consent", jsonObject);
                                    ParticipantStudies participantStudies = FdahpUserRegWSManager.get(language).getParticipantStudies(studyId, userId, applicationId, orgId);
                                    if (participantStudies != null)
                                    {
                                        response.put("sharing", participantStudies.getSharing());
                                    }
                                    response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase());
                                }
                                else
                                {
                                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.NO_DATA_AVAILABLE.getValue(language), FdahpUserRegUtil.ErrorCodes.NO_DATA_AVAILABLE.getValue(language), getViewContext().getResponse());
                                    return null;
                                }
                            }
                            else
                            {
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                                return null;
                            }
                        }
                        else
                        {
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(language), getViewContext().getResponse());
                            return null;
                        }
                    }
                    else
                    {
                        if (message.equals(FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language)))
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        else
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }
                }
                else
                {
                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                    return null;
                }
            }
            catch (Exception e)
            {
                _log.error("ConsentPDFAction Action Error", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }

    /**
     * Delete of an user
     */
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class DeleteAccountAction extends ReadOnlyApiAction
    {

        @Override
        public Object execute(Object o, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            language = getViewContext().getRequest().getHeader("language");
            boolean isAuthenticated = false;
            try
            {
                String userId = getViewContext().getRequest().getHeader("userId");
                String auth = getViewContext().getRequest().getHeader("auth");
                String applicationId = getViewContext().getRequest().getHeader("applicationId");
                String orgId = getViewContext().getRequest().getHeader("orgId");

                if ((userId != null && StringUtils.isNotEmpty(userId)) && (auth != null && StringUtils.isNotEmpty(auth))
                        && applicationId != null && StringUtils.isNotEmpty(applicationId) && orgId != null && StringUtils.isNotEmpty(orgId))
                {
                    isAuthenticated = FdahpUserRegWSManager.get(language).validatedAuthKey(auth, applicationId, orgId);
                    if (isAuthenticated)
                    {
                        String message = FdahpUserRegWSManager.get(language).deleteAccount(userId);
                        if (message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                        {
                            response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase());
                        }
                        else
                        {
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(language), getViewContext().getResponse());
                            return null;
                        }
                    }
                    else
                    {
                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }
                }
                else
                {
                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                    return null;
                }
            }
            catch (Exception e)
            {
                _log.error("Delete Account Action:", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }

    public static class DeactivateForm
    {

        public List<String> _deleteData;

        public List<String> getDeleteData()
        {
            return _deleteData;
        }

        public void setDeleteData(List<String> deleteData)
        {
            _deleteData = deleteData;
        }
    }

    /**
     * Deactivation of an user
     */
    @Marshal(Marshaller.Jackson)
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    @MethodsAllowed(HttpUtil.Method.DELETE)
    public class DeactivateAction extends MutatingApiAction<DeactivateForm>
    {

        @Override
        public Object execute(DeactivateForm deactivateForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            boolean isAuthenticated = false;
            language = getViewContext().getRequest().getHeader("language");
            String message = FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(language);
            try
            {
                if (isDelete())
                {
                    String auth = getViewContext().getRequest().getHeader("auth");
                    String userId = getViewContext().getRequest().getHeader("userId");
                    String applicationId = getViewContext().getRequest().getHeader("applicationId");
                    String orgId = getViewContext().getRequest().getHeader("orgId");

                    if (auth != null && StringUtils.isNotEmpty(auth) && applicationId != null && StringUtils.isNotEmpty(applicationId)
                            && orgId != null && StringUtils.isNotEmpty(orgId))
                    {
                        String appMessage = FdahpUserRegWSManager.get(language).validatedUserAppDetailsByAllApi(userId, "", applicationId, orgId);
                        if (StringUtils.isNotEmpty(appMessage) && appMessage.equals(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                        {

                            isAuthenticated = FdahpUserRegWSManager.get(language).validatedAuthKey(auth, applicationId, orgId);
                            if (isAuthenticated)
                            {
                                if (null != userId && StringUtils.isNotEmpty(userId))
                                {
                                    message = FdahpUserRegWSManager.get(language).deActivate(userId, deactivateForm, applicationId, orgId);
                                    if (message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                                    {
                                        FdahpUserRegWSManager.addAuditEvent(userId, "ACCOUNT DELETE", "User account deleted. (User ID = " + userId + ") ", "FdaUserAuditEvent", getContainer_AppID(applicationId, orgId).getId());
                                        response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase());
                                    }
                                    else
                                    {
                                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(language), getViewContext().getResponse());
                                        return null;
                                    }
                                }
                                else
                                {
                                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                                    return null;
                                }
                            }
                            else
                            {
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(language), getViewContext().getResponse());
                                return null;
                            }

                        }
                        else
                        {
                            if (appMessage.equals(FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language)))
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language), getViewContext().getResponse());
                            else
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                            return null;
                        }

                    }
                    else
                    {
                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }
                }
                else
                {
                    getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the DELETE method when calling this action.");
                    return null;
                }

            }
            catch (Exception e)
            {
                _log.error("Deactivate  Action Error:", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }

    /**
     * update the study sate
     */
    @Marshal(Marshaller.Jackson)
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class UpdateStudyStateAction extends MutatingApiAction<PreferencesForm>
    {

        @Override
        public ApiResponse execute(PreferencesForm preferencesForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            language = getViewContext().getRequest().getHeader("language");
            List<ParticipantStudies> addParticipantStudiesList = new ArrayList<ParticipantStudies>();
            try
            {
                String auth = getViewContext().getRequest().getHeader("auth");
                String userId = getViewContext().getRequest().getHeader("userId");
                String applicationId = getViewContext().getRequest().getHeader("applicationId");
                String orgId = getViewContext().getRequest().getHeader("orgId");

                boolean isAuthenticated = false;
                if (auth != null && StringUtils.isNotEmpty(auth) &&
                        applicationId != null && StringUtils.isNotEmpty(applicationId) && orgId != null && StringUtils.isNotEmpty(orgId))
                {
                    String message = FdahpUserRegWSManager.get(language).validatedUserAppDetailsByAllApi(userId, "", applicationId, orgId);
                    if (StringUtils.isNotEmpty(message) && message.equals(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                    {
                        isAuthenticated = FdahpUserRegWSManager.get(language).validatedAuthKey(auth, applicationId, orgId);
                        if (isAuthenticated)
                        {
                            if (preferencesForm != null && userId != null && StringUtils.isNotEmpty(userId))
                            {
                                if (preferencesForm.getStudies() != null && preferencesForm.getStudies().size() > 0)
                                {
                                    List<StudiesBean> studiesBeenList = preferencesForm.getStudies();
                                    List<ParticipantStudies> existParticipantStudies = FdahpUserRegWSManager.get(language).getParticipantStudiesList(userId, applicationId, orgId);

                                    for (int i = 0; i < studiesBeenList.size(); i++)
                                    {
                                        StudiesBean studiesBean = studiesBeenList.get(i);
                                        boolean isExists = false;
                                        if (existParticipantStudies != null && existParticipantStudies.size() > 0)
                                        {
                                            for (ParticipantStudies participantStudies : existParticipantStudies)
                                            {

                                                if (studiesBean.getStudyId().equalsIgnoreCase(participantStudies.getStudyId())
                                                        && applicationId.equals(participantStudies.getApplicationId())
                                                        && orgId.equals(participantStudies.getOrgId()))
                                                {
                                                    isExists = true;
                                                    if (participantStudies.getStatus() != null && participantStudies.getStatus().equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.YET_TO_JOIN.getValue(language)))
                                                    {
                                                        participantStudies.setEnrolledDate(FdahpUserRegUtil.getCurrentUtilDateTime());
                                                    }
                                                    if (studiesBean.getStatus() != null && StringUtils.isNotEmpty(studiesBean.getStatus()))
                                                    {
                                                        participantStudies.setStatus(studiesBean.getStatus());
                                                        if (studiesBean.getStatus().equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.IN_PROGRESS.getValue(language)))
                                                        {
                                                            List<ParticipantActivities> participantActivitiesList = FdahpUserRegWSManager.get(language).getParticipantActivitiesList(studiesBean.getStudyId(), userId, applicationId, orgId);
                                                            if (participantActivitiesList != null && participantActivitiesList.size() > 0)
                                                            {
                                                                for (ParticipantActivities participantActivities : participantActivitiesList)
                                                                {
                                                                    participantActivities.setActivityVersion(null);
                                                                    participantActivities.setActivityState(null);
                                                                    participantActivities.setActivityRunId(null);
                                                                    participantActivities.setBookmark(false);
                                                                    participantActivities.setTotal(0);
                                                                    participantActivities.setCompleted(0);
                                                                    participantActivities.setMissed(0);
                                                                }
                                                                FdahpUserRegWSManager.get(language).saveParticipantActivities(participantActivitiesList);
                                                            }
                                                            participantStudies.setEnrolledDate(FdahpUserRegUtil.getCurrentUtilDateTime());
                                                        }
                                                    }
                                                    if (studiesBean.getBookmarked() != null)
                                                        participantStudies.setBookmark(studiesBean.getBookmarked());
                                                    if (studiesBean.getCompletion() != null)
                                                        participantStudies.setCompletion(studiesBean.getCompletion());
                                                    if (studiesBean.getAdherence() != null)
                                                        participantStudies.setAdherence(studiesBean.getAdherence());
                                                    if (studiesBean.getParticipantId() != null && StringUtils.isNotEmpty(studiesBean.getParticipantId()))
                                                        participantStudies.setParticipantId(studiesBean.getParticipantId());
                                                    addParticipantStudiesList.add(participantStudies);
                                                }
                                            }
                                        }
                                        if (!isExists)
                                        {
                                            ParticipantStudies participantStudies = new ParticipantStudies();
                                            if (studiesBean.getStudyId() != null && StringUtils.isNotEmpty(studiesBean.getStudyId()))
                                                participantStudies.setStudyId(studiesBean.getStudyId());
                                            if (applicationId != null && StringUtils.isNotEmpty(applicationId))
                                                participantStudies.setApplicationId(applicationId);
                                            if (orgId != null && StringUtils.isNotEmpty(orgId))
                                                participantStudies.setOrgId(orgId);
                                            if (studiesBean.getStatus() != null && StringUtils.isNotEmpty(studiesBean.getStatus()))
                                            {
                                                participantStudies.setStatus(studiesBean.getStatus());
                                                if (studiesBean.getStatus().equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.IN_PROGRESS.getValue(language)))
                                                {
                                                    participantStudies.setEnrolledDate(FdahpUserRegUtil.getCurrentUtilDateTime());
                                                }

                                            }
                                            else
                                            {
                                                participantStudies.setStatus(FdahpUserRegUtil.ErrorCodes.YET_TO_JOIN.getValue(language));
                                            }
                                            if (studiesBean.getBookmarked() != null)
                                                participantStudies.setBookmark(studiesBean.getBookmarked());
                                            if (userId != null && StringUtils.isNotEmpty(userId))
                                                participantStudies.setUserId(userId);
                                            if (studiesBean.getCompletion() != null)
                                                participantStudies.setCompletion(studiesBean.getCompletion());
                                            if (studiesBean.getAdherence() != null)
                                                participantStudies.setAdherence(studiesBean.getAdherence());
                                            if (studiesBean.getParticipantId() != null && StringUtils.isNotEmpty(studiesBean.getParticipantId()))
                                                participantStudies.setParticipantId(studiesBean.getParticipantId());
                                            addParticipantStudiesList.add(participantStudies);
                                        }
                                    }
                                    FdahpUserRegWSManager.get(language).saveParticipantStudies(addParticipantStudiesList);
                                }
                                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase());
                            }
                            else
                            {
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                                return null;
                            }
                        }
                        else
                        {
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(language), getViewContext().getResponse());
                            return null;
                        }
                    }
                    else
                    {
                        if (message.equals(FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language)))
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        else
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }
                }
                else
                {
                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                    return null;
                }
            }
            catch (Exception e)
            {
                _log.error("UpdateStudyState Action Error :", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }

    /**
     * Get the study state of an user
     */
    @Marshal(Marshaller.Jackson)
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class StudyStateAction extends ReadOnlyApiAction<UserForm>
    {

        @Override
        public ApiResponse execute(UserForm userForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            language = getViewContext().getRequest().getHeader("language");
            boolean isAuthenticated = false;
            try
            {
                String auth = getViewContext().getRequest().getHeader("auth");
                String userId = getViewContext().getRequest().getHeader("userId");
                String applicationId = getViewContext().getRequest().getHeader("applicationId");
                String orgId = getViewContext().getRequest().getHeader("orgId");

                if (auth != null && StringUtils.isNotEmpty(auth)
                        && applicationId != null && StringUtils.isNotEmpty(applicationId)
                        && orgId != null && StringUtils.isNotEmpty(orgId))
                {
                    String message = FdahpUserRegWSManager.get(language).validatedUserAppDetailsByAllApi(userId, "", applicationId, orgId);
                    if (StringUtils.isNotEmpty(message) && message.equals(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                    {
                        isAuthenticated = FdahpUserRegWSManager.get(language).validatedAuthKey(auth, applicationId, orgId);
                        if (isAuthenticated)
                        {
                            if (userId != null && StringUtils.isNotEmpty(userId))
                            {
                                response = FdahpUserRegWSManager.get(language).getPreferences(userId, applicationId, orgId);
                            }
                            else
                            {
                                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                                return null;
                            }
                        }
                        else
                        {
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(language), getViewContext().getResponse());
                            return null;
                        }

                    }
                    else
                    {
                        if (message.equals(FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language)))
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(language), FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        else
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }
                }
                else
                {
                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                    return null;
                }
            }
            catch (Exception e)
            {
                _log.error("StudyStateAction Action Error", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;

            }
            return response;
        }
    }

    public static class NotificationForm
    {

        public List<NotificationBean> _notifications;

        public List<NotificationBean> getNotifications()
        {
            return _notifications;
        }

        public void setNotifications(List<NotificationBean> notifications)
        {
            _notifications = notifications;
        }
    }

    /**
     * Send the push notifications
     */
    @Marshal(Marshaller.Jackson)
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class SendNotificationAction extends MutatingApiAction<NotificationForm>
    {

        @Override
        public ApiResponse execute(NotificationForm notificationForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            Map<String, Map<String, JSONArray>> studiesMap = null;
            try
            {
                if (notificationForm != null && notificationForm.getNotifications() != null && !notificationForm.getNotifications().isEmpty())
                {
                    if (!notificationForm.getNotifications().isEmpty())
                    {
                        HashSet<String> studySet = new HashSet<>();
                        HashSet<String> appSet = new HashSet<>();
                        studiesMap = new HashMap<>();

                        for (NotificationBean notificationBean : notificationForm.getNotifications())
                        {
                            if (notificationBean.getNotificationType().equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.STUDY_LEVEL.getValue(language)))
                            {
                                studySet.add(notificationBean.getCustomStudyId());
                                appSet.add(notificationBean.getAppId());
                            }
                            else
                            {
                                appSet.add(notificationBean.getAppId());
                            }
                        }

                        Map<String, JSONArray> allDeviceTokens = FdahpUserRegWSManager.get(language).getDeviceTokenOfAllUsers("'" + StringUtils.join(appSet, "','") + "'");

                        if (studySet != null && !studySet.isEmpty() && appSet != null && !appSet.isEmpty())
                        {
                            studiesMap = FdahpUserRegWSManager.get(language).getStudyLevelDeviceToken("'" + StringUtils.join(studySet, "','") + "'", "'" + StringUtils.join(appSet, "','") + "'");
                            _log.info("studiesMap:" + studiesMap);
                        }
                        for (NotificationBean notificationBean : notificationForm.getNotifications())
                        {
                            if (notificationBean.getNotificationType().equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.GATEWAY_LEVEL.getValue(language)))
                            {
                                notificationBean.setNotificationType(FdahpUserRegUtil.ErrorCodes.GATEWAY.getValue(language));
                                if (allDeviceTokens.get(FdahpUserRegUtil.ErrorCodes.DEVICE_ANDROID.getValue(language)) != null)
                                {
                                    notificationBean.setDeviceToken(allDeviceTokens.get(FdahpUserRegUtil.ErrorCodes.DEVICE_ANDROID.getValue(language)));
                                    if (notificationBean.getDeviceToken() != null && notificationBean.getDeviceToken().length() > 0)
                                    {
                                        pushFCMNotification(notificationBean);
                                    }

                                }
                                if (allDeviceTokens.get(FdahpUserRegUtil.ErrorCodes.DEVICE_IOS.getValue(language)) != null)
                                {
                                    notificationBean.setDeviceToken(allDeviceTokens.get(FdahpUserRegUtil.ErrorCodes.DEVICE_IOS.getValue(language)));
                                    if (notificationBean.getDeviceToken() != null && notificationBean.getDeviceToken().length() > 0)
                                    {
                                        FdahpUserRegUtil.pushNotification(notificationBean, language);
                                    }

                                }
                            }
                            else if (notificationBean.getNotificationType().equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.STUDY_LEVEL.getValue(language)))
                            {
                                Map<String, JSONArray> deviceTokensMap = studiesMap.get(notificationBean.getCustomStudyId());
                                notificationBean.setNotificationType(FdahpUserRegUtil.ErrorCodes.STUDY.getValue(language));
                                if (deviceTokensMap != null)
                                {
                                    if (deviceTokensMap.get(FdahpUserRegUtil.ErrorCodes.DEVICE_ANDROID.getValue(language)) != null)
                                    {
                                        notificationBean.setDeviceToken(deviceTokensMap.get(FdahpUserRegUtil.ErrorCodes.DEVICE_ANDROID.getValue(language)));
                                        if (notificationBean.getDeviceToken() != null && notificationBean.getDeviceToken().length() > 0)
                                        {
                                            pushFCMNotification(notificationBean);
                                        }
                                    }
                                    if (deviceTokensMap.get(FdahpUserRegUtil.ErrorCodes.DEVICE_IOS.getValue(language)) != null)
                                    {
                                        notificationBean.setDeviceToken(deviceTokensMap.get(FdahpUserRegUtil.ErrorCodes.DEVICE_IOS.getValue(language)));
                                        if (notificationBean.getDeviceToken() != null && notificationBean.getDeviceToken().length() > 0)
                                        {
                                            FdahpUserRegUtil.pushNotification(notificationBean, language);
                                        }
                                    }
                                }
                            }

                        }
                        response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase());
                    }
                    else
                    {
                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }

                }
                else
                {
                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                    return null;
                }
            }
            catch (Exception e)
            {
                _log.error("SendNotificationAction Action Error", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(language), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }

    /**
     * Andriod push notification
     *
     * @param notification
     */
    public void pushFCMNotification(NotificationBean notification)
    {
        AppPropertiesDetails appPropertiesDetails = null;
        String authKey = "";
        try
        {
            String FMCurl = (String) configProp.get("API_URL_FCM");
            appPropertiesDetails = FdahpUserRegWSManager.get(language).getAppPropertiesDetailsByAppId(notification.getAppId(), notification.getOrgId());

            if (appPropertiesDetails != null)
            {
                authKey = appPropertiesDetails.getAndroidServerKey();// You FCM AUTH key

                URL url = new URL(FMCurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setUseCaches(false);
                conn.setDoInput(true);
                conn.setDoOutput(true);

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "key=" + authKey);
                conn.setRequestProperty("Content-Type", "application/json");

                JSONObject json = new JSONObject();

                json.put("registration_ids", notification.getDeviceToken());
                json.put("priority", "high");

                JSONObject dataInfo = new JSONObject();
                dataInfo.put("subtype", notification.getNotificationSubType());
                dataInfo.put("type", notification.getNotificationType());
                dataInfo.put("title", notification.getNotificationTitle());
                dataInfo.put("message", notification.getNotificationText());
                if (notification.getCustomStudyId() != null && StringUtils.isNotEmpty(notification.getCustomStudyId()))
                {
                    dataInfo.put("studyId", notification.getCustomStudyId());
                }
                json.put("data", dataInfo);
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(json.toString());
                wr.flush();
                conn.getInputStream();
            }
        }
        catch (Exception e)
        {
            _log.error("pushFCMNotification Action Error", e);
        }
    }

    /**
     * Generate the consent doc as pdf file
     */
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class GenerateTheFileAction extends ReadOnlyApiAction<StudyConsent>
    {

        @Override
        public ApiResponse execute(StudyConsent StudyConsent, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            List<StudyConsent> studyConsentList = FdahpUserRegWSManager.get(language).getStudyConsentList();
            FileContentService fileContentService = ServiceRegistry.get().getService(FileContentService.class);
            //File root = fileContentService.getDefaultRoot(getViewContext().getContainer(), true);
            File root = fileContentService.getFileRoot(getViewContext().getContainer(), FileContentService.ContentType.files);
            if (!root.exists())
                root.mkdirs();
            File rootModule = null;
            if (root != null)
            {
                rootModule = new File(root, FdahpUserRegWSModule.NAME);
                if (!rootModule.exists())
                    rootModule.mkdirs();
                _log.info(rootModule);

            }
            if (studyConsentList != null && studyConsentList.size() > 0)
            {
                for (StudyConsent studyConsent : studyConsentList)
                {
                    if (rootModule != null)
                    {
                        File dir = new File(rootModule, studyConsent.getStudyId());
                        if (!dir.exists())
                            dir.mkdir();
                        String fileName = FdahpUserRegUtil.getStandardFileName(studyConsent.getStudyId(), studyConsent.getUserId(), studyConsent.getVersion());
                        _log.info(fileName);
                        try
                        {
                            byte[] decodedBytes;
                            FileOutputStream fop;
                            decodedBytes = Base64.getDecoder().decode(studyConsent.getPdf());
                            File file = new File(dir, fileName);
                            fop = new FileOutputStream(file);
                            fop.write(decodedBytes);
                            fop.flush();
                            fop.close();
                        }
                        catch (Exception e)
                        {
                            _log.error("Error..", e);
                            response.put("message", "Fail");
                        }
                    }

                }
            }
            response.put("message", "SUCESS");
            response.put("status", studyConsentList.size() + " Files Created");
            return response;
        }
    }

    /**
     * saving the generated the consent docmunet in folder path
     *
     * @param studyConsent
     * @return
     */
    public String saveConsentDocument(StudyConsent studyConsent)
    {
        String fileName = "";
        try
        {
            FileContentService fileContentService = ServiceRegistry.get().getService(FileContentService.class);
            File root = fileContentService.getFileRoot(getViewContext().getContainer(), FileContentService.ContentType.files);
            if (!root.exists())
                root.mkdirs();
            File rootModule = null;
            if (root != null)
            {
                rootModule = new File(root, FdahpUserRegWSModule.NAME);
                if (!rootModule.exists())
                    rootModule.mkdirs();
            }
            if (rootModule != null)
            {
                File dir = new File(rootModule, studyConsent.getStudyId());
                if (!dir.exists())
                    dir.mkdir();
                fileName = FdahpUserRegUtil.getStandardFileName(studyConsent.getStudyId(), studyConsent.getUserId(), studyConsent.getVersion());
                _log.info(fileName);
                try
                {
                    byte[] decodedBytes;
                    FileOutputStream fop;
                    decodedBytes = Base64.getDecoder().decode(studyConsent.getPdf().replaceAll("\n", ""));
                    File file = new File(dir, fileName);
                    fop = new FileOutputStream(file);
                    fop.write(decodedBytes);
                    fop.flush();
                    fop.close();
                }
                catch (Exception e)
                {
                    _log.error("FdahpUserRegWSController saveConsentDocument:", e);
                }
            }
        }
        catch (Exception e)
        {
            _log.error("FdahpUserRegWSController saveConsentDocument:", e);
        }
        return fileName;
    }

    public static class RefreshTokenForm
    {

        public String _refreshToken;

        public String getRefreshToken()
        {
            return _refreshToken;
        }

        public void setRefreshToken(String refreshToken)
        {
            _refreshToken = refreshToken;
        }
    }

    /**
     * Refresh of an auth key
     */
    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class RefreshTokenAction extends MutatingApiAction<RefreshTokenForm>
    {
        @Override
        public ApiResponse execute(RefreshTokenForm refreshTokenForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            AuthInfo existedAuthInfo = null;
            String applicationId = getViewContext().getRequest().getHeader("applicationId");
            String orgId = getViewContext().getRequest().getHeader("orgId");
            language = getViewContext().getRequest().getHeader("language");

            try
            {
                if (refreshTokenForm != null && applicationId != null && StringUtils.isNotEmpty(applicationId)
                        && orgId != null && StringUtils.isNotEmpty(orgId))
                {
                    if ((refreshTokenForm.getRefreshToken() != null && StringUtils.isNotEmpty(refreshTokenForm.getRefreshToken())))
                    {
                        existedAuthInfo = FdahpUserRegWSManager.get(language).getAuthInfoByRefreshToken(refreshTokenForm.getRefreshToken());
                        if (null != existedAuthInfo)
                        {
                            AuthInfo authInfo = FdahpUserRegWSManager.get(language).saveAuthInfo(existedAuthInfo.getParticipantId(), false, applicationId, orgId);
                            if (authInfo != null)
                            {
                                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(language), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase());
                                response.put("userId", authInfo.getParticipantId());
                                response.put("auth", authInfo.getAuthKey());
                                response.put("refreshToken", authInfo.getRefreshToken());
                            }
                        }
                        else
                        {
                            FdahpUserRegWSManager.addAuditEvent(null, "FAILED RefreshToken IN", "Wrong RefreshToken. Which is not existed.", "FdaUserAuditEvent", getContainer_AppID(applicationId, orgId).getId());
                            FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_103.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_REFRESHTOKEN.name(), FdahpUserRegUtil.ErrorCodes.INVALID_REFRESHTOKEN.getValue(language), getViewContext().getResponse());
                            return null;
                        }
                    }
                    else
                    {
                        FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.name(), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                        return null;
                    }
                }
                else
                {
                    FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.name(), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(language), getViewContext().getResponse());
                    return null;
                }


            }
            catch (Exception e)
            {
                _log.error("Login Action:", e);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(language), FdahpUserRegUtil.ErrorCodes.UNKNOWN.name(), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(language), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }

    /**
     * saving the consent documents into particular study folder
     *
     * @param studyConsent
     * @return
     */
    public String saveStudyConsentDocument(StudyConsent studyConsent)
    {
        String fileName = "";
        try
        {
            FileContentService fileContentService = ServiceRegistry.get().getService(FileContentService.class);
            Container availableContainer = null;
            boolean isAvailable = false;
            Module module = ModuleLoader.getInstance().getModule(FdahpUserRegWSModule.NAME);
            Set<Container> all = ContainerManager.getAllChildren(ContainerManager.getRoot());

            ModuleProperty mp = module.getModuleProperties().get("StudyId");
            String postedStudyId = studyConsent.getStudyId();
            File root = null;
            for (Container c : all)
            {
                String studyId = mp.getValueContainerSpecific(c);
                if (postedStudyId.equalsIgnoreCase(studyId))
                {
                    isAvailable = true;
                    root = fileContentService.getFileRoot(c, FileContentService.ContentType.files);
                    break;
                }
            }
            if (isAvailable)
            {
                if (!root.exists())
                    root.mkdirs();
                fileName = FdahpUserRegUtil.getStandardFileName(studyConsent.getStudyId(), studyConsent.getUserId(), studyConsent.getVersion());
                _log.info(fileName);
                try
                {
                    byte[] decodedBytes;
                    FileOutputStream fop;
                    decodedBytes = Base64.getDecoder().decode(studyConsent.getPdf().replaceAll("\n", ""));
                    File file = new File(root, fileName);
                    fop = new FileOutputStream(file);
                    fop.write(decodedBytes);
                    fop.flush();
                    fop.close();
                }
                catch (Exception e)
                {
                    _log.error("FdahpUserRegWSController saveStudyConsentDocument:", e);
                }
            }
            else
            {
                fileName = saveConsentDocument(studyConsent);
            }

        }
        catch (Exception e)
        {
            _log.error("FdahpUserRegWSController saveStudyConsentDocument:", e);
        }
        return fileName;
    }

    @RequiresPermission(AdminOperationsPermission.class)
    public class ShowAppPropertiesUpdateAction extends SimpleViewAction<Object>
    {
        @Override
        public ModelAndView getView(Object o, BindException errors)
        {
            return new JspView<>("/com/hphc/mystudies/appProperties.jsp");
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            urlProvider(AdminUrls.class).addAdminNavTrail(root, "Update Registration Server Properties", AppPropertiesUpdateAction.class, getContainer());
        }
    }

    @RequiresPermission(AdminOperationsPermission.class)
    public class AppPropertiesUpdateAction extends MutatingApiAction<AppPropertiesDetailsBean>
    {
        @Override
        public Object execute(AppPropertiesDetailsBean appPropertiesDetailsBean, BindException errors) throws Exception
        {
            String message = null;
            boolean success = false;
            ApiSimpleResponse apiSimpleResponse = new ApiSimpleResponse();

            try
            {
                if (appPropertiesDetailsBean.getAppId() != null)
                {
                    //add code for user data partition
                    AppPropertiesDetails appPropertiesDetails = new AppPropertiesDetails();

                    appPropertiesDetails.setAppId(appPropertiesDetailsBean.getAppId());
                    appPropertiesDetails.setOrgId(appPropertiesDetailsBean.getOrgId());
                    appPropertiesDetails.setIosBundleId(appPropertiesDetailsBean.getIosBundleId());
                    appPropertiesDetails.setAndroidBundleId(appPropertiesDetailsBean.getAndroidBundleId());
                    appPropertiesDetails.setAndroidServerKey(appPropertiesDetailsBean.getAndroidServerKey());
                    appPropertiesDetails.setIosCertificate(appPropertiesDetailsBean.getIosCertificate());
                    appPropertiesDetails.setIosCertificatePassword(appPropertiesDetailsBean.getIosCertificatePassword());

                    appPropertiesDetails.setEmail(appPropertiesDetailsBean.getEmail());
                    appPropertiesDetails.setEmailPassword(appPropertiesDetailsBean.getEmailPassword());

                    appPropertiesDetails.setRegEmailSub(appPropertiesDetailsBean.getRegisterEmailSubject());
                    appPropertiesDetails.setRegEmailBody(appPropertiesDetailsBean.getRegisterEmailBody());
                    appPropertiesDetails.setForgotEmailSub(appPropertiesDetailsBean.getForgotPassEmailSubject());
                    appPropertiesDetails.setForgotEmailBody(appPropertiesDetailsBean.getForgotPassEmailBody());

                    appPropertiesDetails.setRegEmailSubSpanish(appPropertiesDetailsBean.getRegisterEmailSubjectSpanish());
                    appPropertiesDetails.setRegEmailBodySpanish(appPropertiesDetailsBean.getRegisterEmailBodySpanish());
                    appPropertiesDetails.setForgotEmailSubSpanish(appPropertiesDetailsBean.getForgotPassEmailSubjectSpanish());
                    appPropertiesDetails.setForgotEmailBodySpanish(appPropertiesDetailsBean.getForgotPassEmailBodySpanish());

                    appPropertiesDetails.setMethodHandler(appPropertiesDetailsBean.isMethodHandler());

                    appPropertiesDetails.setFeedbackEmail(appPropertiesDetailsBean.getFeedbackEmail());
                    appPropertiesDetails.setContactUsEmail(appPropertiesDetailsBean.getContactUsEmail());
                    appPropertiesDetails.setAppName(appPropertiesDetailsBean.getAppName());

                    appPropertiesDetails.setCreatedOn(new Date());

                    message = FdahpUserRegWSManager.get(language).saveAppPropertiesDetails(appPropertiesDetails);
                    if (message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language)))
                        success = true;
                }
            }
            catch (Exception e)
            {
                message = e.getMessage();
            }

            apiSimpleResponse.put("success", success);

            if (success)
            {
                apiSimpleResponse.put("returnUrl", urlProvider(AdminUrls.class).getAdminConsoleURL());
            }
            else
            {
                apiSimpleResponse.put("message", null != message ? message : FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(language));
            }

            return apiSimpleResponse;
        }
    }

    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class FeedbackAction extends MutatingApiAction<FeedbackAndContactUsBean>
    {
        @Override
        public ApiResponse execute(FeedbackAndContactUsBean feedbackAndContactUsBean, BindException errors) throws Exception
        {
            ApiSimpleResponse apiSimpleResponse = new ApiSimpleResponse();
            String applicationId = getViewContext().getRequest().getHeader("applicationId");
            String orgId = getViewContext().getRequest().getHeader("orgId");
            language = getViewContext().getRequest().getHeader("language");

            if (StringUtils.isNotEmpty(applicationId) && StringUtils.isNotEmpty(orgId))
            {
                AppPropertiesDetails appPropertiesDetails = FdahpUserRegWSManager.get(language).getAppPropertiesDetailsByAppId(applicationId, orgId);

                String subject = "App Feedback: " + feedbackAndContactUsBean.getSubject();
                String msg = "<p>Hi</p>\n" +
                        "<p>&nbsp;</p>\n" +
                        "<p>A user of the " + appPropertiesDetails.getAppName() + " mobile app has provided feedback via the app. Here's the content of the feedback:</p>\n" +
                        "<p>&nbsp;</p>\n" +
                        "<p>" + feedbackAndContactUsBean.getBody() + "</p>\n" +
                        "<p>&nbsp;</p>\n" +
                        "<p>Thanks,</p>\n" +
                        "<p>The FDA MyStudies Support Team</p>\n" +
                        "<p>---------------------------------------------------------</p>\n" +
                        "<p>This is an auto-generated email. Please do not reply.</p>";

                FdahpUserRegUtil.sendMessage(subject, msg, appPropertiesDetails.getFeedbackEmail(), appPropertiesDetails);
                apiSimpleResponse.put(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase(), true);
            }
            else
            {
                apiSimpleResponse.put(FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(language).toLowerCase(), true);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.name(), FdahpUserRegUtil.ErrorCodes.FEEDBACK_NOT_SENT.getValue(language), getViewContext().getResponse());
            }
            return apiSimpleResponse;
        }
    }

    @CSRF(CSRF.Method.NONE)
    @RequiresNoPermission
    public class ContactUsAction extends MutatingApiAction<FeedbackAndContactUsBean>
    {
        @Override
        public ApiResponse execute(FeedbackAndContactUsBean feedbackAndContactUsBean, BindException errors) throws Exception
        {
            ApiSimpleResponse apiSimpleResponse = new ApiSimpleResponse();
            String applicationId = getViewContext().getRequest().getHeader("applicationId");
            String orgId = getViewContext().getRequest().getHeader("orgId");
            language = getViewContext().getRequest().getHeader("language");

            if (applicationId != null && StringUtils.isNotEmpty(applicationId)
                    && orgId != null && StringUtils.isNotEmpty(orgId))
            {
                AppPropertiesDetails appPropertiesDetails = FdahpUserRegWSManager.get(language).getAppPropertiesDetailsByAppId(applicationId, orgId);

                String subject = "App HelpDesk: " + feedbackAndContactUsBean.getSubject();
                String msg = "<p>Hi</p>\n" +
                        "<p>A user of the " + appPropertiesDetails.getAppName() + " mobile app has reached out to the Helpdesk. Below are the contact form details:</p>\n" +
                        "<p>___________________________________________</p>\n" +
                        "<p>First Name: " + feedbackAndContactUsBean.getFirstName() + "</p>\n" +
                        "<p>Email: " + feedbackAndContactUsBean.getEmail() + "</p>\n" +
                        "<p>Subject: " + feedbackAndContactUsBean.getSubject() + "</p>\n" +
                        "<p>Message: " + feedbackAndContactUsBean.getBody() + "</p>\n" +
                        "<p>___________________________________________</p>\n" +
                        "<p>Please respond to the app user at the email he/she has provided.</p>\n" +
                        "<p>Thanks,</p>\n" +
                        "<p>The FDA MyStudies Support Team</p>\n" +
                        "<p>---------------------------------------------------------</p>\n" +
                        "<p>This is an auto-generated email. Please do not reply.</p>";

                FdahpUserRegUtil.sendMessage(subject, msg, appPropertiesDetails.getContactUsEmail(), appPropertiesDetails);
                apiSimpleResponse.put(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue(language).toLowerCase(), true);
            }
            else
            {
                apiSimpleResponse.put(FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(language).toLowerCase(), true);
                FdahpUserRegUtil.getFailureResponse(language, FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(language), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.name(), FdahpUserRegUtil.ErrorCodes.INJUIRY_NOT_SENT.getValue(language), getViewContext().getResponse());
            }

            return apiSimpleResponse;
        }
    }


    private Container getContainer_AppID(String postedAppId, String postedOrgId)
    {
        Container appIdContainer = null;
        Container orgIdContainer = null;
        Module module = ModuleLoader.getInstance().getModule(FdahpUserRegWSModule.NAME);
        ModuleProperty mp = module.getModuleProperties().get("StudyId");

        List<Container> all = ContainerManager.getChildren(ContainerManager.getRoot());
        for (Container orgContainer : all)
        {
            if (postedOrgId.equalsIgnoreCase(mp.getValueContainerSpecific(orgContainer)))
            {
                orgIdContainer = orgContainer;
                List<Container> allApp = ContainerManager.getChildren(orgContainer);
                for (Container appContainer : allApp)
                {
                    if (postedAppId.equalsIgnoreCase(mp.getValueContainerSpecific(appContainer)))
                    {
                        appIdContainer = appContainer;
                        break;
                    }
                }
            }
        }
        if (appIdContainer == null)
        {
            _log.error("container not available for AppID " + postedAppId);
            if (orgIdContainer == null)
                _log.error("container not available for OrgID " + postedOrgId);
            return orgIdContainer;
        }
        else
        {
            return appIdContainer;
        }
    }
}