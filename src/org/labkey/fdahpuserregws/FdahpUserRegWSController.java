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
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.util.SystemOutLogger;
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
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.fdahpuserregws.bean.ActivitiesBean;
import org.labkey.fdahpuserregws.bean.InfoBean;
import org.labkey.fdahpuserregws.bean.ParticipantForm;
import org.labkey.fdahpuserregws.bean.ParticipantInfoBean;
import org.labkey.fdahpuserregws.bean.ProfileBean;
import org.labkey.fdahpuserregws.bean.SettingsBean;
import org.labkey.fdahpuserregws.bean.StudiesBean;
import org.labkey.fdahpuserregws.model.AuthInfo;
import org.labkey.fdahpuserregws.model.FdahpUserRegUtil;
import org.labkey.fdahpuserregws.model.ParticipantActivities;
import org.labkey.fdahpuserregws.model.ParticipantDetails;
import org.labkey.fdahpuserregws.model.ParticipantStudies;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class FdahpUserRegWSController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(FdahpUserRegWSController.class);
    public static final String NAME = "fdahpuserregws";

    public FdahpUserRegWSController()
    {
        setActionResolver(_actionResolver);
    }

    private static final Logger _log = Logger.getLogger(FdahpUserRegWSController.class);

    @RequiresPermission(ReadPermission.class)
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

    @RequiresPermission(ReadPermission.class)
    public class PingAction extends ApiAction<Object>
    {

        @Override
        public ApiResponse execute(Object o, BindException errors) throws Exception
        {
            ParticipantDetails participantDetails = new ParticipantDetails();
            ApiSimpleResponse apiSimpleResponse = new ApiSimpleResponse();
            apiSimpleResponse.put("reponse", "FdahpUserResWebServices Works!");
            apiSimpleResponse.put(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase(), true);
            return apiSimpleResponse;
        }
    }
    @RequiresPermission(ReadPermission.class)
    public class RegisterAction extends ApiAction<ParticipantForm>{

        @Override
        protected ModelAndView handleGet() throws Exception
        {
            getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the POST method when calling this action.");
            return null;
        }

        /*@Override
        public void validateForm(ParticipantFrom  form, Errors errors){
            if
            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString(),"Invalid Input");
        }*/

        @Override
        public Object execute(ParticipantForm participantForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            ParticipantDetails addParticipantDetails=null;
            try{
                if(StringUtils.isNotBlank(participantForm.getFirstName()) && StringUtils.isNotBlank(participantForm.getLastName())
                        && StringUtils.isNotBlank(participantForm.getEmail()) && StringUtils.isNotBlank(participantForm.getPassword())){
                    List<ParticipantDetails> participantDetailses = FdahpUserRegWSManager.get().getParticipantDetailsByEmail(getContainer(),participantForm.getEmail());
                    if(participantDetailses != null && participantDetailses.size() > 0){
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), FdahpUserRegUtil.ErrorCodes.EMAIL_EXISTS.getValue(), getViewContext().getResponse());
                        errors.rejectValue("email",ERROR_MSG,FdahpUserRegUtil.ErrorCodes.EMAIL_EXISTS.getValue());
                    }else{
                        addParticipantDetails = FdahpUserRegWSManager.get().saveParticipant(getContainer(),getParticipant(participantForm));
                        if(addParticipantDetails != null){
                            response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                            response.put("userId", addParticipantDetails.getId());
                            if(addParticipantDetails.getStatus() == 2)
                                response.put("verified", false);
                            if(addParticipantDetails.getStatus() == 1)
                                response.put("verified", true);
                            if(addParticipantDetails.getId() != null)
                            {
                                AuthInfo authInfo = FdahpUserRegWSManager.get().saveAuthInfo(getContainer(), addParticipantDetails.getId());
                                if(authInfo != null){
                                    response.put("auth", authInfo.getAuthKey());
                                }
                            }
                        }else{
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                            getViewContext().getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue());
                            return null;
                        }
                    }
                }else{
                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                    if (StringUtils.trimToNull(participantForm.getLastName()) == null)
                        errors.rejectValue("firstName",ERROR_MSG,"First Name is required.");
                    if (StringUtils.trimToNull(participantForm.getFirstName()) == null)
                        errors.rejectValue("lastName",ERROR_MSG,  "Last Name is required.");
                    if (StringUtils.trimToNull(participantForm.getEmail()) == null)
                        errors.rejectValue("email",ERROR_MSG,"email is required.");
                    if (StringUtils.trimToNull(participantForm.getPassword()) == null)
                        errors.rejectValue("password",ERROR_MSG,"password is required.");
                    getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue());
                    return null;
                }

            }catch (Exception e){
                 _log.error("register action:",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                getViewContext().getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue());
                return null;

            }
            return response;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ConfirmRegistrationAction extends ApiAction{

        @Override
        public Object execute(Object o, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            try{
                String userId = getViewContext().getRequest().getHeader("userId");
                String auth = getViewContext().getRequest().getHeader("auth");
                boolean isAuthenticated=false;
                if(StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(auth)){
                    isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(getContainer(),auth,Integer.valueOf(userId));
                    if(isAuthenticated){
                        ParticipantDetails participantDetails = FdahpUserRegWSManager.get().getParticipantDetails(getContainer(),Integer.valueOf(userId));
                        if(null != participantDetails){
                            participantDetails.setStatus(1);
                            ParticipantDetails updateParticipantDetails = FdahpUserRegWSManager.get().saveParticipant(getContainer(),participantDetails);
                            if(null != updateParticipantDetails){
                                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                                response.put("userId", updateParticipantDetails.getId());
                                if(updateParticipantDetails.getStatus() == 2)
                                    response.put("verified", false);
                                if(updateParticipantDetails.getStatus() == 1)
                                    response.put("verified", true);
                                response.put("auth", auth);
                            }
                        }
                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                        getViewContext().getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED, FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue());
                        return null;
                    }

                }else{
                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                    getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                    return null;
                }
            }catch (Exception e){
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                getViewContext().getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue());
                 _log.error("ConfirmRegistration action:",e);
                return null;
            }
            return response;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class LoginAction extends ApiAction<Object>{

        @Override
        protected ModelAndView handleGet() throws Exception
        {
            getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the POST method when calling this action.");
            return null;
        }

        @Override
        public ApiResponse execute(Object o, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            ParticipantForm participantForm = null;
            try{
                String email = getViewContext().getRequest().getHeader("emailId");
                String password = getViewContext().getRequest().getHeader("password");
                if(StringUtils.isNotBlank(email) && StringUtils.isNotBlank(password)){
                    participantForm= FdahpUserRegWSManager.get().signingParticipant(getContainer(),email,password);
                  //  System.out.println("participantForm:"+participantForm);
                    if(null!=participantForm){
                        response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                        response.put("userId",participantForm.getUserId());
                        response.put("auth",participantForm.getAuth());
                        if(participantForm.getStatus() == 2)
                        {
                            response.put("verified", false);
                        }
                        if(participantForm.getStatus() == 1){
                            response.put("verified", true);
                        }
                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INACTIVE.getValue(),FdahpUserRegUtil.ErrorCodes.INACTIVE.name(), FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue(), getViewContext().getResponse());
                        getViewContext().getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED, FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue());
                        return null;
                    }
                }else{
                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.name(), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                    getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue());
                    return null;
                }
            }catch (Exception e){
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.name(), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                getViewContext().getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue());
                 _log.error("Login Action:",e);
                return null;
            }
            return response;
        }
    }
    private ParticipantDetails getParticipant(ParticipantForm form){
        ParticipantDetails participantDetails = null;
        if(null != form.getUserId()){
            participantDetails = FdahpUserRegWSManager.get().getParticipantDetails(getContainer(),form.getUserId());
        }
        if (participantDetails == null){
            participantDetails = new ParticipantDetails();
            participantDetails.setStatus(2);
        }
        if(form.getFirstName() != null)
            participantDetails.setFirstName(form.getFirstName());
        if(form.getLastName() != null)
            participantDetails.setLastName(form.getLastName());
        if(form.getEmail() !=null)
            participantDetails.setEmail(form.getEmail());
        if(form.getPassword() != null)
            participantDetails.setPassword(FdahpUserRegUtil.getEncryptedString(form.getPassword()));
        if(form.getUsePassCode() != null)
            participantDetails.setUsePassCode(form.getUsePassCode());
        if(form.getLocalNotification() != null)
            participantDetails.setLocalNotificationFlag(form.getLocalNotification());
        if(form.getReminderFlag() != null)
            participantDetails.setReminderFlag(form.getReminderFlag());
        if(form.getRemoteNotification() != null)
            participantDetails.setRemoteNotificationFlag(form.getRemoteNotification());
        if(form.getTouchId() != null)
            participantDetails.setTouchId(form.getTouchId());
        return participantDetails;
    }
    @RequiresPermission(ReadPermission.class)
    public class ForgotPasswordAction extends ApiAction<Object>
    {

        @Override
        public ApiResponse execute(Object o, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            try{
                String emailId = getViewContext().getRequest().getHeader("emailId");
                if(StringUtils.isNotBlank(emailId)){
                    ParticipantDetails participantDetails = FdahpUserRegWSManager.get().getParticipantDetailsByEmail(emailId);
                    if(participantDetails != null){
                       /* String password =  RandomStringUtils.randomAlphanumeric(6);
                        participantDetails.setPassword(FdahpUserRegUtil.getEncryptedString(password));
                        String messageBody = "<html> <body>" +
                                "Dear "+participantDetails.getFirstName()+" "+participantDetails.getLastName()+",<BR><p> Your new password is '<b>" + password + "</b>'</p>" +
                                " <BR>Thanks,<BR>BTC Soft"+
                                "<BR>"+"</body></html>";*/
                       String message = "<html> <body>" +
                               "Dear "+participantDetails.getFirstName()+" "+participantDetails.getLastName()+",<BR><p> Please click the below link to reset the password</p>" +
                               "<a href='www.google.co.in'>reset password</a>"+
                               " <BR>Thanks,<BR>"+
                               "<BR>"+"</body></html>";
                       boolean isMailSent = sendemail(participantDetails.getEmail(),"ForgotPasswordLink",message);
                        if (isMailSent){
                            response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                        }else{
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                            getViewContext().getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED, FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue());
                            return null;
                        }
                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                        getViewContext().getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue());
                        return null;
                    }
                }else{
                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue(), getViewContext().getResponse());
                    getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT_ERROR_MSG.getValue());
                    return null;
                }
            }catch (Exception e){
                 _log.error("ForgotPassword Action Error:",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                getViewContext().getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue());
                return null;
            }
            return response;
        }
    }
    public boolean sendemail(String email, String subject, String messageBody) throws Exception{

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
    }

    @RequiresPermission(ReadPermission.class)
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
            String userId = getViewContext().getRequest().getHeader("userId");
            String auth = getViewContext().getRequest().getHeader("auth");
            boolean isAuthenticated = false;
            try{
                if(StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(auth)){
                    isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(getContainer(),auth,Integer.valueOf(userId));
                    if(isAuthenticated){
                        String oldPassword = form.getOldPassword();
                        String newPassword = form.getNewPassword();
                        ParticipantDetails participantDetails = FdahpUserRegWSManager.get().getParticipantDetails(getContainer(),Integer.valueOf(userId));
                        if(participantDetails !=null ){
                           if(participantDetails.getPassword().equalsIgnoreCase(oldPassword)){
                               participantDetails.setPassword(FdahpUserRegUtil.getEncryptedString(newPassword));
                               ParticipantDetails updParticipantDetails = FdahpUserRegWSManager.get().saveParticipant(getContainer(),participantDetails);
                               if(updParticipantDetails != null){
                                   response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                               }
                           }else{
                               FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                               getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                               return null;
                           }
                        }else{
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(),FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                            getViewContext().getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED, FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.toString());
                            return null;
                        }

                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                        getViewContext().getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED, FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue());
                        return null;
                    }

                }else{
                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                    getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                    return null;
                }
            }catch (Exception e){
                 _log.error("ChangePassword Action Error",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                getViewContext().getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue());
                return null;
            }
            return response;
        }
    }
    public static class ChangePasswordForm extends ReturnUrlForm{
        private String _oldPassword;
        private String _newPassword;
        public String getOldPassword()
        {
            return _oldPassword;
        }

        public void setOldPassword(String oldPassword)
        {
            _oldPassword = oldPassword;
        }

        public String getNewPassword()
        {
            return _newPassword;
        }

        public void setNewPassword(String newPassword)
        {
            _newPassword = newPassword;
        }
    }
    @RequiresPermission(ReadPermission.class)
    public class LogoutAction extends  ApiAction<Object>{

        @Override
        public Object execute(Object o, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            boolean isAuthenticated = false;
            String message = FdahpUserRegUtil.ErrorCodes.FAILURE.getValue();
            try
            {
                 if(isDelete()){
                    String userId = getViewContext().getRequest().getHeader("userId");
                    String auth = getViewContext().getRequest().getHeader("auth");
                    if(StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(auth)){
                        isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(getContainer(),auth,Integer.valueOf(userId));
                        if(isAuthenticated){
                            message = FdahpUserRegWSManager.get().signout(Integer.valueOf(userId));
                            if(message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue())){
                                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                            }else{
                                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(), FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                                getViewContext().getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue());
                                return null;
                            }

                        }else{
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                            getViewContext().getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED, FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue());
                            return null;
                        }

                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                        getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                        return null;
                    }
                }else{
                    getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the DELETE method when calling this action.");
                    return null;
                }

            }catch (Exception e){
                 _log.error("Logout Action Error:",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                getViewContext().getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue());
                return null;
            }
            return response;
        }
    }
    @Marshal(Marshaller.Jackson)
    @RequiresPermission(ReadPermission.class)
    public class UserProfileAction extends ApiAction<Object>{

        @Override
        public ApiResponse execute(Object o, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            boolean isAuthenticated = false;
            try{
                String userId = getViewContext().getRequest().getHeader("userId");
                String auth = getViewContext().getRequest().getHeader("auth");
                if(StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(auth)){
                    isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(getContainer(),auth,Integer.valueOf(userId));
                    if(isAuthenticated){
                        response = FdahpUserRegWSManager.get().getParticipantDetails(Integer.valueOf(userId));

                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                        getViewContext().getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED, FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue());
                        return null;
                    }
                }else{
                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                    getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                    return null;
                }
            }catch (Exception e){
                _log.error("User Profile Action",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                getViewContext().getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue());
                return null;
            }
            return response;
        }
    }
    @Marshal(Marshaller.Jackson)
    @RequiresPermission(ReadPermission.class)
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
                String userId = getViewContext().getRequest().getHeader("userId");
                String auth = getViewContext().getRequest().getHeader("auth");
                boolean isAuthenticated = false;
                if(StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(auth)){
                    isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(getContainer(),auth,Integer.valueOf(userId));
                    if(isAuthenticated) if (profileForm != null)
                    {
                        //participantForm.setUserId(Integer.valueOf(userId));
                        //ParticipantDetails participantDetails = getParticipant(participantForm);
                        ParticipantDetails participantDetails = FdahpUserRegWSManager.get().getParticipantDetails(getContainer(), Integer.valueOf(userId));
                        if (participantDetails != null)
                        {
                            if (profileForm.getProfile() != null)
                            {
                                if (profileForm.getProfile().getFirstName() != null)
                                    participantDetails.setFirstName(profileForm.getProfile().getFirstName());
                                if (profileForm.getProfile().getLastName() != null)
                                    participantDetails.setLastName(profileForm.getProfile().getLastName());
                                if (profileForm.getProfile().getEmailId() != null)
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
                                if (profileForm.getSettings().getTouchId())
                                    participantDetails.setTouchId(profileForm.getSettings().getTouchId());
                            }
                            if (profileForm.getInfo() != null)
                            {
                                AuthInfo authInfo = FdahpUserRegWSManager.get().getAuthInfo(getContainer(), auth, Integer.valueOf(userId));
                                if (authInfo != null)
                                {
                                    if (profileForm.getInfo().getOs() != null){
                                        authInfo.setDeviceType(profileForm.getInfo().getOs());
                                    }
                                    if (profileForm.getInfo().getOs() != null && (profileForm.getInfo().getOs().equalsIgnoreCase("IOS") || profileForm.getInfo().getOs().equalsIgnoreCase("I"))){
                                        authInfo.setIosAppVersion(profileForm.getInfo().getAppVersion());
                                    }else{
                                        authInfo.setAndroidAppVersion(profileForm.getInfo().getAppVersion());
                                    }
                                    if(profileForm.getInfo().getDeviceToken() != null){
                                        authInfo.setDeviceToken(profileForm.getInfo().getDeviceToken());
                                    }
                                    updaAuthInfo =  FdahpUserRegWSManager.get().updateAuthInfo(authInfo);
                                }
                            }
                            if (profileForm.getParticipantInfo() != null && profileForm.getParticipantInfo().size() > 0)
                            {
                                List<ParticipantStudies> participantStudiesList = FdahpUserRegWSManager.get().getParticipantStudiesList(Integer.valueOf(userId));
                                for (int i = 0; i < profileForm.getParticipantInfo().size(); i++)
                                {
                                    ParticipantInfoBean participantInfoBean = profileForm.getParticipantInfo().get(i);
                                    boolean isExists = false;
                                    if (participantStudiesList != null && participantStudiesList.size() > 0)
                                    {
                                        for (ParticipantStudies participantStudies : participantStudiesList)
                                        {
                                            if (Integer.valueOf(participantInfoBean.getStudyId()).equals(participantStudies.getStudyId()))
                                            {
                                                isExists = true;
                                                if(participantInfoBean.getAppToken() != null)
                                                    participantStudies.setAppToken(participantInfoBean.getAppToken());
                                                if(participantInfoBean.getParticipantId() != null)
                                                    participantStudies.setParticipantId(Integer.valueOf(participantInfoBean.getParticipantId()));
                                                participantStudies.setStatus(FdahpUserRegUtil.ErrorCodes.JOINED.getValue());
                                                addParticipantStudiesList.add(participantStudies);
                                            }
                                        }
                                    }
                                    if (!isExists)
                                    {
                                        ParticipantStudies participantStudies = new ParticipantStudies();
                                        if(participantInfoBean.getAppToken() != null)
                                            participantStudies.setAppToken(participantInfoBean.getAppToken());
                                        if(participantInfoBean.getParticipantId() != null)
                                            participantStudies.setParticipantId(Integer.valueOf(participantInfoBean.getParticipantId()));
                                        if(participantInfoBean.getStudyId() != null)
                                            participantStudies.setStudyId(Integer.valueOf(participantInfoBean.getStudyId()));
                                        participantStudies.setStatus(FdahpUserRegUtil.ErrorCodes.JOINED.getValue());
                                        participantStudies.setUserId(Integer.valueOf(userId));
                                        addParticipantStudiesList.add(participantStudies);
                                    }
                                }
                                message = FdahpUserRegWSManager.get().saveParticipantStudies(addParticipantStudiesList);
                            }
                            ParticipantDetails updateParticipantDetails = FdahpUserRegWSManager.get().saveParticipant(getContainer(), participantDetails);
                            if (updateParticipantDetails != null || message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue()) || updaAuthInfo != null)
                            {
                                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(), FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                            }
                        }
                    }
                    else
                    {
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                        getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                        return null;
                    }
                    else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                        getViewContext().getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED, FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue());
                        return null;
                    }
                }else{
                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                    getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                    return null;
                }
            }catch (Exception e){
                 _log.error("UpdateUSerProfile Action Error :",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                getViewContext().getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue());
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
    @RequiresPermission(ReadPermission.class)
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
                String userId = getViewContext().getRequest().getHeader("userId");
                String auth = getViewContext().getRequest().getHeader("auth");
                boolean isAuthenticated = false;
                if(StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(auth)){
                    isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(getContainer(),auth,Integer.valueOf(userId));
                    if(isAuthenticated){
                        if(preferencesForm != null){
                            if(preferencesForm.getStudies() != null){
                               List<StudiesBean> studiesBeenList = preferencesForm.getStudies();
                                List<ParticipantStudies> existParticipantStudies = FdahpUserRegWSManager.get().getParticipantStudiesList(Integer.valueOf(userId));
                                for (int i=0;i < studiesBeenList.size() ; i++){
                                    StudiesBean studiesBean =  studiesBeenList.get(i);
                                    boolean isExists = false;
                                    if(existParticipantStudies != null && existParticipantStudies.size() >0){
                                         for (ParticipantStudies participantStudies : existParticipantStudies){
                                             if(Integer.valueOf(studiesBean.getStudyId()).equals(participantStudies.getStudyId())){
                                                 isExists = true;
                                                 participantStudies.setStatus(studiesBean.getStatus());
                                                 participantStudies.setBookmark(studiesBean.getBookmarked());
                                                 addParticipantStudiesList.add(participantStudies);
                                             }
                                         }
                                    }
                                    if(!isExists){
                                        ParticipantStudies participantStudies = new ParticipantStudies();
                                        //participantStudies.setParticipantId(Integer.valueOf(userId));
                                        participantStudies.setStudyId(Integer.valueOf(studiesBean.getStudyId()));
                                        participantStudies.setStatus(studiesBean.getStatus());
                                        participantStudies.setBookmark(studiesBean.getBookmarked());
                                        participantStudies.setUserId(Integer.valueOf(userId));
                                        addParticipantStudiesList.add(participantStudies);
                                    }
                                }
                               FdahpUserRegWSManager.get().saveParticipantStudies(addParticipantStudiesList);
                            }
                            if(preferencesForm.getActivities() != null){
                                List<ActivitiesBean> activitiesBeanList = preferencesForm.getActivities();
                                List<ParticipantActivities> existedParticipantActivitiesList = FdahpUserRegWSManager.get().getParticipantActivitiesList(Integer.valueOf(userId));
                                for (int i=0;i < activitiesBeanList.size() ; i++){
                                    ActivitiesBean activitiesBean = activitiesBeanList.get(i);
                                    boolean isExists = false;
                                    if(existedParticipantActivitiesList != null && existedParticipantActivitiesList.size() > 0)
                                        for(ParticipantActivities participantActivities : existedParticipantActivitiesList){
                                            if(Integer.valueOf(activitiesBean.getStudyId()).equals(participantActivities.getStudyId()) && Integer.valueOf(activitiesBean.getActivityId()).equals(participantActivities.getActivityId())){
                                                isExists =true;
                                                participantActivities.setParticipantId(Integer.valueOf(userId));
                                                participantActivities.setStatus(activitiesBean.getStatus());
                                                participantActivities.setBookmark(activitiesBean.getBookmarked());
                                                participantActivities.setActivityVersion(activitiesBean.getActivityVersion());
                                                participantActivitiesList.add(participantActivities);
                                            }
                                        }
                                    if (!isExists){
                                        ParticipantActivities participantActivities = new ParticipantActivities();
                                        participantActivities.setActivityId(Integer.valueOf(activitiesBean.getActivityId()));
                                        participantActivities.setStudyId(Integer.valueOf(activitiesBean.getStudyId()));
                                        participantActivities.setParticipantId(Integer.valueOf(userId));
                                        participantActivities.setStatus(activitiesBean.getStatus());
                                        participantActivities.setBookmark(activitiesBean.getBookmarked());
                                        participantActivities.setActivityVersion(activitiesBean.getActivityVersion());
                                        participantActivitiesList.add(participantActivities);
                                    }
                                }
                                FdahpUserRegWSManager.get().saveParticipantActivities(participantActivitiesList);
                            }
                            response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());


                        }else{
                            FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                            getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                            return null;
                        }
                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                        getViewContext().getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED, FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue());
                        return null;
                    }
                }else{
                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                    getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                    return null;
                }
            }catch (Exception e){
                 _log.error("UpdatePreferences Action Error :",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                getViewContext().getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue());
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

    @Marshal(Marshaller.Jackson)
    @RequiresPermission(ReadPermission.class)
    public class UserPreferencesAction extends  ApiAction{

        @Override
        public ApiResponse execute(Object o, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            boolean isAuthenticated = false;
            try{
                String userId = getViewContext().getRequest().getHeader("userId");
                String auth = getViewContext().getRequest().getHeader("auth");
                if(StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(auth)){
                    isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(getContainer(),auth,Integer.valueOf(userId));
                    if(isAuthenticated){
                        response = FdahpUserRegWSManager.get().getPreferences(Integer.valueOf(userId));

                    }else{
                        FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                        getViewContext().getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED, FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue());
                        return null;
                    }
                }else{
                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                    getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                    return null;
                }
            }catch (Exception e){
                 _log.error("UserPreferencesAction Action Error",e);
                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                getViewContext().getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue());
                return null;

            }
            return response;
        }
    }

    public static class ConsentStatusForm{
        private  String _studyId;
        private  Boolean _eligibilityStatus;
        private  Boolean _consentStatus;
        private  String _consent;
        private  String _userId;

        public String getStudyId()
        {
            return _studyId;
        }

        public void setStudyId(String studyId)
        {
            _studyId = studyId;
        }

        public Boolean getEligibilityStatus()
        {
            return _eligibilityStatus;
        }

        public void setEligibilityStatus(Boolean eligibilityStatus)
        {
            _eligibilityStatus = eligibilityStatus;
        }

        public Boolean getConsentStatus()
        {
            return _consentStatus;
        }

        public void setConsentStatus(Boolean consentStatus)
        {
            _consentStatus = consentStatus;
        }

        public String getConsent()
        {
            return _consent;
        }

        public void setConsent(String consent)
        {
            _consent = consent;
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
    @RequiresPermission(ReadPermission.class)
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
           try{
               String userId = getViewContext().getRequest().getHeader("userId");
               String auth = getViewContext().getRequest().getHeader("auth");
               if(StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(auth)){
                   isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(getContainer(),auth,Integer.valueOf(userId));
                   if(isAuthenticated){
                       if(consentStatusForm != null ){
                            if(consentStatusForm.getStudyId() != null){
                                ParticipantStudies participantStudies = FdahpUserRegWSManager.get().getParticipantStudies(Integer.valueOf(consentStatusForm.getStudyId()),Integer.valueOf(userId));
                                if(participantStudies != null){
                                    if(consentStatusForm.getEligibilityStatus() != null){
                                        participantStudies.setEligbibility(consentStatusForm.getEligibilityStatus());
                                    }
                                    if(consentStatusForm.getConsentStatus() != null)
                                        participantStudies.setConsentStatus(consentStatusForm.getConsentStatus());
                                    if(consentStatusForm.getConsent()!= null)
                                        participantStudies.setConsent(consentStatusForm.getConsent());

                                    List<ParticipantStudies> participantStudiesList = new ArrayList<ParticipantStudies>();
                                    participantStudiesList.add(participantStudies);
                                    String message = FdahpUserRegWSManager.get().saveParticipantStudies(participantStudiesList);
                                    response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),message);
                                }else{
                                    FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                                    getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                                    return null;
                                }
                            }else{
                                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                                getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                                return null;
                           }

                       }else{
                           FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                           getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                           return null;
                       }

                   }else{
                       FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                       getViewContext().getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED, FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue());
                       return null;
                   }
               }else{
                   FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                   getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                   return null;
               }
           }catch (Exception e){
                _log.error("UserPreferencesAction Action Error",e);
               FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
               getViewContext().getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue());
               return null;
           }
           return response;
       }


   }

   @RequiresPermission(ReadPermission.class)
    public class ActivityStateAction extends ApiAction
   {

       @Override
       public ApiResponse execute(Object o, BindException errors) throws Exception
       {
           ApiSimpleResponse response  = new ApiSimpleResponse();
           boolean isAuthenticated = false;
           try{
               String userId = getViewContext().getRequest().getHeader("userId");
               String auth = getViewContext().getRequest().getHeader("auth");
               if(StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(auth)){
                   isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(getContainer(),auth,Integer.valueOf(userId));
                   if(isAuthenticated){
                       String studyId = getViewContext().getRequest().getHeader("studyId");
                       if(studyId != null){
                           List<ParticipantActivities> participantActivitiesList = FdahpUserRegWSManager.get().getParticipantActivitiesList(Integer.valueOf(studyId),Integer.valueOf(userId));
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
                                   jsonArray.put(jsonObject);
                               }
                           }
                           response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                           response.put(FdahpUserRegUtil.ErrorCodes.ACTIVITIES.getValue(),jsonArray);
                       }else{
                           FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                           getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                           return null;
                       }

                   }else{
                       FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                       getViewContext().getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED, FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue());
                       return null;
                   }
               }else{
                   FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                   getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                   return null;
               }
           }catch (Exception e){
                _log.error("UserPreferencesAction Action Error",e);
               FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
               getViewContext().getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue());
               return null;

           }
           return response;
       }
   }

   public static class ActivityStateForm {
       private String _userId;
       private String _studyId;
       private String _activityId;
       private String _activityVersion;
       private String _activityState;

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
   }
   @RequiresPermission(ReadPermission.class)
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
               String userId = getViewContext().getRequest().getHeader("userId");
               String auth = getViewContext().getRequest().getHeader("auth");
               List<ParticipantActivities> addParticipantActivitiesList = new ArrayList<ParticipantActivities>();
               if(StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(auth)){
                   isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(getContainer(),auth,Integer.valueOf(userId));
                   if(isAuthenticated){

                       if(activityStateForm != null){
                           if(activityStateForm.getStudyId() != null && activityStateForm.getActivityId() != null){
                               List<ParticipantActivities> participantActivitiesList;
                               participantActivitiesList = FdahpUserRegWSManager.get().getParticipantActivitiesList(Integer.valueOf(activityStateForm.getStudyId()),Integer.valueOf(userId));
                               boolean isExist = false;
                               if(participantActivitiesList != null && participantActivitiesList.size()>0){
                                   for (ParticipantActivities participantActivities : participantActivitiesList)
                                   {
                                       if (participantActivities.getActivityId().equals(Integer.valueOf(activityStateForm.getActivityId())))
                                       {
                                           isExist = true;
                                           participantActivities.setActivityVersion(activityStateForm.getActivityVersion());
                                           participantActivities.setActivityState(activityStateForm.getActivityState());
                                           addParticipantActivitiesList.add(participantActivities);
                                       }
                                   }

                               }
                               if(!isExist){
                                   ParticipantActivities addParticipantActivities = new ParticipantActivities();
                                   addParticipantActivities.setActivityState(activityStateForm.getActivityState());
                                   addParticipantActivities.setActivityVersion(activityStateForm.getActivityVersion());
                                   addParticipantActivities.setActivityId(Integer.valueOf(activityStateForm.getActivityId()));
                                   addParticipantActivities.setStudyId(Integer.valueOf(activityStateForm.getStudyId()));
                                   addParticipantActivities.setParticipantId(Integer.valueOf(userId));
                                   addParticipantActivitiesList.add(addParticipantActivities);
                               }
                               String message = FdahpUserRegWSManager.get().saveParticipantActivities(addParticipantActivitiesList);
                               if (message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue())){
                                   response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                               }else{
                                   FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                                   getViewContext().getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue());
                                   return null;
                               }
                           }else{
                               FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                               getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                               return null;
                           }
                       }else{
                           FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                           getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                           return null;
                       }
                   }else{
                       FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                       getViewContext().getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED, FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue());
                       return null;
                   }
               }else{
                   FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                   getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                   return null;
               }
           }catch (Exception e){
               _log.error("UserPreferencesAction Action Error",e);
               FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
               getViewContext().getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue());
               return null;

           }
          return response;
       }
   }

   @RequiresPermission(ReadPermission.class)
    public class WithdrawAction extends  ApiAction{

       @Override
       public ApiResponse execute(Object o, BindException errors) throws Exception
       {
           ApiSimpleResponse response = new ApiSimpleResponse();
           boolean isAuthenticated = false;
           String message = FdahpUserRegUtil.ErrorCodes.FAILURE.getValue();
           try
           {
               if(isDelete()){
                   String userId = getViewContext().getRequest().getHeader("userId");
                   String auth = getViewContext().getRequest().getHeader("auth");
                   if(StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(auth)){
                       isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(getContainer(),auth,Integer.valueOf(userId));
                       if(isAuthenticated){
                           String studyId = getViewContext().getRequest().getHeader("studyId");
                           if(null !=studyId){
                                message = FdahpUserRegWSManager.get().withDrawStudy(Integer.valueOf(studyId),Integer.valueOf(userId));
                               if(message.equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue())){
                                   response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                               }else{
                                   FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                                   getViewContext().getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue());
                                   return null;
                               }
                           }else{
                               FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                               getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                               return null;
                           }

                       }else{
                           FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                           getViewContext().getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED, FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue());
                           return null;
                       }

                   }else{
                       FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                       getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                       return null;
                   }
               }else{
                   getViewContext().getResponse().sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "You must use the DELETE method when calling this action.");
                   return null;
               }

           }catch (Exception e){
               _log.error("Withdraw Action Error:",e);
               FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
               getViewContext().getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue());
               return null;
           }
           return response;
       }
   }

   @RequiresPermission(ReadPermission.class)
    public class ConsentPDFAction extends ApiAction{

       @Override
       public ApiResponse execute(Object o, BindException errors) throws Exception
       {
           ApiSimpleResponse response = new ApiSimpleResponse();
           boolean isAuthenticated = false;
           try{
               String userId = getViewContext().getRequest().getHeader("userId");
               String auth = getViewContext().getRequest().getHeader("auth");
               if(StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(auth)){
                   isAuthenticated = FdahpUserRegWSManager.get().validatedAuthKey(getContainer(),auth,Integer.valueOf(userId));
                   if(isAuthenticated){
                       String studyId = getViewContext().getRequest().getHeader("studyId");
                       if(StringUtils.isNoneBlank(studyId)){
                            ParticipantStudies participantStudies = FdahpUserRegWSManager.get().getParticipantStudies(Integer.valueOf(studyId),Integer.valueOf(userId));
                            if(participantStudies != null){
                                if(participantStudies.getConsent() != null)
                                    response.put("consent",participantStudies.getConsent());
                                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
                            }else{
                                FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
                                getViewContext().getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue());
                                return  null;
                            }
                       }else{
                           FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                           getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                           return null;
                       }
                   }else{
                       FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_101.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_AUTH_CODE.getValue(), FdahpUserRegUtil.ErrorCodes.SESSION_EXPIRED_MSG.getValue(), getViewContext().getResponse());
                       getViewContext().getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED, FdahpUserRegUtil.ErrorCodes.ACCOUNT_DEACTIVATE_ERROR_MSG.getValue());
                       return null;
                   }
               }else{
                   FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(),FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.getValue(), getViewContext().getResponse());
                   getViewContext().getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, FdahpUserRegUtil.ErrorCodes.INVALID_INPUT.toString());
                   return null;
               }
           }catch (Exception e){
               _log.error("UserPreferencesAction Action Error",e);
               FdahpUserRegUtil.getFailureResponse(FdahpUserRegUtil.ErrorCodes.STATUS_104.getValue(),FdahpUserRegUtil.ErrorCodes.UNKNOWN.getValue(),FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue(), getViewContext().getResponse());
               getViewContext().getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, FdahpUserRegUtil.ErrorCodes.CONNECTION_ERROR_MSG.getValue());
               return null;

           }
           return response;
       }
   }
}