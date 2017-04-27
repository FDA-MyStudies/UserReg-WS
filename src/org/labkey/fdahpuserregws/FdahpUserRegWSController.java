/*
 * Copyright (c) 2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.fdahpuserregws;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.action.ApiAction;
import org.labkey.api.action.ApiResponse;
import org.labkey.api.action.ApiSimpleResponse;
import org.labkey.api.action.Marshal;
import org.labkey.api.action.Marshaller;
import org.labkey.api.action.ReturnUrlForm;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.security.RequiresNoPermission;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.fdahpuserregws.bean.ActivitiesBean;
import org.labkey.fdahpuserregws.bean.ConsentBean;
import org.labkey.fdahpuserregws.bean.InfoBean;
import org.labkey.fdahpuserregws.bean.ParticipantForm;
import org.labkey.fdahpuserregws.bean.ParticipantInfoBean;
import org.labkey.fdahpuserregws.bean.ProfileBean;
import org.labkey.fdahpuserregws.bean.SettingsBean;
import org.labkey.fdahpuserregws.bean.StudiesBean;
import org.labkey.fdahpuserregws.model.AuthInfo;
import org.labkey.fdahpuserregws.model.FdahpUserRegUtil;
import org.labkey.fdahpuserregws.model.ParticipantActivities;
import org.labkey.fdahpuserregws.model.ParticipantStudies;
import org.labkey.fdahpuserregws.model.PasswordHistory;
import org.labkey.fdahpuserregws.model.StudyConsent;
import org.labkey.fdahpuserregws.model.UserDetails;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.labkey.api.util.GUID;

import javax.activation.MimeType;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.labkey.api.module.Module;
import org.labkey.api.settings.LookAndFeelProperties;
import org.labkey.api.module.ModuleLoader;

public class FdahpUserRegWSController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(FdahpUserRegWSController.class);
    public static final String NAME = "fdahpuserregws";

    public FdahpUserRegWSController()
    {
        setActionResolver(_actionResolver);
    }

    private static final Logger _log = Logger.getLogger(FdahpUserRegWSController.class);

    Properties configProp = FdahpUserRegUtil.getProperties();

    @RequiresNoPermission
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return new JspView("/org/labkey/fdahpuserregws/view/hello.jsp");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }

    @RequiresNoPermission
    public class PingAction extends ApiAction<Object>
    {

        @Override
        public ApiResponse execute(Object o, BindException errors) throws Exception
        {
            UserDetails participantDetails = new UserDetails();
            ApiSimpleResponse apiSimpleResponse = new ApiSimpleResponse();
            apiSimpleResponse.put("reponse", "FdahpUserRegWebServices Works!");
            /*String email = LookAndFeelProperties.getInstance(getContainer()).getSystemEmailAddress();
            apiSimpleResponse.put("email", email);*/
            apiSimpleResponse.put(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase(), true);
            return apiSimpleResponse;
        }
    }
    @RequiresNoPermission
    public class RegisterAction extends ApiAction<ParticipantForm>{

        @Override
        protected ModelAndView handleGet() throws Exception
        {
            getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the POST method when calling this action.");
            return null;
        }

        @Override
        public Object execute(ParticipantForm participantForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            UserDetails addParticipantDetails=null;
            try{
                if((participantForm.getEmailId() != null && StringUtils.isNotEmpty(participantForm.getEmailId())) && (participantForm.getPassword() != null && StringUtils.isNotEmpty(participantForm.getPassword()))){
                    List<UserDetails> participantDetails = FdahpUserRegWSManager.get().getParticipantDetailsListByEmail(participantForm.getEmailId());
                    if(participantDetails != null && participantDetails.size() > 0){
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), FdahpUserRegUtil.ErrorCodes.EMAIL_EXISTS.getValue(), getViewContext().getResponse());
                        //errors.rejectValue("email",ERROR_MSG,FdahpUserRegUtil.ErrorCodes.EMAIL_EXISTS.getValue());
                        return null;
                    }else{
                        addParticipantDetails = FdahpUserRegWSManager.get().saveParticipant(getParticipant(participantForm));
                        if(addParticipantDetails != null){
                            response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                            response.put("userId", addParticipantDetails.getUserId());
                            if(addParticipantDetails.getStatus() != null)
                                if(addParticipantDetails.getStatus() == 2)
                                    response.put("verified", false);
                            if(addParticipantDetails.getStatus() == 1)
                                response.put("verified", true);
                            if(addParticipantDetails.getId() != null)
                            {
                                AuthInfo authInfo = FdahpUserRegWSManager.get().saveAuthInfo(addParticipantDetails.getUserId());
                                if(authInfo != null){
                                    response.put("auth", authInfo.getAuthKey());
                                }
                            }
                            String message = "<html>" +
                                    "<body>" +
                                    "<div style='margin:20px;padding:10px;font-family: sans-serif;font-size: 14px;'>" +
                                    "<span>Hi,</span><br/><br/>" +
                                    "<span>Thanks for registering with us! We look forward to having you on board and actively taking part in<br/>Health Studies being conducted by the FDA and its partners.</span><br/><br/>" +
                                    "<span>Your sign-up process is almost complete. Please use the Verification Code provided below to<br/>verify your email in the mobile app. </span><br/><br/>" +
                                    "<span><strong>Verification Code:</strong>" +addParticipantDetails.getSecurityToken()+ "</span><br/><br/>" +
                                    "<span>This code can be used only once and is valid for a period of 48 hours only.</span><br/><br/>" +
                                    "<span>Please note that  registration (or sign up) for the app  is requested only to provide you with a <br/>seamless experience of using the app. Your registration information does not become part of <br/>the data collected for any study(ies) housed in the app. Each study has its own consent process <br/> and your data for the study will not be collected without you providing your informed consent prior<br/> to joining the study. </span><br/><br/>"+
                                    "<span>For any questions or assistance, please write to <a>"+configProp.get("support.email")+"</a> </span><br/><br/>" +
                                    "<span style='font-size:15px;'>Thanks,</span><br/><span>The FDA Health Studies Gateway Team</span>" +
                                    "<br/><span>----------------------------------------------------</span><br/>" +
                                    "<span style='font-size:10px;'>PS - This is an auto-generated email. Please do not reply.</span>" +
                                    "</div>" +
                                    "</body>" +
                                    "</html>";
                           //FdahpUserRegUtil.sendMessage("Welcome to the FDA Health Studies Gateway!",message,addParticipantDetails.getEmail());
                            boolean isMailSent = FdahpUserRegUtil.sendemail(addParticipantDetails.getEmail(),"Welcome to the FDA Health Studies Gateway!",message);
                            if (isMailSent){
                                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                            }else{
                                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.FAILURE_TO_SENT_MAIL.getValue(), getViewContext().getResponse());
                                return null;
                            }
                            response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                        }else{
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(), getViewContext().getResponse());
                            return null;
                        }
                    }
                }else{
                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                   /* if (StringUtils.trimToNull(participantForm.getFirstName()) == null)
                        errors.rejectValue("firstName",ERROR_MSG,"First Name is required.");
                    if (StringUtils.trimToNull(participantForm.getLastName()) == null)
                        errors.rejectValue("lastName",ERROR_MSG,  "Last Name is required.");*/
                    if (StringUtils.trimToNull(participantForm.getEmailId()) == null)
                        errors.rejectValue("emailId",ERROR_MSG,"email is required.");
                    if (StringUtils.trimToNull(participantForm.getPassword()) == null)
                        errors.rejectValue("password",ERROR_MSG,"password is required.");
                }
            }catch (Exception e){
                 _log.error("register action:",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                return null;

            }
            return response;
        }
    }
    public static class UserForm {

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

    @RequiresNoPermission
    public class ConfirmRegistrationAction extends ApiAction{

        @Override
        public Object execute(Object o, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            try{
                String userId = getViewContext().getRequest().getHeader("userId");
                String auth = getViewContext().getRequest().getHeader("auth");
                boolean isAuthenticated = false;
                if(auth != null && StringUtils.isNotEmpty(auth))
                {
                    isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(auth);
                    if (isAuthenticated)
                    {
                        if(userId != null && StringUtils.isNotEmpty(userId)){
                            UserDetails participantDetails =  FdahpUserRegWSManager.get().getParticipantDetails(userId);
                            if(participantDetails != null){
                                if(participantDetails.getStatus() == 2)
                                    response.put("verified", false);
                                if(participantDetails.getStatus() == 1)
                                    response.put("verified", true);
                                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                            }else{
                                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.NO_DATA_AVAILABLE.getValue(),FdahpUserRegUtil.ErrorCodes.NO_DATA_AVAILABLE.getValue(), getViewContext().getResponse());
                                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(), FdahpUserRegUtil.ErrorCodes.FAILURE.getValue().toLowerCase());
                            }
                        }else{
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                            return null;
                        }
                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                        return null;
                    }
                }

            }catch (Exception e){
                _log.error("ConfirmRegistration action:",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }
    public static class VerificationForm {

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
    @RequiresNoPermission
    public class VerifyAction extends ApiAction<VerificationForm>{

        @Override
        protected ModelAndView handleGet() throws Exception
        {
            getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the POST method when calling this action.");
            return null;
        }

        @Override
        public Object execute(VerificationForm verificationForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            try{
               // String securityToken = getViewContext().getRequest().getParameter("token");
                if(verificationForm != null){
                    if((verificationForm.getEmailId() != null && StringUtils.isNotEmpty(verificationForm.getEmailId())) &&
                            (verificationForm.getCode() != null && StringUtils.isNotEmpty(verificationForm.getCode()))){
                        //UserDetails participantDetails = participantDetails = FdahpUserRegWSManager.get().getParticipantDetailsByToken(verificationForm.getEmailId(),verificationForm.getCode());
                        UserDetails participantDetails = participantDetails = FdahpUserRegWSManager.get().getParticipantDetailsByEmail(verificationForm.getEmailId());
                        if(null != participantDetails){
                            if(participantDetails.getSecurityToken() != null && participantDetails.getSecurityToken().equalsIgnoreCase(verificationForm.getCode())){
                                if(participantDetails.getStatus() == 2){
                                    Date validateDate  = FdahpUserRegUtil.addHours(FdahpUserRegUtil.getCurrentDateTime(),48);
                                    if(FdahpUserRegUtil.getCurrentUtilDateTime().before(validateDate) || FdahpUserRegUtil.getCurrentUtilDateTime().equals(validateDate)){
                                        participantDetails.setStatus(1);
                                        //participantDetails.setSecurityToken(null);
                                        participantDetails.setVerificationDate(FdahpUserRegUtil.getCurrentUtilDateTime());
                                        UserDetails updateParticipantDetails = FdahpUserRegWSManager.get().saveParticipant(participantDetails);
                                        if(null != updateParticipantDetails){
                                            response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                                        }else{
                                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                                            return null;
                                        }
                                    }else{
                                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_103.getValue(),FdahpUserRegUtil.ErrorCodes.CODE_EXPIRED.getValue(),FdahpUserRegUtil.ErrorCodes.CODE_EXPIRED.getValue(), getViewContext().getResponse());
                                        return null;
                                    }
                                }else{
                                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_103.getValue(),FdahpUserRegUtil.ErrorCodes.USER_ALREADY_VERIFIED.getValue(),FdahpUserRegUtil.ErrorCodes.USER_ALREADY_VERIFIED.getValue(), getViewContext().getResponse());
                                    return null;
                                }

                            }else{
                                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_CODE.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_CODE.getValue(), getViewContext().getResponse());
                                return null;
                            }
                        }else{
                             FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.EMAIL_NOT_EXISTS.getValue(),FdahpUserRegUtil.ErrorCodes.EMAIL_NOT_EXISTS.getValue(), getViewContext().getResponse());
                             return null;
                        }
                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                        return null;
                    }
                }else{
                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                    return null;
                }
            }catch (Exception e){
                _log.error("ConfirmRegistration action:",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }

    @RequiresNoPermission
    public class LoginAction extends ApiAction<LoginForm>{

        @Override
        protected ModelAndView handleGet() throws Exception
        {
            getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the POST method when calling this action.");
            return null;
        }

        @Override
        public ApiResponse execute(LoginForm loginForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            //ParticipantForm participantForm = null;
            UserDetails participantDetails = null;
            try{
                if(loginForm != null){
                   // String email = getViewContext().getRequest().getHeader("emailId");
                   // String password = getViewContext().getRequest().getHeader("password");
                    if((loginForm.getEmailId() != null && StringUtils.isNotEmpty(loginForm.getEmailId())) && (loginForm.getPassword() != null && StringUtils.isNotEmpty(loginForm.getPassword()))){
                        //participantForm= FdahpUserRegWSManager.get().signingParticipant(loginForm.getEmailId(),loginForm.getPassword());
                        participantDetails = FdahpUserRegWSManager.get().getParticipantDetailsByEmail(loginForm.getEmailId());
                        if(null != participantDetails){
                            if(participantDetails.getPassword() != null && participantDetails.getPassword().equalsIgnoreCase(FdahpUserRegUtil.getEncryptedString(loginForm.getPassword()))){
                                if(participantDetails.getTempPassword()){
                                    participantDetails.setResetPassword(null);
                                    participantDetails.setTempPassword(false);
                                    participantDetails.setTempPasswordDate(FdahpUserRegUtil.getCurrentUtilDateTime());
                                    FdahpUserRegWSManager.get().saveParticipant(participantDetails);
                                }
                                AuthInfo authInfo = FdahpUserRegWSManager.get().saveAuthInfo(participantDetails.getUserId());
                                if(authInfo != null){
                                    response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                                    response.put("userId",participantDetails.getUserId());
                                    response.put("auth",authInfo.getAuthKey());
                                    if(participantDetails.getStatus() == 2)
                                    {
                                        response.put("verified", false);
                                    }
                                    if(participantDetails.getStatus() == 1){
                                        response.put("verified", true);
                                    }
                                }  else{
                                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.name(), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                                    return null;
                                }

                            }else if(participantDetails.getResetPassword() != null && participantDetails.getResetPassword().equalsIgnoreCase(FdahpUserRegUtil.getEncryptedString(loginForm.getPassword()))){
                                if(participantDetails.getTempPassword()){
                                    Date validateDate  = FdahpUserRegUtil.addHours(FdahpUserRegUtil.getCurrentDateTime(),48);
                                    if(participantDetails.getTempPasswordDate().before(validateDate) || participantDetails.getTempPasswordDate().equals(validateDate)){
                                        AuthInfo authInfo = FdahpUserRegWSManager.get().saveAuthInfo(participantDetails.getUserId());
                                        if(authInfo != null){
                                            response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                                            response.put("userId",participantDetails.getUserId());
                                            response.put("auth",authInfo.getAuthKey());
                                            if(participantDetails.getStatus() == 2)
                                            {
                                                response.put("verified", false);
                                            }
                                            if(participantDetails.getStatus() == 1){
                                                response.put("verified", true);
                                            }
                                            response.put("resetPassword", participantDetails.getTempPassword());
                                        } else{
                                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.name(), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                                            return null;
                                        }
                                    }else{
                                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_103.getValue(),FdahpUserRegUtil.ErrorCodes.CODE_EXPIRED.getValue(),FdahpUserRegUtil.ErrorCodes.CODE_EXPIRED.getValue(), getViewContext().getResponse());
                                        return null;
                                    }
                                }
                            }else{
                                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_USERNAME_PASSWORD_MSG.name(), FdahpUserRegUtil.ErrorCodes.INVALID_USERNAME_PASSWORD_MSG.getValue(), getViewContext().getResponse());
                                errors.rejectValue("emailId",ERROR_MSG,  "emailId is wrong.");
                                errors.rejectValue("password",ERROR_MSG,  "password is wrong.");
                            }

                        }else{
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_CREDENTIALS.name(), FdahpUserRegUtil.ErrorCodes.INVALID_CREDENTIALS.getValue(), getViewContext().getResponse());
                            return null;
                        }
                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.name(), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                        return null;
                    }
                }else{
                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.name(), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                    errors.rejectValue("emailId",ERROR_MSG,  "emailId is required.");
                    errors.rejectValue("password",ERROR_MSG,  "password is required.");
                    //return null;
                }


            }catch (Exception e){
                _log.error("Login Action:",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.name(), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }


    private UserDetails getParticipant(ParticipantForm form){
        UserDetails participantDetails = null;
        if(null != form.getUserId()){
            participantDetails = FdahpUserRegWSManager.get().getParticipantDetails(form.getUserId());
        }
        if (participantDetails == null){
            participantDetails = new UserDetails();
            participantDetails.setStatus(2);
            String token = RandomStringUtils.randomAlphanumeric(6);
            participantDetails.setSecurityToken(token);
            participantDetails.setVerificationDate(FdahpUserRegUtil.getCurrentUtilDateTime());

            String userId = UUID.randomUUID().toString();
            participantDetails.setUserId(userId);
        }
        if(form.getFirstName() != null)
            participantDetails.setFirstName(form.getFirstName());
        if(form.getLastName() != null)
            participantDetails.setLastName(form.getLastName());
        if(form.getEmailId() !=null)
            participantDetails.setEmail(form.getEmailId());
        if(form.getPassword() != null)
            participantDetails.setPassword(FdahpUserRegUtil.getEncryptedString(form.getPassword()));
        if(form.getUsePassCode() != null)
            participantDetails.setUsePassCode(form.getUsePassCode());
        if(form.getLocalNotification() != null)
            participantDetails.setLocalNotificationFlag(form.getLocalNotification());
        if(form.getRemoteNotification() != null)
            participantDetails.setRemoteNotificationFlag(form.getRemoteNotification());
        if(form.getTouchId() != null)
            participantDetails.setTouchId(form.getTouchId());
        return participantDetails;
    }
    @RequiresNoPermission
    public class ForgotPasswordAction extends ApiAction<LoginForm>
    {

        @Override
        protected ModelAndView handleGet() throws Exception
        {
            getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the POST method when calling this action.");
            return null;
        }

        @Override
        public ApiResponse execute(LoginForm loginForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            try{
                if(loginForm != null && loginForm.getEmailId() != null && StringUtils.isNotEmpty(loginForm.getEmailId())){
                    UserDetails participantDetails = FdahpUserRegWSManager.get().getParticipantDetailsByEmail(loginForm.getEmailId());
                    if(participantDetails != null){
                        if(participantDetails.getStatus() == 1){
                            String tempPassword = RandomStringUtils.randomAlphanumeric(6);
                            //participantDetails.setPassword(FdahpUserRegUtil.getEncryptedString(tempPassword));
                            participantDetails.setTempPassword(true);
                            participantDetails.setResetPassword(FdahpUserRegUtil.getEncryptedString(tempPassword));
                            participantDetails.setTempPasswordDate(FdahpUserRegUtil.getCurrentUtilDateTime());
                            UserDetails upParticipantDetails = FdahpUserRegWSManager.get().saveParticipant(participantDetails);
                            if(upParticipantDetails != null){
                                String message="<html>" +
                                        "<body>" +
                                        "<div style='margin:20px;padding:10px;font-family: sans-serif;font-size: 14px;'>" +
                                        "<span>Hi,</span><br/><br/>" +
                                        "<span>Thanks for reaching out for password help.</span><br/><br/>" +
                                        "<span>Given below is a temporary password which you can use to sign in to the FDA Health Studies<br/> Gateway App. You will be required to set up a new password after signing in.</span><br/><br/>" +
                                        "<span><strong>Temporary Password:</strong> " + tempPassword + "</span><br/><br/>" +
                                        "<span>Please note that this temporary password can be used only once and is valid for a period of 48 hours only.</span><br/><br/>" +
                                        "<span>For any questions or assistance, please write to <a href='mailto:info@fdagateway.com' target='_blank'>info@fdagateway.com</a> </span><br/><br/>" +
                                        "<span style='font-size:15px;'>Thanks,</span><br/><span>The FDA Health Studies Gateway Team</span>" +
                                        "<br/><span>----------------------------------------------------</span><br/>" +
                                        "<span style='font-size:10px;'>PS - This is an auto-generated email. Please do not reply.. In case you did not request for password help, please visit the app and change your password as a precautionary measure.</span>" +
                                        "</div>" +
                                        "</body>" +
                                        "</html>";
                                /*FdahpUserRegUtil.sendMessage("Password Help - FDA Health Studies Gateway App!",message,participantDetails.getEmail());
                                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());*/
                                boolean isMailSent = FdahpUserRegUtil.sendemail(participantDetails.getEmail(),"Password Help - FDA Health Studies Gateway App",message);
                                if (isMailSent){
                                    response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                                }else{
                                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(), FdahpUserRegUtil.ErrorCodes.FAILURE_TO_SENT_MAIL.getValue(), getViewContext().getResponse());
                                    return null;
                                }
                            }else{
                                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.EMAIL_NOT_VERIFIED.getValue(), FdahpUserRegUtil.ErrorCodes.EMAIL_NOT_VERIFIED.getValue(), getViewContext().getResponse());
                                return null;
                            }
                        }else{
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_103.getValue(),FdahpUserRegUtil.ErrorCodes.EMAIL_NOT_VERIFIED.getValue(), FdahpUserRegUtil.ErrorCodes.EMAIL_NOT_VERIFIED.getValue(), getViewContext().getResponse());
                            return null;
                        }


                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_CREDENTIALS.getValue(), FdahpUserRegUtil.ErrorCodes.INVALID_CREDENTIALS.getValue(), getViewContext().getResponse());
                        errors.rejectValue("emailId",ERROR_MSG,FdahpUserRegUtil.ErrorCodes.INVALID_CREDENTIALS.getValue());
                    }
                }else{
                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                    return null;
                }
            }catch (Exception e){
                 _log.error("ForgotPassword Action Error:",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }
    @RequiresNoPermission
    public class ResendConfirmationAction extends  ApiAction<LoginForm>{

        @Override
        protected ModelAndView handleGet() throws Exception
        {
            getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the POST method when calling this action.");
            return null;
        }

        @Override
        public ApiResponse execute(LoginForm loginForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            String auth = getViewContext().getRequest().getHeader("auth");
            boolean isAuthenticated = false;
            String code ="";
            try
            {
                if(loginForm != null && loginForm.getEmailId() != null && StringUtils.isNotEmpty(loginForm.getEmailId())){
                    UserDetails participantDetails = FdahpUserRegWSManager.get().getParticipantDetailsByEmail(loginForm.getEmailId());
                    if(participantDetails != null ){
                        if(participantDetails.getStatus() == 2){
                           // Date validateDate  = FdahpUserRegUtil.addHours(FdahpUserRegUtil.getCurrentDateTime(),48);
                            code = RandomStringUtils.randomAlphanumeric(6);
                            participantDetails.setSecurityToken(code);
                            participantDetails.setVerificationDate(FdahpUserRegUtil.getCurrentUtilDateTime());
                            FdahpUserRegWSManager.get().saveParticipant(participantDetails);
                            /*if(FdahpUserRegUtil.getCurrentUtilDateTime().before(validateDate) || FdahpUserRegUtil.getCurrentUtilDateTime().equals(validateDate)){
                                code = participantDetails.getSecurityToken();
                            }else{
                                code = RandomStringUtils.randomAlphanumeric(6);
                                participantDetails.setSecurityToken(code);
                                participantDetails.setVerificationDate(FdahpUserRegUtil.getCurrentUtilDateTime());
                                FdahpUserRegWSManager.get().saveParticipant(participantDetails);
                            }*/
                            String message = "<html>" +
                                    "<body>" +
                                    "<div style='margin:20px;padding:10px;font-family: sans-serif;font-size: 14px;'>" +
                                    "<span>Hi,</span><br/><br/>" +
                                    "<span>Thanks for registering with us! We look forward to having you on board and actively taking part in<br/>Health Studies being conducted by the FDA and its partners.</span><br/><br/>" +
                                    "<span>Your sign-up process is almost complete. Please use the Verification Code provided below to<br/>verify your email in the mobile app. </span><br/><br/>" +
                                    "<span><strong>Verification Code:</strong>" +participantDetails.getSecurityToken()+ "</span><br/><br/>" +
                                    "<span>This code can be used only once and is valid for a period of 48 hours only.</span><br/><br/>" +
                                    "<span>Please note that  registration (or sign up) for the app  is requested only to provide you with a <br/>seamless experience of using the app. Your registration information does not become part of <br/>the data collected for any study(ies) housed in the app. Each study has its own consent process <br/> and your data for the study will not be collected without you providing your informed consent prior<br/> to joining the study. </span><br/><br/>"+
                                    "<span>For any questions or assistance, please write to <a>"+configProp.get("support.email")+"</a> </span><br/><br/>" +
                                    "<span style='font-size:15px;'>Thanks,</span><br/><span>The FDA Health Studies Gateway Team</span>" +
                                    "<br/><span>----------------------------------------------------</span><br/>" +
                                    "<span style='font-size:10px;'>PS - This is an auto-generated email. Please do not reply.</span>" +
                                    "</div>" +
                                    "</body>" +
                                    "</html>";
                            /*FdahpUserRegUtil.sendMessage("Welcome to the FDA Health Studies Gateway!",message,participantDetails.getEmail());
                            response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());*/
                            boolean isMailSent = FdahpUserRegUtil.sendemail(participantDetails.getEmail(),"Welcome to the FDA Health Studies Gateway!",message);
                            if (isMailSent){
                                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                            }else{
                                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.FAILURE_TO_SENT_MAIL.getValue(), getViewContext().getResponse());
                                return null;
                            }
                        }else{
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_103.getValue(),FdahpUserRegUtil.ErrorCodes.USER_ALREADY_VERIFIED.getValue(), FdahpUserRegUtil.ErrorCodes.USER_ALREADY_VERIFIED.getValue(), getViewContext().getResponse());
                            return null;
                        }

                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.EMAIL_NOT_EXISTS.getValue(), FdahpUserRegUtil.ErrorCodes.EMAIL_NOT_EXISTS.getValue(), getViewContext().getResponse());
                        return null;
                    }
                }else{
                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                    return null;
                }

            }catch (Exception e){
                _log.error("ResendConfirmationAction Action Error:",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }
    /*public boolean sendemail(String email, String subject, String messageBody) throws Exception{

        boolean sentMail = false;
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
		    props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            Session session = Session.getInstance(props,
                    new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication("apps@boston-technology.com", "password789");
                        }
                    });
            Message message = new MimeMessage(session);
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject(subject);
            message.setContent(messageBody, "text/html");
            Transport.send(message);
            sentMail = true;
        } catch (MessagingException e) {
             _log.error("ERROR:  sendemail() - ",e);
            sentMail = false;
        } catch (Exception e) {
             _log.error("ERROR:  sendemail() - ",e);
        }

        return sentMail;
    }*/

    @RequiresNoPermission
    public class ChangePasswordAction extends ApiAction<ChangePasswordForm>{

        @Override
        protected ModelAndView handleGet() throws Exception
        {
            getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the POST method when calling this action.");
            return null;
        }

        @Override
        public ApiResponse execute(ChangePasswordForm form, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            String auth = getViewContext().getRequest().getHeader("auth");
            boolean isAuthenticated = false;
            List<PasswordHistory> passwordHistories = null;
            Boolean isValidPassword = true;
            try{
                if(auth != null && StringUtils.isNotEmpty(auth)){
                    isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(auth);
                    if(isAuthenticated){
                        String oldPassword = form.getCurrentPassword();
                        String newPassword = form.getNewPassword();
                        String userId = getViewContext().getRequest().getHeader("userId");
                        if((oldPassword != null && StringUtils.isNotEmpty(oldPassword)) && (newPassword != null && StringUtils.isNotEmpty(newPassword))){
                            UserDetails participantDetails = FdahpUserRegWSManager.get().getParticipantDetails(userId);
                            if(participantDetails != null ){
                                if((participantDetails.getPassword() != null && participantDetails.getPassword().equalsIgnoreCase(FdahpUserRegUtil.getEncryptedString(oldPassword))) || (participantDetails.getResetPassword() != null && participantDetails.getResetPassword().equalsIgnoreCase(FdahpUserRegUtil.getEncryptedString(oldPassword)))){
                                    if(!oldPassword.equalsIgnoreCase(newPassword)){
                                        passwordHistories = FdahpUserRegWSManager.get().getPasswordHistoryList(userId);
                                        if(passwordHistories != null && !passwordHistories.isEmpty()){
                                            for (PasswordHistory userPasswordHistory : passwordHistories) {
                                                if(FdahpUserRegUtil.getEncryptedString(newPassword).equalsIgnoreCase(userPasswordHistory.getPassword())){
                                                    isValidPassword = false;
                                                    break;
                                                }
                                            }
                                        }
                                        if(isValidPassword){
                                            participantDetails.setPassword(FdahpUserRegUtil.getEncryptedString(newPassword));
                                            if(participantDetails.getTempPassword())
                                                participantDetails.setTempPassword(false);
                                            participantDetails.setResetPassword(null);
                                            participantDetails.setTempPasswordDate(FdahpUserRegUtil.getCurrentUtilDateTime());
                                            UserDetails updParticipantDetails = FdahpUserRegWSManager.get().saveParticipant(participantDetails);
                                            if(updParticipantDetails != null && !participantDetails.getTempPassword()){
                                                String message = FdahpUserRegWSManager.get().savePasswordHistory(userId,FdahpUserRegUtil.getEncryptedString(newPassword));
                                                if(message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue()))
                                                    response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());

                                            }

                                        }else{
                                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.NEW_PASSWORD_NOT_SAME_LAST_PASSWORD.getValue(),FdahpUserRegUtil.ErrorCodes.NEW_PASSWORD_NOT_SAME_LAST_PASSWORD.getValue(), getViewContext().getResponse());
                                            errors.rejectValue("currentPassword",ERROR_MSG,FdahpUserRegUtil.ErrorCodes.NEW_PASSWORD_NOT_SAME_LAST_PASSWORD.getValue());
                                        }

                                    }else{
                                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.OLD_PASSWORD_AND_NEW_PASSWORD_NOT_SAME.getValue(), getViewContext().getResponse());
                                        errors.rejectValue("currentPassword",ERROR_MSG,FdahpUserRegUtil.ErrorCodes.OLD_PASSWORD_AND_NEW_PASSWORD_NOT_SAME.getValue());
                                    }

                                }else{
                                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.OLD_PASSWORD_NOT_EXISTS.getValue(), getViewContext().getResponse());
                                    errors.rejectValue("currentPassword",ERROR_MSG,FdahpUserRegUtil.ErrorCodes.OLD_PASSWORD_NOT_EXISTS.getValue());
                                }
                            } else{
                                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                                return null;
                            }

                        }else{
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                            return null;
                        }
                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                        return null;
                    }

                }else{
                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                    return null;
                }
            }catch (Exception e){
                 _log.error("ChangePassword Action Error",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }
    public static class ChangePasswordForm extends ReturnUrlForm{
        private String _currentPassword;
        private String _newPassword;
        private  String _userId;

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
    @RequiresNoPermission
    public class LogoutAction extends  ApiAction<UserForm>{

        @Override
        public Object execute(UserForm userForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            boolean isAuthenticated = false;
            String message = FdahpUserRegUtil.ErrorCodes.FAILURE.getValue();
            try
            {
                 if(isDelete()){
                    String auth = getViewContext().getRequest().getHeader("auth");
                    String userId = getViewContext().getRequest().getHeader("userId");

                    if(auth != null && StringUtils.isNotEmpty(auth)){
                        isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(auth);
                        if(isAuthenticated){
                            if(null != userId && StringUtils.isNotEmpty(userId)){
                                message = FdahpUserRegWSManager.get().signout(userId);
                                if(message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue())){
                                    response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                                }else{
                                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(), FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(), getViewContext().getResponse());
                                    return null;
                                }
                            }else{
                                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                                return null;
                            }
                        }else{
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                            return null;
                        }

                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                        return null;
                    }
                }else{
                    getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the DELETE method when calling this action.");
                    return null;
                }

            }catch (Exception e){
                 _log.error("Logout Action Error:",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }
    @Marshal(Marshaller.Jackson)
    @RequiresNoPermission
    public class UserProfileAction extends ApiAction<Object>{

        @Override
        public ApiResponse execute(Object object, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            boolean isAuthenticated = false;
            try{
                String auth = getViewContext().getRequest().getHeader("auth");
                String userId = getViewContext().getRequest().getHeader("userId");
                if(auth != null && StringUtils.isNotEmpty(auth)){
                    isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(auth);
                    if(isAuthenticated){
                        if(null != userId && StringUtils.isNotEmpty(userId)){
                            response = FdahpUserRegWSManager.get().getParticipantInfoDetails(userId);
                        }else{
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                            return null;
                        }
                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                        return null;
                    }
                }else{
                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                    return null;
                }
            }catch (Exception e){
                _log.error("User Profile Action",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }
    @Marshal(Marshaller.Jackson)
    @RequiresNoPermission
    public class UpdateUserProfileAction extends  ApiAction<ProfileForm>{

        @Override
        protected ModelAndView handleGet() throws Exception
        {
            getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the POST method when calling this action.");
            return null;
        }
        @Override
        public ApiResponse execute(ProfileForm profileForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            List<ParticipantStudies> addParticipantStudiesList = new ArrayList<ParticipantStudies>();
            String message = FdahpUserRegUtil.ErrorCodes.FAILURE.getValue();
            AuthInfo updaAuthInfo = null;
            try{
                String auth = getViewContext().getRequest().getHeader("auth");
                String userId = getViewContext().getRequest().getHeader("userId");
                boolean isAuthenticated = false;
                if(auth != null && StringUtils.isNotEmpty(auth))
                {
                    isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(auth);
                    if (isAuthenticated)
                    {
                        if (profileForm != null && userId != null && StringUtils.isNotEmpty(userId))
                        {
                            UserDetails participantDetails = FdahpUserRegWSManager.get().getParticipantDetails(userId);
                            if (participantDetails != null)
                            {
                                if (profileForm.getSettings() != null)
                                {
                                    if (profileForm.getSettings().getRemoteNotifications() != null)
                                        participantDetails.setRemoteNotificationFlag(profileForm.getSettings().getRemoteNotifications());
                                    if (profileForm.getSettings().getLocalNotifications() != null)
                                        participantDetails.setLocalNotificationFlag(profileForm.getSettings().getLocalNotifications());
                                    if (profileForm.getSettings().getPasscode() != null)
                                        participantDetails.setUsePassCode(profileForm.getSettings().getPasscode());
                                    if (profileForm.getSettings().getTouchId() != null)
                                        participantDetails.setTouchId(profileForm.getSettings().getTouchId());
                                    if(profileForm.getSettings().getReminderLeadTime() != null && StringUtils.isNotEmpty(profileForm.getSettings().getReminderLeadTime()))
                                        participantDetails.setReminderLeadTime(profileForm.getSettings().getReminderLeadTime());

                                    if(profileForm.getSettings().getLocale() != null && StringUtils.isNotEmpty(profileForm.getSettings().getLocale()))
                                        participantDetails.setLocale(profileForm.getSettings().getLocale());
                                }
                                if (profileForm.getInfo() != null)
                                {
                                    AuthInfo authInfo = FdahpUserRegWSManager.get().getAuthInfo(auth, userId);
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
                                        updaAuthInfo = FdahpUserRegWSManager.get().updateAuthInfo(authInfo);
                                    }
                                }
                                if (profileForm.getParticipantInfo() != null && profileForm.getParticipantInfo().size() > 0)
                                {
                                    List<ParticipantStudies> participantStudiesList = FdahpUserRegWSManager.get().getParticipantStudiesList(userId);
                                    for (int i = 0; i < profileForm.getParticipantInfo().size(); i++)
                                    {
                                        ParticipantInfoBean participantInfoBean = profileForm.getParticipantInfo().get(i);
                                        boolean isExists = false;
                                        if (participantStudiesList != null && participantStudiesList.size() > 0)
                                        {
                                            for (ParticipantStudies participantStudies : participantStudiesList)
                                            {
                                                if (participantInfoBean.getStudyId().equalsIgnoreCase(participantStudies.getStudyId()))
                                                {
                                                    isExists = true;
                                                    if (participantInfoBean.getParticipantId() != null && StringUtils.isNotEmpty(participantInfoBean.getParticipantId()))
                                                        participantStudies.setParticipantId(participantInfoBean.getParticipantId());
                                                    if(participantInfoBean.getEnrolledDate() != null && StringUtils.isNotEmpty(participantInfoBean.getEnrolledDate()))
                                                        participantStudies.setEnrolledDate(participantInfoBean.getEnrolledDate());
                                                     participantStudies.setStatus(FdahpUserRegUtil.ErrorCodes.JOINED.getValue());
                                                    addParticipantStudiesList.add(participantStudies);
                                                }
                                            }
                                        }
                                        if (!isExists)
                                        {
                                            ParticipantStudies participantStudies = new ParticipantStudies();
                                            if (participantInfoBean.getParticipantId() != null && StringUtils.isNotEmpty(participantInfoBean.getParticipantId()))
                                                participantStudies.setParticipantId(participantInfoBean.getParticipantId());
                                            if (participantInfoBean.getStudyId() != null && StringUtils.isNotEmpty(participantInfoBean.getStudyId()))
                                                participantStudies.setStudyId(participantInfoBean.getStudyId());
                                            if(participantInfoBean.getEnrolledDate() != null && StringUtils.isNotEmpty(participantInfoBean.getEnrolledDate()))
                                                participantStudies.setEnrolledDate(participantInfoBean.getEnrolledDate());
                                            participantStudies.setStatus(FdahpUserRegUtil.ErrorCodes.JOINED.getValue());
                                            participantStudies.setUserId(userId);
                                            addParticipantStudiesList.add(participantStudies);
                                        }
                                    }
                                    message = FdahpUserRegWSManager.get().saveParticipantStudies(addParticipantStudiesList);
                                }
                                UserDetails updateParticipantDetails = FdahpUserRegWSManager.get().saveParticipant(participantDetails);
                                if (updateParticipantDetails != null || message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue()) || updaAuthInfo != null)
                                {
                                    response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                                }
                            }else{
                                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                                return null;
                            }
                        }
                        else
                        {
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                            return null;
                        }
                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(), FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                        return null;
                    }
                }else
                    {
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                        return null;
                    }
            }catch (Exception e){
                 _log.error("UpdateUSerProfile Action Error :",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }

    public static class ProfileForm {


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
    @Marshal(Marshaller.Jackson)
    @RequiresNoPermission
    public class UpdatePreferencesAction extends  ApiAction<PreferencesForm>{

        @Override
        protected ModelAndView handleGet() throws Exception
        {
            getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the POST method when calling this action.");
            return null;
        }

        @Override
        public ApiResponse execute(PreferencesForm preferencesForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response  = new ApiSimpleResponse();
            List<ParticipantStudies> addParticipantStudiesList = new ArrayList<ParticipantStudies>();
            List<ParticipantActivities> participantActivitiesList = new ArrayList<ParticipantActivities>();
            try{
                String auth = getViewContext().getRequest().getHeader("auth");
                String userId = getViewContext().getRequest().getHeader("userId");
                boolean isAuthenticated = false;
                if(auth != null && StringUtils.isNotEmpty(auth)){
                    isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(auth);
                    if(isAuthenticated){
                        if(preferencesForm != null && userId != null && StringUtils.isNotEmpty(userId)){
                            if(preferencesForm.getStudies() != null && preferencesForm.getStudies().size() > 0){
                               List<StudiesBean> studiesBeenList = preferencesForm.getStudies();
                                List<ParticipantStudies> existParticipantStudies = FdahpUserRegWSManager.get().getParticipantStudiesList(userId);

                                for (int i=0;i < studiesBeenList.size() ; i++){
                                    StudiesBean studiesBean =  studiesBeenList.get(i);
                                    boolean isExists = false;
                                    if(existParticipantStudies != null && existParticipantStudies.size() >0){
                                         for (ParticipantStudies participantStudies : existParticipantStudies){

                                             if(studiesBean.getStudyId().equalsIgnoreCase(participantStudies.getStudyId())){
                                                 isExists = true;
                                                 if(studiesBean.getStatus() != null && StringUtils.isNotEmpty(studiesBean.getStatus()))
                                                     participantStudies.setStatus(studiesBean.getStatus());
                                                     participantStudies.setEnrolledDate(FdahpUserRegUtil.getCurrentDate());
                                                 if(studiesBean.getBookmarked() != null)
                                                     participantStudies.setBookmark(studiesBean.getBookmarked());
                                                 /*if(studiesBean.getEnrolledDate() != null)
                                                     participantStudies.setEnrolledDate(studiesBean.getEnrolledDate());*/
                                                 addParticipantStudiesList.add(participantStudies);
                                             }
                                         }
                                    }
                                    if(!isExists){
                                        ParticipantStudies participantStudies = new ParticipantStudies();
                                        //participantStudies.setParticipantId(Integer.valueOf(userId));
                                        if(studiesBean.getStudyId()!= null && StringUtils.isNotEmpty(studiesBean.getStudyId()))
                                            participantStudies.setStudyId(studiesBean.getStudyId());
                                        if(studiesBean.getStatus()!= null && StringUtils.isNotEmpty(studiesBean.getStatus())){
                                            participantStudies.setStatus(studiesBean.getStatus());
                                            participantStudies.setEnrolledDate(FdahpUserRegUtil.getCurrentDate());
                                        }else{
                                            participantStudies.setStatus(FdahpUserRegUtil.ErrorCodes.YET_TO_JOIN.getValue());
                                        }
                                        if(studiesBean.getBookmarked() != null)
                                            participantStudies.setBookmark(studiesBean.getBookmarked());
                                        if(userId != null && StringUtils.isNotEmpty(userId))
                                            participantStudies.setUserId(userId);
                                        if(studiesBean.getEnrolledDate() != null)
                                            participantStudies.setEnrolledDate(studiesBean.getEnrolledDate());
                                       addParticipantStudiesList.add(participantStudies);
                                    }
                                }
                               FdahpUserRegWSManager.get().saveParticipantStudies(addParticipantStudiesList);
                            }
                            /*if(preferencesForm.getActivities() != null && preferencesForm.getActivities().size() > 0){
                                List<ActivitiesBean> activitiesBeanList = preferencesForm.getActivities();
                                List<ParticipantActivities> existedParticipantActivitiesList = FdahpUserRegWSManager.get().getParticipantActivitiesList(userId);
                                for (int i=0;i < activitiesBeanList.size() ; i++){
                                    ActivitiesBean activitiesBean = activitiesBeanList.get(i);
                                    boolean isExists = false;
                                    if(existedParticipantActivitiesList != null && existedParticipantActivitiesList.size() > 0)
                                        for(ParticipantActivities participantActivities : existedParticipantActivitiesList){
                                            if(activitiesBean.getStudyId().equalsIgnoreCase(participantActivities.getStudyId()) && activitiesBean.getActivityId().equalsIgnoreCase(participantActivities.getActivityId())){
                                                isExists =true;
                                                if(userId != null && StringUtils.isNotEmpty(userId))
                                                    participantActivities.setParticipantId(userId);
                                                if(activitiesBean.getStatus() != null && StringUtils.isNotEmpty(activitiesBean.getStatus()))
                                                    participantActivities.setStatus(activitiesBean.getStatus());
                                                if(activitiesBean.getBookmarked() != null )
                                                    participantActivities.setBookmark(activitiesBean.getBookmarked());
                                                if(activitiesBean.getActivityVersion() != null && StringUtils.isNotEmpty(activitiesBean.getActivityVersion()))
                                                    participantActivities.setActivityVersion(activitiesBean.getActivityVersion());
                                                if(activitiesBean.getActivityRunId() != null && StringUtils.isNotEmpty(activitiesBean.getActivityRunId()))
                                                    participantActivities.setActivityRunId(activitiesBean.getActivityRunId());
                                                participantActivitiesList.add(participantActivities);
                                            }
                                        }
                                    if (!isExists){
                                        ParticipantActivities participantActivities = new ParticipantActivities();
                                        if(activitiesBean.getActivityId() != null && StringUtils.isNotEmpty(activitiesBean.getActivityId()))
                                            participantActivities.setActivityId(activitiesBean.getActivityId());
                                        if(activitiesBean.getStudyId() != null && StringUtils.isNotEmpty(activitiesBean.getStudyId()))
                                            participantActivities.setStudyId(activitiesBean.getStudyId());
                                        if(userId!= null && StringUtils.isNotEmpty(userId))
                                            participantActivities.setParticipantId(userId);
                                        if(activitiesBean.getStatus()!= null && StringUtils.isNotEmpty(activitiesBean.getStatus()))
                                            participantActivities.setStatus(activitiesBean.getStatus());
                                        else
                                            participantActivities.setStatus(FdahpUserRegUtil.ErrorCodes.YET_TO_JOIN.getValue());
                                        if(activitiesBean.getBookmarked() != null)
                                            participantActivities.setBookmark(activitiesBean.getBookmarked());
                                        if(activitiesBean.getActivityVersion() != null && StringUtils.isNotEmpty(activitiesBean.getActivityVersion()))
                                            participantActivities.setActivityVersion(activitiesBean.getActivityVersion());
                                        if(activitiesBean.getActivityRunId() != null && StringUtils.isNotEmpty(activitiesBean.getActivityRunId()))
                                            participantActivities.setActivityRunId(activitiesBean.getActivityRunId());
                                       participantActivitiesList.add(participantActivities);
                                    }
                                }
                                FdahpUserRegWSManager.get().saveParticipantActivities(participantActivitiesList);
                            }*/
                            response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                        }else{
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                            return null;
                        }
                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                        return null;
                    }
                }else{
                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                    return null;
                }
            }catch (Exception e){
                 _log.error("UpdatePreferences Action Error :",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }
    public static class PreferencesForm {


        public List<StudiesBean>  _studies;
        public ActivitiesBean  _activity;
        public String _studyId;

        public List<StudiesBean> getStudies()
        {
            return _studies;
        }

        public void setStudies(List<StudiesBean> studies)
        {
            _studies = studies;
        }

        public ActivitiesBean getActivity()
        {
            return _activity;
        }

        public void setActivity(ActivitiesBean activity)
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



    public static class LoginForm{

        public  String _emailId;
        public  String  _password;

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

    @Marshal(Marshaller.Jackson)
    @RequiresNoPermission
    public class UserPreferencesAction extends  ApiAction<UserForm>{

        @Override
        public ApiResponse execute(UserForm userForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            boolean isAuthenticated = false;
            try{
                String auth = getViewContext().getRequest().getHeader("auth");
                String userId =  getViewContext().getRequest().getHeader("userId");
                if(auth != null && StringUtils.isNotEmpty(auth)){
                    isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(auth);
                    if(isAuthenticated){
                        if(userId != null && StringUtils.isNotEmpty(userId)){
                            response = FdahpUserRegWSManager.get().getPreferences(userId);
                        }else{
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                            return null;
                        }
                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                        return null;
                    }
                }else{
                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                    return null;
                }
            }catch (Exception e){
                 _log.error("UserPreferencesAction Action Error",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                return null;

            }
            return response;
        }
    }


    public static class ConsentStatusForm{

        private  String _studyId;
        private  Boolean _eligibility;
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
    @Marshal(Marshaller.Jackson)
    @RequiresNoPermission
   public class UpdateEligibilityConsentStatusAction extends  ApiAction<ConsentStatusForm>{

       @Override
       protected ModelAndView handleGet() throws Exception
       {
           getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the POST method when calling this action.");
           return null;
       }

       @Override
       public ApiResponse execute(ConsentStatusForm consentStatusForm, BindException errors) throws Exception
       {
           ApiSimpleResponse response = new ApiSimpleResponse();
           boolean isAuthenticated = false;
           StudyConsent updateConsent = null;
           try{
               String auth = getViewContext().getRequest().getHeader("auth");
               String userId =  getViewContext().getRequest().getHeader("userId");
               if(auth != null && StringUtils.isNotEmpty(auth)){
                   isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(auth);
                   if(isAuthenticated){
                       if(consentStatusForm != null && consentStatusForm.getConsent() != null){
                            if(consentStatusForm.getStudyId() != null && StringUtils.isNotEmpty(consentStatusForm.getStudyId()) && userId != null && StringUtils.isNotEmpty(userId)){
                                ParticipantStudies participantStudies = FdahpUserRegWSManager.get().getParticipantStudies(consentStatusForm.getStudyId(),userId);
                                if(participantStudies != null){
                                    if(consentStatusForm.getEligibility() != null){
                                        participantStudies.setEligbibility(consentStatusForm.getEligibility());
                                    }
                                    if(consentStatusForm.getSharing() != null && StringUtils.isNotEmpty(consentStatusForm.getSharing())){
                                        participantStudies.setSharing(consentStatusForm.getSharing());
                                    }
                                    /*if(consentStatusForm.getConsentStatus() != null)
                                        participantStudies.setConsentStatus(consentStatusForm.getConsentStatus());
                                    if(consentStatusForm.getConsent()!= null && StringUtils.isNotEmpty(consentStatusForm.getConsent()))
                                        participantStudies.setConsent(consentStatusForm.getConsent());*/

                                    List<ParticipantStudies> participantStudiesList = new ArrayList<ParticipantStudies>();
                                    participantStudiesList.add(participantStudies);
                                    String message = FdahpUserRegWSManager.get().saveParticipantStudies(participantStudiesList);
                                    if(consentStatusForm.getConsent() != null){
                                        StudyConsent consent = null;
                                        if(consentStatusForm.getConsent().getVersion() != null && StringUtils.isNotEmpty(consentStatusForm.getConsent().getVersion())){
                                            consent = FdahpUserRegWSManager.get().getStudyConsent(userId,consentStatusForm.getStudyId(),consentStatusForm.getConsent().getVersion());
                                            if(consent != null){
                                                if(consentStatusForm.getConsent().getVersion() != null && StringUtils.isNoneEmpty(consentStatusForm.getConsent().getVersion()))
                                                    consent.setVersion(consentStatusForm.getConsent().getVersion());
                                                if(consentStatusForm.getConsent().getStatus() != null && StringUtils.isNoneEmpty(consentStatusForm.getConsent().getStatus()))
                                                    consent.setStatus(consentStatusForm.getConsent().getStatus());
                                                if(consentStatusForm.getConsent().getPdf() != null && StringUtils.isNoneEmpty(consentStatusForm.getConsent().getPdf()))
                                                    consent.setPdf(consentStatusForm.getConsent().getPdf());
                                                consent.setUserId(userId);
                                                consent.setStudyId(consentStatusForm.getStudyId());
                                            }else{
                                                consent = new StudyConsent();
                                                consent.setUserId(userId);
                                                consent.setStudyId(consentStatusForm.getStudyId());
                                                consent.setStatus(consentStatusForm.getConsent().getStatus());
                                                consent.setVersion(consentStatusForm.getConsent().getVersion());
                                                consent.setPdf(consentStatusForm.getConsent().getPdf());
                                            }
                                            updateConsent = FdahpUserRegWSManager.get().saveStudyConsent(consent);
                                            if(updateConsent != null && message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue())){
                                                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),message);
                                            }else{
                                                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.FAILURE.getValue());
                                            }
                                        }else{
                                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.CONSENT_VERSION_REQUIRED.getValue(),FdahpUserRegUtil.ErrorCodes.CONSENT_VERSION_REQUIRED.getValue(), getViewContext().getResponse());
                                            return null;
                                        }

                                    }

                                }else{
                                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.NO_DATA_AVAILABLE.getValue(),FdahpUserRegUtil.ErrorCodes.NO_DATA_AVAILABLE.getValue(), getViewContext().getResponse());
                                    //errors.rejectValue("studyId",ERROR_MSG,"No Data available with the studyId");
                                    return null;
                                }
                            }else{
                                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                                return null;
                           }
                       }else{
                           FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                           return null;
                       }
                   }else{
                       FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                       return null;
                   }
               }else{
                   FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                   return null;
               }
           }catch (Exception e){
                _log.error("UpdateEligibilityConsentStatusAction Action Error",e);
               FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
               return null;
           }
           return response;
       }
   }
   public static class ActivityForm {

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

   @RequiresNoPermission
    public class ActivityStateAction extends ApiAction<ActivityForm>
   {

       @Override
       public ApiResponse execute(ActivityForm activityForm, BindException errors) throws Exception
       {
           ApiSimpleResponse response  = new ApiSimpleResponse();
           boolean isAuthenticated = false;
           try{
               String userId = getViewContext().getRequest().getHeader("userId");
               String auth = getViewContext().getRequest().getHeader("auth");
               if(auth != null && StringUtils.isNotEmpty(auth)){
                   isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(auth);
                   if(isAuthenticated){
                       String studyId = getViewContext().getRequest().getHeader("studyId");
                       if(studyId != null && StringUtils.isNotEmpty(studyId) && userId != null && StringUtils.isNotEmpty(userId)){
                           List<ParticipantActivities> participantActivitiesList = FdahpUserRegWSManager.get().getParticipantActivitiesList(studyId,userId);
                           JSONArray jsonArray = new JSONArray();
                           if(participantActivitiesList !=null && participantActivitiesList.size() >0)
                           {
                               for (ParticipantActivities participantActivities : participantActivitiesList){
                                   JSONObject jsonObject = new JSONObject();
                                   if (participantActivities.getActivityId() != null)
                                       jsonObject.put("activityId",participantActivities.getActivityId());
                                   if(participantActivities.getActivityVersion() != null)
                                       jsonObject.put("activityVersion",participantActivities.getActivityVersion());
                                   if(participantActivities.getActivityState() != null)
                                       jsonObject.put("activityState",participantActivities.getActivityState());
                                   if(participantActivities.getActivityRunId() != null)
                                       jsonObject.put("activityRunId",participantActivities.getActivityRunId());
                                   if(participantActivities.getBookmark() != null)
                                       jsonObject.put("bookmarked",participantActivities.getBookmark());
                                   JSONObject runObject = new JSONObject();
                                   if(participantActivities.getTotal() != null)
                                       runObject.put("total",participantActivities.getTotal());
                                   if(participantActivities.getCompleted() != null)
                                       runObject.put("completed",participantActivities.getCompleted());
                                   if(participantActivities.getMissed() != null)
                                       runObject.put("missed",participantActivities.getMissed());
                                   jsonObject.put("activityRun",runObject);
                                   jsonArray.put(jsonObject);
                               }
                           }
                           response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                           response.put(FdahpUserRegUtil.ErrorCodes.ACTIVITIES.getValue(),jsonArray);
                       }else{
                           FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                           return null;
                       }
                   }else{
                       FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                       return null;
                   }
               }else{
                   FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                   return null;
               }
           }catch (Exception e){
                _log.error("ActivityStateAction Action Error",e);
               FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
               return null;

           }
           return response;
       }
   }

   public static class ActivityStateForm {


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



   @Marshal(Marshaller.Jackson)
   @RequiresNoPermission
    public class UpdateActivityStateAction extends  ApiAction<PreferencesForm>{

       @Override
       protected ModelAndView handleGet() throws Exception
       {
           getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the POST method when calling this action.");
           return null;
       }

       @Override
       public ApiResponse execute(PreferencesForm preferencesForm, BindException errors) throws Exception
       {
           ApiSimpleResponse response =  new ApiSimpleResponse();
           boolean isAuthenticated = false;
           try{
               String auth = getViewContext().getRequest().getHeader("auth");
               String userId = getViewContext().getRequest().getHeader("userId");
               List<ParticipantActivities> addParticipantActivitiesList = new ArrayList<ParticipantActivities>();
               if(auth != null && StringUtils.isNotEmpty(auth)){
                   isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(auth);
                   if(isAuthenticated){
                       if(preferencesForm != null && userId != null && StringUtils.isNotEmpty(userId)){
                           if((preferencesForm.getStudyId() != null && StringUtils.isNotEmpty(preferencesForm.getStudyId())) &&
                                   (preferencesForm.getActivity() != null && preferencesForm.getActivity().getActivityId() != null && StringUtils.isNotEmpty(preferencesForm.getActivity().getActivityId()))){
                               List<ParticipantActivities> participantActivitiesList;
                               participantActivitiesList = FdahpUserRegWSManager.get().getParticipantActivitiesList(preferencesForm.getStudyId(),userId);
                               boolean isExist = false;
                               if(participantActivitiesList != null && participantActivitiesList.size()>0){
                                   for (ParticipantActivities participantActivities : participantActivitiesList)
                                   {
                                       if (participantActivities.getActivityId().equalsIgnoreCase(preferencesForm.getActivity().getActivityId()))
                                       {
                                           isExist = true;
                                           if(preferencesForm.getActivity().getActivityVersion()!=null && StringUtils.isNotEmpty(preferencesForm.getActivity().getActivityVersion()))
                                               participantActivities.setActivityVersion(preferencesForm.getActivity().getActivityVersion());
                                           if(preferencesForm.getActivity().getActivityState()!= null && StringUtils.isNotEmpty(preferencesForm.getActivity().getActivityState()))
                                               participantActivities.setActivityState(preferencesForm.getActivity().getActivityState());
                                           if(preferencesForm.getActivity().getActivityRunId() != null && StringUtils.isNotEmpty(preferencesForm.getActivity().getActivityRunId()))
                                               participantActivities.setActivityRunId(preferencesForm.getActivity().getActivityRunId());
                                           if(preferencesForm.getActivity().getBookmarked() != null)
                                               participantActivities.setBookmark(preferencesForm.getActivity().getBookmarked());
                                           if(preferencesForm.getActivity().getActivityRun() != null){
                                               if(preferencesForm.getActivity().getActivityRun().getTotal() != null)
                                                   participantActivities.setTotal(preferencesForm.getActivity().getActivityRun().getTotal());
                                               if(preferencesForm.getActivity().getActivityRun().getCompleted() != null)
                                                   participantActivities.setCompleted(preferencesForm.getActivity().getActivityRun().getCompleted());
                                               if(preferencesForm.getActivity().getActivityRun().getMissed() != null)
                                                   participantActivities.setMissed(preferencesForm.getActivity().getActivityRun().getMissed());
                                           }
                                           addParticipantActivitiesList.add(participantActivities);
                                       }
                                   }

                               }
                               if(!isExist){
                                   ParticipantActivities addParticipantActivities = new ParticipantActivities();
                                   if(preferencesForm.getActivity().getActivityState()!= null && StringUtils.isNotEmpty(preferencesForm.getActivity().getActivityState()))
                                       addParticipantActivities.setActivityState(preferencesForm.getActivity().getActivityState());
                                   if(preferencesForm.getActivity().getActivityVersion()!=null && StringUtils.isNotEmpty(preferencesForm.getActivity().getActivityVersion()))
                                       addParticipantActivities.setActivityVersion(preferencesForm.getActivity().getActivityVersion());
                                   if(preferencesForm.getActivity().getActivityId() != null && StringUtils.isNotEmpty(preferencesForm.getActivity().getActivityId()))
                                       addParticipantActivities.setActivityId(preferencesForm.getActivity().getActivityId());
                                   if(preferencesForm.getActivity().getActivityRunId() != null && StringUtils.isNotEmpty(preferencesForm.getActivity().getActivityRunId()))
                                       addParticipantActivities.setActivityRunId(preferencesForm.getActivity().getActivityRunId());
                                   if(preferencesForm.getStudyId()!=null && StringUtils.isNotEmpty(preferencesForm.getStudyId()))
                                       addParticipantActivities.setStudyId(preferencesForm.getStudyId());
                                   if(userId!= null && StringUtils.isNotEmpty(userId))
                                       addParticipantActivities.setParticipantId(userId);
                                   if(preferencesForm.getActivity().getBookmarked() != null)
                                       addParticipantActivities.setBookmark(preferencesForm.getActivity().getBookmarked());
                                   if(preferencesForm.getActivity().getActivityRun() != null){
                                       if(preferencesForm.getActivity().getActivityRun().getTotal() != null)
                                           addParticipantActivities.setTotal(preferencesForm.getActivity().getActivityRun().getTotal());
                                       if(preferencesForm.getActivity().getActivityRun().getCompleted() != null)
                                           addParticipantActivities.setCompleted(preferencesForm.getActivity().getActivityRun().getCompleted());
                                       if(preferencesForm.getActivity().getActivityRun().getMissed() != null)
                                           addParticipantActivities.setMissed(preferencesForm.getActivity().getActivityRun().getMissed());
                                   }
                                   addParticipantActivitiesList.add(addParticipantActivities);
                               }
                               String message = FdahpUserRegWSManager.get().saveParticipantActivities(addParticipantActivitiesList);
                               if (message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue())){
                                   response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                               }else{
                                   FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(), getViewContext().getResponse());
                                   return null;
                               }
                           }else{
                               FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                               return null;
                           }
                       }else{
                           FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                           return null;
                       }
                   }else{
                       FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                       return null;
                   }
               }else{
                   FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                   return null;
               }
           }catch (Exception e){
               _log.error("UpdateActivityStateAction Action Error",e);
               FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
               return null;
           }
          return response;
       }
   }
   public static class WithDrawForm {

       public String  _studyId;

       public Boolean  _deleteData;

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
   @RequiresNoPermission
    public class WithdrawAction extends  ApiAction<WithDrawForm>{

       @Override
       public ApiResponse execute(WithDrawForm withDrawForm, BindException errors) throws Exception
       {
           ApiSimpleResponse response = new ApiSimpleResponse();
           boolean isAuthenticated = false;
           String message = FdahpUserRegUtil.ErrorCodes.FAILURE.getValue();
           try
           {
               if(isDelete()){
                   String auth = getViewContext().getRequest().getHeader("auth");
                   String userId = getViewContext().getRequest().getHeader("userId");
                   if(auth != null && StringUtils.isNotEmpty(auth)){
                       isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(auth);
                       if(isAuthenticated){
                            if(withDrawForm != null && null != withDrawForm.getStudyId() && StringUtils.isNotEmpty(withDrawForm.getStudyId()) && null != userId &&  StringUtils.isNotEmpty(userId)){
                               ParticipantStudies participantStudies = FdahpUserRegWSManager.get().getParticipantStudies(withDrawForm.getStudyId(),userId);
                               //message = FdahpUserRegWSManager.get().withDrawStudy(Integer.valueOf(studyId),Integer.valueOf(userId));
                               if(participantStudies != null){
                                    if(participantStudies.getStatus().equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.WITHDRAWN.getValue())){
                                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.NO_DATA_AVAILABLE.getValue(),FdahpUserRegUtil.ErrorCodes.WITHDRAWN_STUDY.getValue(), getViewContext().getResponse());
                                        response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.WITHDRAWN_STUDY.getValue());
                                    }else{
                                        message = FdahpUserRegWSManager.get().withDrawStudy(withDrawForm.getStudyId(),userId,withDrawForm.getDeleteData());
                                        if(message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue())){
                                            response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                                        }else{
                                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(), getViewContext().getResponse());
                                            return null;
                                        }
                                    }
                               }else{
                                   FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(), getViewContext().getResponse());
                                   return null;
                               }
                               /*if(message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue())){
                                   response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                               }else{
                                   FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                                   getViewContext().getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue());
                                   return null;
                               }*/
                           }else{
                               FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                               return null;
                           }

                       }else{
                           FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                           return null;
                       }

                   }else{
                       FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                       return null;
                   }
               }else{
                   getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the DELETE method when calling this action.");
                   return null;
               }
           }catch (Exception e){
               _log.error("Withdraw Action Error:",e);
               FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
               return null;
           }
           return response;
       }
   }

   @RequiresNoPermission
    public class ConsentPDFAction extends ApiAction<ActivityForm>{

       @Override
       public ApiResponse execute(ActivityForm activityForm, BindException errors) throws Exception
       {
           ApiSimpleResponse response = new ApiSimpleResponse();
           boolean isAuthenticated = false;
           try{
               String auth = getViewContext().getRequest().getHeader("auth");
               String studyId = getViewContext().getRequest().getHeader("studyId");
               String userId = getViewContext().getRequest().getHeader("userId");
               String consentVersion = getViewContext().getRequest().getHeader("consentVersion");

               if(auth != null && StringUtils.isNotEmpty(auth)){
                   isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(auth);
                   if(isAuthenticated){
                       if(studyId != null && StringUtils.isNoneBlank(studyId) && userId != null && StringUtils.isNotEmpty(userId)){
                           // ParticipantStudies participantStudies = FdahpUserRegWSManager.get().getParticipantStudies(Integer.valueOf(studyId),Integer.valueOf(userId));
                           StudyConsent studyConsent = FdahpUserRegWSManager.get().getStudyConsent(userId,studyId,consentVersion);
                           if(studyConsent != null){
                               JSONObject jsonObject  = new JSONObject();
                               if(studyConsent.getVersion() != null)
                                   jsonObject.put("version",studyConsent.getVersion());
                               if(studyConsent.getPdf() != null)
                                   jsonObject.put("content",studyConsent.getPdf());
                               jsonObject.put("type", "application/pdf");
                               response.put("consent",jsonObject);
                               ParticipantStudies participantStudies = FdahpUserRegWSManager.get().getParticipantStudies(studyId,userId);
                               if(participantStudies != null){
                                   response.put("sharing",participantStudies.getSharing());
                               }
                               response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                            }else{
                                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.NO_DATA_AVAILABLE.getValue(),FdahpUserRegUtil.ErrorCodes.NO_DATA_AVAILABLE.getValue(), getViewContext().getResponse());
                                return  null;
                            }
                       }else{
                           FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                           return null;
                       }
                   }else{
                       FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                       return null;
                   }
               }else{
                   FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                   return null;
               }
           }catch (Exception e){
               _log.error("ConsentPDFAction Action Error",e);
               FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
               return null;
           }
           return response;
       }
   }

   /*@RequiresNoPermission
    public class CreatePasswordAction extends ApiAction<ChangePasswordForm>{

       *//*@Override
       protected ModelAndView handleGet() throws Exception
       {
           getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the POST method when calling this action.");
           return null;
       }*//*

       @Override
       public ApiResponse execute(ChangePasswordForm changePasswordForm, BindException errors) throws Exception
       {
           ApiSimpleResponse response = new ApiSimpleResponse();
           try{
               String token = getViewContext().getRequest().getParameter("token");
               if(StringUtils.isNoneBlank(token)){
                    UserDetails participantDetails = FdahpUserRegWSManager.get().getParticipantDetailsByToken(token);
                    if(participantDetails != null){
                        String newPassword = changePasswordForm.getNewPassword();
                        if(StringUtils.isNotBlank(newPassword)){
                            participantDetails.setPassword(FdahpUserRegUtil.getEncryptedString(newPassword));
                            participantDetails.setSecurityToken(null);
                            UserDetails updatParticipantDetails = FdahpUserRegWSManager.get().saveParticipant(participantDetails);
                            if(updatParticipantDetails != null){
                                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                            }else{
                                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(), getViewContext().getResponse());
                                return null;
                            }
                        }else{
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                            return null;
                        }
                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                        errors.rejectValue("token",ERROR_MSG,"token expires");
                    }
               }else{
                   FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                   return null;
               }
           }catch (Exception e){
               _log.error("Controller Create password action ",e);
               FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
               return null;
           }
           return response;
       }
   }*/

   @RequiresNoPermission
    public class DeleteAccountAction extends ApiAction{

       @Override
       public Object execute(Object o, BindException errors) throws Exception
       {
           ApiSimpleResponse response = new ApiSimpleResponse();
           boolean isAuthenticated = false;
           try{
               String userId = getViewContext().getRequest().getHeader("userId");
               String auth = getViewContext().getRequest().getHeader("auth");
               if((userId != null && StringUtils.isNotEmpty(userId)) && (auth != null && StringUtils.isNotEmpty(auth))){
                   isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(auth);
                   if(isAuthenticated){
                        String message = FdahpUserRegWSManager.get().deleteAccount(userId);
                        if(message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue())){
                            response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                        }else{
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(), getViewContext().getResponse());
                            return null;
                        }
                   }else{
                       FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                       return null;
                   }
               }else{
                   FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                   return null;
               }
           }catch (Exception e){
               _log.error("Delete Account Action:",e);
               FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
               return null;
           }
           return response;
       }
   }
   public static class DeactivateForm {

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
    @Marshal(Marshaller.Jackson)
    @RequiresNoPermission
    public class DeactivateAction extends ApiAction<DeactivateForm>{

        @Override
        public Object execute(DeactivateForm deactivateForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            boolean isAuthenticated = false;
            String message = FdahpUserRegUtil.ErrorCodes.FAILURE.getValue();
            try
            {
                if(isDelete()){
                    String auth = getViewContext().getRequest().getHeader("auth");
                    String userId = getViewContext().getRequest().getHeader("userId");

                    if(auth != null && StringUtils.isNotEmpty(auth)){
                        isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(auth);
                        if(isAuthenticated){
                            if(null != userId && StringUtils.isNotEmpty(userId)){
                                message = FdahpUserRegWSManager.get().deActivate(userId,deactivateForm);
                                if(message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue())){
                                    response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                                }else{
                                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.FAILURE.getValue(), getViewContext().getResponse());
                                    return null;
                                }
                            }else{
                                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                                return null;
                            }
                        }else{
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                            return null;
                        }

                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                        return null;
                    }
                }else{
                    getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the DELETE method when calling this action.");
                    return null;
                }

            }catch (Exception e){
                _log.error("Logout Action Error:",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }
    @Marshal(Marshaller.Jackson)
    @RequiresNoPermission
    public class UpdateStudyStateAction extends  ApiAction<PreferencesForm>{

        @Override
        protected ModelAndView handleGet() throws Exception
        {
            getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the POST method when calling this action.");
            return null;
        }

        @Override
        public ApiResponse execute(PreferencesForm preferencesForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response  = new ApiSimpleResponse();
            List<ParticipantStudies> addParticipantStudiesList = new ArrayList<ParticipantStudies>();
            try{
                String auth = getViewContext().getRequest().getHeader("auth");
                String userId = getViewContext().getRequest().getHeader("userId");
                boolean isAuthenticated = false;
                if(auth != null && StringUtils.isNotEmpty(auth)){
                    isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(auth);
                    if(isAuthenticated){
                        if(preferencesForm != null && userId != null && StringUtils.isNotEmpty(userId)){
                            if(preferencesForm.getStudies() != null && preferencesForm.getStudies().size() > 0)
                            {
                                List<StudiesBean> studiesBeenList = preferencesForm.getStudies();
                                List<ParticipantStudies> existParticipantStudies = FdahpUserRegWSManager.get().getParticipantStudiesList(userId);

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
                                                participantStudies.setEnrolledDate(FdahpUserRegUtil.getCurrentDate());
                                                if (studiesBean.getBookmarked() != null)
                                                    participantStudies.setBookmark(studiesBean.getBookmarked());
                                                if (studiesBean.getCompletion() != null)
                                                    participantStudies.setCompletion(studiesBean.getCompletion());
                                                if (studiesBean.getAdherence() != null)
                                                    participantStudies.setAdherence(studiesBean.getAdherence());
                                                addParticipantStudiesList.add(participantStudies);
                                            }
                                        }
                                    }
                                    if (!isExists)
                                    {
                                        ParticipantStudies participantStudies = new ParticipantStudies();
                                        //participantStudies.setParticipantId(Integer.valueOf(userId));
                                        if (studiesBean.getStudyId() != null && StringUtils.isNotEmpty(studiesBean.getStudyId()))
                                            participantStudies.setStudyId(studiesBean.getStudyId());
                                        if (studiesBean.getStatus() != null && StringUtils.isNotEmpty(studiesBean.getStatus()))
                                        {
                                            participantStudies.setStatus(studiesBean.getStatus());
                                            participantStudies.setEnrolledDate(FdahpUserRegUtil.getCurrentDate());
                                        }
                                        else
                                        {
                                            participantStudies.setStatus(FdahpUserRegUtil.ErrorCodes.YET_TO_JOIN.getValue());
                                        }
                                        if (studiesBean.getBookmarked() != null)
                                            participantStudies.setBookmark(studiesBean.getBookmarked());
                                        if (userId != null && StringUtils.isNotEmpty(userId))
                                            participantStudies.setUserId(userId);
                                       /* if(studiesBean.getEnrolledDate() != null)
                                            participantStudies.setEnrolledDate(studiesBean.getEnrolledDate());*/
                                        if (studiesBean.getCompletion() != null)
                                            participantStudies.setCompletion(studiesBean.getCompletion());
                                        if (studiesBean.getAdherence() != null)
                                            participantStudies.setAdherence(studiesBean.getAdherence());
                                        addParticipantStudiesList.add(participantStudies);
                                    }
                                }
                                FdahpUserRegWSManager.get().saveParticipantStudies(addParticipantStudiesList);
                            }
                            response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                        }else{
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                            return null;
                        }
                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                        return null;
                    }
                }else{
                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                    return null;
                }
            }catch (Exception e){
                _log.error("UpdateStudyState Action Error :",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }

    @Marshal(Marshaller.Jackson)
    @RequiresNoPermission
    public class StudyStateAction extends  ApiAction<UserForm>{

        @Override
        public ApiResponse execute(UserForm userForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            boolean isAuthenticated = false;
            try{
                String auth = getViewContext().getRequest().getHeader("auth");
                String userId =  getViewContext().getRequest().getHeader("userId");
                if(auth != null && StringUtils.isNotEmpty(auth)){
                    isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(auth);
                    if(isAuthenticated){
                        if(userId != null && StringUtils.isNotEmpty(userId)){
                            response = FdahpUserRegWSManager.get().getPreferences(userId);
                        }else{
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                            return null;
                        }
                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                        return null;
                    }
                }else{
                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                    return null;
                }
            }catch (Exception e){
                _log.error("StudyStateAction Action Error",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                return null;

            }
            return response;
        }
    }
}