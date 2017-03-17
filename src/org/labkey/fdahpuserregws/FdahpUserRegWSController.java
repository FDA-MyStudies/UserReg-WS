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
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FdahpUserRegWSController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(FdahpUserRegWSController.class);
    public static final String NAME = "fdahpuserregws";

    public FdahpUserRegWSController()
    {
        setActionResolver(_actionResolver);
    }

    private static final Logger _log = Logger.getLogger(FdahpUserRegWSController.class);

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
            apiSimpleResponse.put("reponse", "FdahpUserResWebServices Works!");
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
                            String message = "<html> <body>" +
                                    "Dear "+addParticipantDetails.getFirstName()+" "+addParticipantDetails.getLastName()+",<BR><p> Please click the below link to confirm the Account</p>" +
                                    "<p><a href='http://192.168.0.6:8081/labkey/fdahpUserRegWS/home/verify.api?token="+addParticipantDetails.getSecurityToken()+"'>Confirm Account</a></p>"+
                                    " <BR>Thanks,<BR>"+
                                    "<BR>"+"</body></html>";
                            boolean isMailSent = FdahpUserRegUtil.sendemail(addParticipantDetails.getEmail(),"Verification Email",message);
                            if (isMailSent){
                                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                            }else{
                                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.FAILURE_TO_SENT_MAIL.getValue(), getViewContext().getResponse());
                                return null;
                            }
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

    @RequiresNoPermission
    public class VerifyAction extends ApiAction<UserForm>{

        @Override
        public Object execute(UserForm userForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            try{
                String securityToken = getViewContext().getRequest().getParameter("token");
                if((securityToken != null && StringUtils.isNotEmpty(securityToken))){
                    UserDetails participantDetails = participantDetails = FdahpUserRegWSManager.get().getParticipantDetailsByToken(securityToken);
                    if(null != participantDetails){
                        participantDetails.setStatus(1);
                        participantDetails.setSecurityToken(null);
                        UserDetails updateParticipantDetails = FdahpUserRegWSManager.get().saveParticipant(participantDetails);
                        if(null != updateParticipantDetails){
                            response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                            //response.put("userId", updateParticipantDetails.getId());
                            if(updateParticipantDetails.getStatus() != null)
                                if(updateParticipantDetails.getStatus() == 2)
                                    response.put("verified", false);
                                if(updateParticipantDetails.getStatus() == 1)
                                    response.put("verified", true);
                        }
                    }else{
                       /* FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                        return null;*/
                        response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(), FdahpUserRegUtil.ErrorCodes.FAILURE.getValue().toLowerCase());
                        response.put("status",FdahpUserRegUtil.ErrorCodes.USER_ALREADY_VERIFIED.getValue());
                       // FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                       // errors.rejectValue("token",ERROR_MSG,"token expires");
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
            ParticipantForm participantForm = null;
            try{
                if(loginForm != null){
                    String email = getViewContext().getRequest().getHeader("emailId");
                    String password = getViewContext().getRequest().getHeader("password");
                    if((loginForm.getEmailId() != null && StringUtils.isNotEmpty(loginForm.getEmailId())) && (loginForm.getPassword() != null && StringUtils.isNotEmpty(loginForm.getPassword()))){
                        participantForm= FdahpUserRegWSManager.get().signingParticipant(loginForm.getEmailId(),loginForm.getPassword());
                        //  System.out.println("participantForm:"+participantForm);
                        if(null!=participantForm){
                            response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                            System.out.println(participantForm.getUserId());
                            response.put("userId",participantForm.getUserId());
                            response.put("auth",participantForm.getAuth());
                            if(participantForm.getStatus() == 2)
                            {
                                response.put("verified", false);
                            }
                            if(participantForm.getStatus() == 1){
                                response.put("verified", true);
                            }
                            if(participantForm.getTempPassword()){
                                response.put("resetPassword", participantForm.getTempPassword());
                            }
                        }else{
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_USERNAME_PASSWORD_MSG.name(), FdahpUserRegUtil.ErrorCodes.INVALID_USERNAME_PASSWORD_MSG.getValue(), getViewContext().getResponse());
                            errors.rejectValue("emailId",ERROR_MSG,  "emailId is wrong.");
                            errors.rejectValue("password",ERROR_MSG,  "password is wrong.");
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
            String token = UUID.randomUUID().toString();
            participantDetails.setSecurityToken(token);
            String userId = UUID.randomUUID().toString();
            System.out.println("token:"+token);
            System.out.println("userId:"+userId);
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
                       /* String password =  RandomStringUtils.randomAlphanumeric(6);
                        participantDetails.setPassword(FdahpUserRegUtil.getEncryptedString(password));
                        String messageBody = "<html> <body>" +
                                "Dear "+participantDetails.getFirstName()+" "+participantDetails.getLastName()+",<BR><p> Your new password is '<b>" + password + "</b>'</p>" +
                                " <BR>Thanks,<BR>BTC Soft"+
                                "<BR>"+"</body></html>";*/
                       // String token = UUID.randomUUID().toString();
                        String tempPassword = RandomStringUtils.randomAlphanumeric(6);
                        participantDetails.setPassword(FdahpUserRegUtil.getEncryptedString(tempPassword));
                        participantDetails.setTempPassword(true);
                        UserDetails upParticipantDetails = FdahpUserRegWSManager.get().saveParticipant(participantDetails);
                        if(upParticipantDetails != null){
                            /*String message = "<html> <body>" +
                                    "Dear "+participantDetails.getFirstName()+" "+participantDetails.getLastName()+",<BR><p> Please click the below link to reset the password</p>" +
                                    "<a href='http://192.168.0.6:8081:8080/labkey/fdahpUserRegWS/home/createPassword.api?token="+token+"'>reset password</a>"+
                                    " <BR>Thanks,<BR>"+
                                    "<BR>"+"</body></html>";*/
                            String message = "<html> <body>" +
                                    "Dear "+participantDetails.getFirstName()+" "+participantDetails.getLastName()+",<BR><p> Your new password is '<b>" + tempPassword + "</b>'</p>" +
                                    "<BR>Thanks,<BR>"+
                                    "<BR>"+"</body></html>";
                            boolean isMailSent = FdahpUserRegUtil.sendemail(participantDetails.getEmail(),"ForgotPasswordLink",message);
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
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_103.getValue(),FdahpUserRegUtil.ErrorCodes.EMAIL_NOT_EXISTS.getValue(), FdahpUserRegUtil.ErrorCodes.EMAIL_NOT_EXISTS.getValue(), getViewContext().getResponse());
                        errors.rejectValue("emailId",ERROR_MSG,FdahpUserRegUtil.ErrorCodes.EMAIL_NOT_EXISTS.getValue());
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
            try
            {
                if(auth != null && StringUtils.isNotEmpty(auth))
                {
                    isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(auth);
                    if (isAuthenticated)
                    {
                        if(loginForm != null && loginForm.getEmailId() != null && StringUtils.isNotEmpty(loginForm.getEmailId())){
                            UserDetails participantDetails = FdahpUserRegWSManager.get().getParticipantDetailsByEmail(loginForm.getEmailId());
                            System.out.println(participantDetails.getStatus());
                            if(participantDetails != null && participantDetails.getStatus() == 2){
                                String message = "<html> <body>" +
                                        "Dear "+participantDetails.getFirstName()+" "+participantDetails.getLastName()+",<BR><p> Please click the below link to confirm the Account</p>" +
                                        "<p><a href='http://192.168.0.6:8081/labkey/fdahpUserRegWS/home/verify.api?token="+participantDetails.getSecurityToken()+"'>Confirm Account</a></p>"+
                                        " <BR>Thanks,<BR>"+
                                        "<BR>"+"</body></html>";
                                boolean isMailSent = FdahpUserRegUtil.sendemail(participantDetails.getEmail(),"Verification Email",message);
                                if (isMailSent){
                                    response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                                }else{
                                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.FAILURE_TO_SENT_MAIL.getValue(), getViewContext().getResponse());
                                    return null;
                                }
                            }else{
                                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.RESEND_EMAIL_NOT_EXISTS.getValue(), FdahpUserRegUtil.ErrorCodes.RESEND_EMAIL_NOT_EXISTS.getValue(), getViewContext().getResponse());
                                return null;
                            }
                        }else{
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
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
                                    UserDetails participantDetails = FdahpUserRegWSManager.get().getParticipantDetails(userId);
                                    if(participantDetails != null ){
                                        if(participantDetails.getPassword().equalsIgnoreCase(FdahpUserRegUtil.getEncryptedString(oldPassword))){
                                            participantDetails.setPassword(FdahpUserRegUtil.getEncryptedString(newPassword));
                                            if(participantDetails.getTempPassword())
                                                participantDetails.setTempPassword(false);
                                            UserDetails updParticipantDetails = FdahpUserRegWSManager.get().saveParticipant(participantDetails);
                                            if(updParticipantDetails != null && !participantDetails.getTempPassword()){
                                                String message = FdahpUserRegWSManager.get().savePasswordHistory(userId,FdahpUserRegUtil.getEncryptedString(newPassword));
                                                if(message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue()))
                                                    response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());

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
                                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.NEW_PASSWORD_NOT_SAME_LAST_PASSWORD.getValue(),FdahpUserRegUtil.ErrorCodes.NEW_PASSWORD_NOT_SAME_LAST_PASSWORD.getValue(), getViewContext().getResponse());
                                    errors.rejectValue("currentPassword",ERROR_MSG,FdahpUserRegUtil.ErrorCodes.NEW_PASSWORD_NOT_SAME_LAST_PASSWORD.getValue());
                                }

                            }else{
                                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_102.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.OLD_PASSWORD_AND_NEW_PASSWORD_NOT_SAME.getValue(), getViewContext().getResponse());
                                errors.rejectValue("currentPassword",ERROR_MSG,FdahpUserRegUtil.ErrorCodes.OLD_PASSWORD_AND_NEW_PASSWORD_NOT_SAME.getValue());
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
                                if (profileForm.getProfile() != null)
                                {
                                    if (profileForm.getProfile().getFirstName() != null && StringUtils.isNotEmpty(profileForm.getProfile().getFirstName()))
                                        participantDetails.setFirstName(profileForm.getProfile().getFirstName());
                                    if (profileForm.getProfile().getLastName() != null && StringUtils.isNotEmpty(profileForm.getProfile().getLastName()))
                                        participantDetails.setLastName(profileForm.getProfile().getLastName());
                                    if (profileForm.getProfile().getEmailId() != null && StringUtils.isNotEmpty(profileForm.getProfile().getEmailId()))
                                        participantDetails.setEmail(profileForm.getProfile().getEmailId());
                                }
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
                                    if(profileForm.getSettings().getRemindersTime() != null && StringUtils.isNotEmpty(profileForm.getSettings().getRemindersTime()))
                                        participantDetails.setReminderTime(profileForm.getSettings().getRemindersTime());
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
                                                if (participantInfoBean.getStudyId().equals(participantStudies.getStudyId()))
                                                {
                                                    isExists = true;
                                                    if (participantInfoBean.getAppToken() != null && StringUtils.isNotEmpty(participantInfoBean.getAppToken()))
                                                        participantStudies.setAppToken(participantInfoBean.getAppToken());
                                                     participantStudies.setStatus(FdahpUserRegUtil.ErrorCodes.JOINED.getValue());
                                                    addParticipantStudiesList.add(participantStudies);
                                                }
                                            }
                                        }
                                        if (!isExists)
                                        {
                                            ParticipantStudies participantStudies = new ParticipantStudies();
                                            if (participantInfoBean.getAppToken() != null && StringUtils.isNotEmpty(participantInfoBean.getAppToken()))
                                                participantStudies.setAppToken(participantInfoBean.getAppToken());
                                            if (participantInfoBean.getStudyId() != null && StringUtils.isNotEmpty(participantInfoBean.getStudyId()))
                                                participantStudies.setStudyId(participantInfoBean.getStudyId());
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
                                             if(Integer.valueOf(studiesBean.getStudyId()).equals(participantStudies.getStudyId())){
                                                 isExists = true;
                                                 if(studiesBean.getStatus() != null && StringUtils.isNotEmpty(studiesBean.getStatus()))
                                                     participantStudies.setStatus(studiesBean.getStatus());
                                                 if(studiesBean.getBookmarked() != null)
                                                     participantStudies.setBookmark(studiesBean.getBookmarked());
                                                 if(studiesBean.getEnrolledDate() != null)
                                                     participantStudies.setEnrolledDate(studiesBean.getEnrolledDate());
                                                 addParticipantStudiesList.add(participantStudies);
                                             }
                                         }
                                    }
                                    if(!isExists){
                                        ParticipantStudies participantStudies = new ParticipantStudies();
                                        //participantStudies.setParticipantId(Integer.valueOf(userId));
                                        if(studiesBean.getStudyId()!= null && StringUtils.isNotEmpty(studiesBean.getStudyId()))
                                            participantStudies.setStudyId(studiesBean.getStudyId());
                                        if(studiesBean.getStatus()!= null && StringUtils.isNotEmpty(studiesBean.getStatus()))
                                            participantStudies.setStatus(studiesBean.getStatus());
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
                            if(preferencesForm.getActivities() != null && preferencesForm.getActivities().size() > 0){
                                List<ActivitiesBean> activitiesBeanList = preferencesForm.getActivities();
                                List<ParticipantActivities> existedParticipantActivitiesList = FdahpUserRegWSManager.get().getParticipantActivitiesList(userId);
                                for (int i=0;i < activitiesBeanList.size() ; i++){
                                    ActivitiesBean activitiesBean = activitiesBeanList.get(i);
                                    boolean isExists = false;
                                    if(existedParticipantActivitiesList != null && existedParticipantActivitiesList.size() > 0)
                                        for(ParticipantActivities participantActivities : existedParticipantActivitiesList){
                                            if(activitiesBean.getStudyId().equals(participantActivities.getStudyId()) && Integer.valueOf(activitiesBean.getActivityId()).equals(participantActivities.getActivityId())){
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
                                            participantActivities.setActivityId(Integer.valueOf(activitiesBean.getActivityId()));
                                        if(activitiesBean.getStudyId() != null && StringUtils.isNotEmpty(activitiesBean.getStudyId()))
                                            participantActivities.setStudyId(activitiesBean.getStudyId());
                                        if(userId!= null && StringUtils.isNotEmpty(userId))
                                            participantActivities.setParticipantId(userId);
                                        if(activitiesBean.getStatus()!= null && StringUtils.isNotEmpty(activitiesBean.getStatus()))
                                            participantActivities.setStatus(activitiesBean.getStatus());
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
                 _log.error("UpdatePreferences Action Error :",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                return null;
            }
            return response;
        }
    }
    public static class PreferencesForm {


        public List<StudiesBean>  _studies;
        public List<ActivitiesBean>  _activities;

        public List<StudiesBean> getStudies()
        {
            return _studies;
        }

        public void setStudies(List<StudiesBean> studies)
        {
            _studies = studies;
        }

        public List<ActivitiesBean> getActivities()
        {
            return _activities;
        }

        public void setActivities(List<ActivitiesBean> activities)
        {
            _activities = activities;
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
                                        }
                                        updateConsent = FdahpUserRegWSManager.get().saveStudyConsent(consent);
                                    }
                                    if(updateConsent != null && message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue())){
                                        response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),message);
                                    }
                                }else{
                                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_103.getValue(),FdahpUserRegUtil.ErrorCodes.NO_DATA_AVAILABLE.getValue(),FdahpUserRegUtil.ErrorCodes.NO_DATA_AVAILABLE.getValue(), getViewContext().getResponse());
                                    errors.rejectValue("studyId",ERROR_MSG,"No Data available with the studyId");
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
                _log.error("UserPreferencesAction Action Error",e);
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
                _log.error("UserPreferencesAction Action Error",e);
               FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
               return null;

           }
           return response;
       }
   }

   public static class ActivityStateForm {

       private String _studyId;
       private String _activityId;
       private String _activityVersion;
       private String _activityState;
       private String _activityRunId;

       public String getStudyId()
       {
           return _studyId;
       }

       public void setStudyId(String studyId)
       {
           _studyId = studyId;
       }

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
   @RequiresNoPermission
    public class UpdateActivityStateAction extends  ApiAction<ActivityStateForm>{

       @Override
       protected ModelAndView handleGet() throws Exception
       {
           getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the POST method when calling this action.");
           return null;
       }

       @Override
       public ApiResponse execute(ActivityStateForm activityStateForm, BindException errors) throws Exception
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
                       if(activityStateForm != null && userId != null && StringUtils.isNotEmpty(userId)){
                           if((activityStateForm.getStudyId() != null && StringUtils.isNotEmpty(activityStateForm.getStudyId())) && (activityStateForm.getActivityId() != null && StringUtils.isNotEmpty(activityStateForm.getActivityId()))){
                               List<ParticipantActivities> participantActivitiesList;
                               participantActivitiesList = FdahpUserRegWSManager.get().getParticipantActivitiesList(activityStateForm.getStudyId(),userId);
                               boolean isExist = false;
                               if(participantActivitiesList != null && participantActivitiesList.size()>0){
                                   for (ParticipantActivities participantActivities : participantActivitiesList)
                                   {
                                       if (participantActivities.getActivityId().equals(Integer.valueOf(activityStateForm.getActivityId())))
                                       {
                                           isExist = true;
                                           if(activityStateForm.getActivityVersion()!=null && StringUtils.isNotEmpty(activityStateForm.getActivityVersion()))
                                               participantActivities.setActivityVersion(activityStateForm.getActivityVersion());
                                           if(activityStateForm.getActivityState()!= null && StringUtils.isNotEmpty(activityStateForm.getActivityState()))
                                               participantActivities.setActivityState(activityStateForm.getActivityState());
                                           if(activityStateForm.getActivityRunId() != null && StringUtils.isNotEmpty(activityStateForm.getActivityRunId()))
                                               participantActivities.setActivityRunId(activityStateForm.getActivityRunId());
                                           addParticipantActivitiesList.add(participantActivities);
                                       }
                                   }

                               }
                               if(!isExist){
                                   ParticipantActivities addParticipantActivities = new ParticipantActivities();
                                   if(activityStateForm.getActivityState()!= null && StringUtils.isNotEmpty(activityStateForm.getActivityState()))
                                       addParticipantActivities.setActivityState(activityStateForm.getActivityState());
                                   if(activityStateForm.getActivityVersion()!=null && StringUtils.isNotEmpty(activityStateForm.getActivityVersion()))
                                       addParticipantActivities.setActivityVersion(activityStateForm.getActivityVersion());
                                   if(activityStateForm.getActivityId() != null && StringUtils.isNotEmpty(activityStateForm.getActivityId()))
                                       addParticipantActivities.setActivityId(Integer.valueOf(activityStateForm.getActivityId()));
                                   if(activityStateForm.getActivityRunId() != null && StringUtils.isNotEmpty(activityStateForm.getActivityRunId()))
                                       addParticipantActivities.setActivityRunId(activityStateForm.getActivityRunId());
                                   if(activityStateForm.getStudyId()!=null && StringUtils.isNotEmpty(activityStateForm.getStudyId()))
                                       addParticipantActivities.setStudyId(activityStateForm.getStudyId());
                                   if(userId!= null && StringUtils.isNotEmpty(userId))
                                       addParticipantActivities.setParticipantId(userId);
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
               _log.error("UserPreferencesAction Action Error",e);
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
                              System.out.println(participantStudies);
                               if(participantStudies != null){
                                    if(participantStudies.getStatus().equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.WITHDRAWN.getValue())){
                                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_103.getValue(),FdahpUserRegUtil.ErrorCodes.NO_DATA_AVAILABLE.getValue(),FdahpUserRegUtil.ErrorCodes.WITHDRAWN_STUDY.getValue(), getViewContext().getResponse());
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
                       if(studyId != null && StringUtils.isNoneBlank(studyId) && userId != null && StringUtils.isNotEmpty(userId) && consentVersion != null && StringUtils.isNoneBlank(consentVersion)){
                           // ParticipantStudies participantStudies = FdahpUserRegWSManager.get().getParticipantStudies(Integer.valueOf(studyId),Integer.valueOf(userId));
                           StudyConsent studyConsent = FdahpUserRegWSManager.get().getStudyConsent(userId,studyId,consentVersion);
                           if(studyConsent != null){
                               JSONObject jsonObject  = new JSONObject();
                               if(studyConsent.getVersion() != null)
                                   jsonObject.put("version",studyConsent.getVersion());
                               if(studyConsent.getPdf() != null)
                                   jsonObject.put("pdf",studyConsent.getPdf());
                               response.put("consent",jsonObject);
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
               _log.error("UserPreferencesAction Action Error",e);
               FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
               return null;
           }
           return response;
       }
   }

   @RequiresNoPermission
    public class CreatePasswordAction extends ApiAction<ChangePasswordForm>{

       /*@Override
       protected ModelAndView handleGet() throws Exception
       {
           getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the POST method when calling this action.");
           return null;
       }*/

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
   }

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
}