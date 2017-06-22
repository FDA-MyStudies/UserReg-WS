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
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.action.ApiSimpleResponse;

import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.gwt.client.AuditBehaviorType;
import org.labkey.api.query.FieldKey;
import org.labkey.api.audit.AuditLogService;
import org.labkey.fdahpuserregws.bean.ActivitiesBean;

import org.labkey.fdahpuserregws.bean.ParticipantForm;
import org.labkey.fdahpuserregws.bean.ParticipantInfoBean;
import org.labkey.fdahpuserregws.bean.ProfileBean;
import org.labkey.fdahpuserregws.bean.SettingsBean;
import org.labkey.fdahpuserregws.bean.StudiesBean;
import org.labkey.fdahpuserregws.model.AuthInfo;
import org.labkey.fdahpuserregws.model.FdahpUserRegUtil;
import org.labkey.fdahpuserregws.model.LoginAttempts;
import org.labkey.fdahpuserregws.model.ParticipantActivities;
import org.labkey.fdahpuserregws.model.PasswordHistory;
import org.labkey.fdahpuserregws.model.StudyConsent;
import org.labkey.fdahpuserregws.model.UserDetails;
import org.labkey.fdahpuserregws.model.ParticipantStudies;
import org.labkey.fdahpuserregws.FdahpUserRegWSController.DeactivateForm;

import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class FdahpUserRegWSManager
{
    private static final FdahpUserRegWSManager _instance = new FdahpUserRegWSManager();

    Properties configProp = FdahpUserRegUtil.getProperties();

    private FdahpUserRegWSManager()
    {
        // prevent external construction with a private default constructor
    }

    public static FdahpUserRegWSManager get()
    {
        return _instance;
    }

    private static final Logger _log = Logger.getLogger(FdahpUserRegWSManager.class);

    public UserDetails saveParticipant(UserDetails participant){
        DbScope dbScope = FdahpUserRegWSSchema.getInstance().getSchema().getScope();
        UserDetails addParticipant = null;
        DbScope.Transaction transaction = dbScope.ensureTransaction();
        try{
            TableInfo table = FdahpUserRegWSSchema.getInstance().getParticipantDetails();
            table.setAuditBehavior(AuditBehaviorType.DETAILED);
            if(null!=participant && participant.getId() == null){
                addParticipant = Table.insert(null,table, participant);
            }else{
                addParticipant = Table.update(null, table, participant, participant.getId());
            }

        }catch (Exception e){
            _log.error("saveParticipant:",e);
        }
        transaction.commit();
        return addParticipant;
    }

    public UserDetails getParticipantDetails(String id){
        SimpleFilter filter = new SimpleFilter();
        //filter.addCondition(FieldKey.fromParts("Id"), id);
        filter.addCondition(FieldKey.fromParts("UserId"), id);
        return new TableSelector(FdahpUserRegWSSchema.getInstance().getParticipantDetails(), filter, null).getObject(UserDetails.class);
    }

    public List<UserDetails> getParticipantDetailsListByEmail(String email){
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("Email"), email);
        return new TableSelector(FdahpUserRegWSSchema.getInstance().getParticipantDetails(),filter,null).getArrayList(UserDetails.class);
    }

    public AuthInfo saveAuthInfo(String userId){
        DbScope dbScope = FdahpUserRegWSSchema.getInstance().getSchema().getScope();
        UserDetails addParticipant = null;
        DbScope.Transaction transaction = dbScope.ensureTransaction();
        AuthInfo authInfo = null;
        try{
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition(FieldKey.fromParts("ParticipantId"), userId);
            authInfo = new TableSelector(FdahpUserRegWSSchema.getInstance().getAuthInfo(),filter,null).getObject(AuthInfo.class);
            String authKey = "0";
            authKey = RandomStringUtils.randomNumeric(9);
            TableInfo table = FdahpUserRegWSSchema.getInstance().getAuthInfo();
            table.setAuditBehavior(AuditBehaviorType.DETAILED);
            if(null !=authInfo){
                authInfo.setAuthKey(authKey);
                authInfo.setDeviceToken("");
                authInfo.setDeviceType("");
                authInfo.setModifiedOn(new Date());
                _log.info("session.expiration.days:"+FdahpUserRegUtil.addDays(FdahpUserRegUtil.getCurrentDateTime(),Integer.parseInt((String) configProp.get("session.expiration.days"))));
                authInfo.setSessionExpiredDate(FdahpUserRegUtil.addDays(FdahpUserRegUtil.getCurrentDateTime(),Integer.parseInt((String) configProp.get("session.expiration.days"))));
                Table.update(null,table, authInfo,authInfo.getAuthId());
            }else{
                authInfo = new AuthInfo();
                authInfo.setAuthKey(authKey);
                authInfo.setDeviceToken("");
                authInfo.setDeviceType("");
                authInfo.setParticipantId(userId);
                authInfo.setCreatedOn(new Date());
                _log.info("session.expiration.days:"+FdahpUserRegUtil.addDays(FdahpUserRegUtil.getCurrentDateTime(),Integer.parseInt((String) configProp.get("session.expiration.days"))));
                authInfo.setSessionExpiredDate(FdahpUserRegUtil.addDays(FdahpUserRegUtil.getCurrentDateTime(),Integer.parseInt((String) configProp.get("session.expiration.days"))));
                Table.insert(null,table, authInfo);
            }
        }catch (Exception e){
            _log.error("saveAuthInfo:",e);
        }
        transaction.commit();
        return authInfo;
    }
    /*public boolean validatedAuthKey(String authKey,Integer participantId){
        boolean isAuthenticated = false;
        try{
            AuthInfo authInfo = null;
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition(FieldKey.fromParts("AuthKey"), authKey);
            filter.addCondition(FieldKey.fromParts("ParticipantId"), participantId);
            authInfo  = new TableSelector(FdahpUserRegWSSchema.getInstance().getAuthInfo(),filter,null).getObject(AuthInfo.class);
            if(authInfo != null){
                isAuthenticated = true;
            }
        }catch (Exception e){
            _log.error("FdahpUserRegWSManger validatedAuthKey ()",e);
        }

        return isAuthenticated;
    }*/

    public boolean validatedAuthKey(String authKey){
        boolean isAuthenticated = false;
        try{
            AuthInfo authInfo = null;
            TableInfo table = FdahpUserRegWSSchema.getInstance().getAuthInfo();
            table.setAuditBehavior(AuditBehaviorType.DETAILED);
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition(FieldKey.fromParts("AuthKey"), authKey);
            authInfo  = new TableSelector(FdahpUserRegWSSchema.getInstance().getAuthInfo(),filter,null).getObject(AuthInfo.class);
            if(authInfo != null){
                if(FdahpUserRegUtil.getCurrentUtilDateTime().before(authInfo.getSessionExpiredDate()) || FdahpUserRegUtil.getCurrentUtilDateTime().equals(authInfo.getSessionExpiredDate())){
                    isAuthenticated = true;
                   /* authInfo.setModifiedOn(new Date());
                    authInfo.setSessionExpiredDate(FdahpUserRegUtil.addMinutes(FdahpUserRegUtil.getCurrentDateTime(),Integer.parseInt((String) configProp.get("session.expiration.minute"))));
                    Table.update(null,table, authInfo,authInfo.getAuthId());*/
                }

            }
        }catch (Exception e){
            _log.error("FdahpUserRegWSManger validatedAuthKey ()",e);
        }

        return isAuthenticated;
    }

    public ParticipantForm signingParticipant(String email, String password){
        ParticipantForm participantForm = null;
        UserDetails participantDetails = null;
        try{
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition(FieldKey.fromParts("Email"), email);
            filter.addCondition(FieldKey.fromParts("Password"), FdahpUserRegUtil.getEncryptedString(password));
            participantDetails = new TableSelector(FdahpUserRegWSSchema.getInstance().getParticipantDetails(),filter,null).getObject(UserDetails.class);
            if(null != participantDetails){
                participantForm = new ParticipantForm();
                AuthInfo authInfo = saveAuthInfo(participantDetails.getUserId());
                if(authInfo != null){
                    participantForm.setAuth(authInfo.getAuthKey());
                }
                participantForm.setUserId(participantDetails.getUserId());
                participantForm.setFirstName(participantDetails.getFirstName());
                participantForm.setStatus(participantDetails.getStatus());
                participantForm.setLastName(participantDetails.getLastName());
                participantForm.setEmailId(participantDetails.getEmail());
                participantForm.setTempPassword(participantDetails.getTempPassword());
                participantForm.setTempPasswordDate(participantDetails.getTempPasswordDate());
            }
        }catch (Exception e){
            _log.error("FdahpUserRegWSManager signingParticipant()",e);
        }
        return  participantForm;
    }

    public UserDetails getParticipantDetailsByEmail(String email){
        UserDetails participantDetails = null;
        try{
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition(FieldKey.fromParts("Email"), email);
            participantDetails =  new TableSelector(FdahpUserRegWSSchema.getInstance().getParticipantDetails(),filter,null).getObject(UserDetails.class);
        }catch (Exception e){
            _log.error("FdahpUserRegWSManager getParticipantDetailsByEmail()",e);
        }
        return  participantDetails;
    }

    public String signout(String userId){
        String message = FdahpUserRegUtil.ErrorCodes.FAILURE.getValue();
        try{
            DbSchema schema = FdahpUserRegWSSchema.getInstance().getSchema();
            TableInfo authInfo = FdahpUserRegWSSchema.getInstance().getAuthInfo();
            authInfo.setAuditBehavior(AuditBehaviorType.DETAILED);
            SqlExecutor executor = new SqlExecutor(schema);
            SQLFragment sqlUpdateVisitDates = new SQLFragment();

            sqlUpdateVisitDates.append("UPDATE ").append(authInfo.getSelectName()).append("\n")
                    .append("SET AuthKey = 0, DeviceToken = NULL, ModifiedOn='"+FdahpUserRegUtil.getCurrentDateTime()+"'")
                    .append(" WHERE ParticipantId = '"+userId+"'");
            int execute = executor.execute(sqlUpdateVisitDates);
            if (execute > 0){
                message = FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue();
            }
        }catch (Exception e){
            _log.error("FdahpUserRegWSManager signout error:",e);
        }
        return message;
    }

    public ApiSimpleResponse getParticipantInfoDetails(String userId){
        JSONObject jsonObject  = new JSONObject();
        ApiSimpleResponse response  = new ApiSimpleResponse();
        try{
            UserDetails participantDetails = getParticipantDetails(userId);
            if(participantDetails != null){
                ProfileBean profileBean = new ProfileBean();
                if(participantDetails.getEmail() != null)
                    profileBean.setEmailId(participantDetails.getEmail());
                response.put(FdahpUserRegUtil.ErrorCodes.PROFILE.getValue(),profileBean);
                SettingsBean settingsBean = new SettingsBean();
                if(participantDetails.getLocalNotificationFlag() != null)
                    settingsBean.setLocalNotifications(participantDetails.getLocalNotificationFlag());
                if(participantDetails.getUsePassCode() != null)
                    settingsBean.setPasscode(participantDetails.getUsePassCode());
                if(participantDetails.getRemoteNotificationFlag()!=null)
                    settingsBean.setRemoteNotifications(participantDetails.getRemoteNotificationFlag());
                if(participantDetails.getTouchId() != null)
                    settingsBean.setTouchId(participantDetails.getTouchId());
                if(participantDetails.getReminderLeadTime() != null && !participantDetails.getReminderLeadTime().isEmpty()){
                    settingsBean.setReminderLeadTime(participantDetails.getReminderLeadTime());
                }else{
                    settingsBean.setReminderLeadTime("");
                }
                if(participantDetails.getLocale() != null)
                    settingsBean.setLocale(participantDetails.getLocale());
                response.put(FdahpUserRegUtil.ErrorCodes.SETTINGS.getValue(),settingsBean);
                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
            }
            /*List<ParticipantStudies> participantStudiesList = getParticipantStudiesList(userId);
            List<ParticipantInfoBean> participantInfoBeanList = new ArrayList<ParticipantInfoBean>();
            if(participantStudiesList != null && participantStudiesList.size() > 0){

                for (ParticipantStudies participantStudies : participantStudiesList){
                    if(participantStudies.getParticipantId() != null)
                    {
                        ParticipantInfoBean participantInfoBean = new ParticipantInfoBean();
                        if(participantStudies.getStudyId() != null)
                            participantInfoBean.setStudyId(participantStudies.getStudyId());
                        if(participantStudies.getParticipantId() != null)
                            participantInfoBean.setParticipantId(participantStudies.getParticipantId());
                        if(participantStudies.getEnrolledDate() != null)
                            participantInfoBean.setEnrolledDate(participantStudies.getEnrolledDate());
                        participantInfoBeanList.add(participantInfoBean);
                    }
                }

            }
            response.put(FdahpUserRegUtil.ErrorCodes.PARTICIPANTINFO.getValue(),participantInfoBeanList);*/
        }catch (Exception e){
            _log.error("FdahpUserRegWSManager getParticipantDetails error:",e);
        }
        return  response;
    }

    public List<ParticipantStudies> getParticipantStudiesList(String userId){
        List<ParticipantStudies> participantStudiesList = null;
        try{
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition(FieldKey.fromParts("UserId"), userId);
            participantStudiesList = new TableSelector(FdahpUserRegWSSchema.getInstance().getParticipantStudies(),filter,null).getArrayList(ParticipantStudies.class);
        }catch (Exception e){
            _log.error("FdahpUserRegWSManager getParticipantStudiesList error:",e);
        }
        return  participantStudiesList;
    }

    public List<ParticipantActivities> getParticipantActivitiesList(String userId){
        List<ParticipantActivities> participantActivitiesList = null;
        try{
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition(FieldKey.fromParts("ParticipantId"), userId);
            participantActivitiesList = new TableSelector(FdahpUserRegWSSchema.getInstance().getParticipantActivities(),filter,null).getArrayList(ParticipantActivities.class);
        }catch (Exception e){
            _log.error("FdahpUserRegWSManager getParticipantActivitiesList error :",e);
        }
        return participantActivitiesList;
    }

    public String saveParticipantStudies(List<ParticipantStudies> participantStudiesList){
        String message = FdahpUserRegUtil.ErrorCodes.FAILURE.getValue();
        DbScope dbScope = FdahpUserRegWSSchema.getInstance().getSchema().getScope();
        DbScope.Transaction transaction = dbScope.ensureTransaction();
        try{
            TableInfo table = FdahpUserRegWSSchema.getInstance().getParticipantStudies();
            table.setAuditBehavior(AuditBehaviorType.DETAILED);
            for(ParticipantStudies participantStudies : participantStudiesList){
                if(participantStudies.getId() != null ){
                    Table.update(null,table,participantStudies,participantStudies.getId());
                }else{
                    Table.insert(null, table, participantStudies);
                }
                addAuditEvent(participantStudies.getUserId(),"Study State Update"," Study state has been updated "+participantStudies.getStudyId()+".","FdaStudyAuditEvent","");
            }

            if(participantStudiesList.size() > 0){
                message = FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue();
            }
        }catch (Exception e){
            _log.error("FdahpUserRegWSManager saveParticipantStudies error :",e);
        }
        transaction.commit();
        return message;
    }

    public String saveParticipantActivities(List<ParticipantActivities> participantActivitiesList){
        String message = FdahpUserRegUtil.ErrorCodes.FAILURE.getValue();
        DbScope dbScope = FdahpUserRegWSSchema.getInstance().getSchema().getScope();
        DbScope.Transaction transaction = dbScope.ensureTransaction();
        try{
            TableInfo table = FdahpUserRegWSSchema.getInstance().getParticipantActivities();
            table.setAuditBehavior(AuditBehaviorType.DETAILED);
            for (ParticipantActivities participantActivities : participantActivitiesList){
                if(participantActivities.getId() != null)
                    Table.update(null,table,participantActivities,participantActivities.getId());
                else
                    Table.insert(null,table,participantActivities);
                addAuditEvent(participantActivities.getParticipantId(),"Activity State Update","Activity state has been updated "+participantActivities.getActivityId()+".","FdaActivityAuditEvent","");
            }


            if (participantActivitiesList.size() > 0){
                message = FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue();
            }
        }catch (Exception e){
            _log.error("FdahpUserRegWSManager saveParticipantActivities error :",e);
        }
        transaction.commit();
        return message;
    }

    public ApiSimpleResponse getPreferences(String userId){
        ApiSimpleResponse response = new ApiSimpleResponse();
        try{
            List<ParticipantStudies> participantStudiesList = getParticipantStudiesList(userId);
            List<StudiesBean> studiesBeenList = new ArrayList<StudiesBean>();
            if(null!=participantStudiesList && participantStudiesList.size() >0){

                for (ParticipantStudies participantStudies : participantStudiesList){
                    //if((participantStudies.getStatus() != null && !participantStudies.getStatus().equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.WITHDRAWN.getValue()))){
                        StudiesBean studiesBean = new StudiesBean();
                        if(participantStudies.getStudyId() != null)
                            studiesBean.setStudyId(participantStudies.getStudyId());
                        if(participantStudies.getBookmark() != null)
                            studiesBean.setBookmarked(participantStudies.getBookmark());
                        if(participantStudies.getStatus() != null){
                            studiesBean.setStatus(participantStudies.getStatus());
                        }else{
                            studiesBean.setStatus("");
                        }


                        if(participantStudies.getEnrolledDate() != null){
                            studiesBean.setEnrolledDate(FdahpUserRegUtil.getFormattedDateTimeZone(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(participantStudies.getEnrolledDate()), "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
                        }else{
                            studiesBean.setEnrolledDate("");
                        }




                        if(participantStudies.getCompletion() != null){
                            studiesBean.setCompletion(participantStudies.getCompletion());
                        }else{
                            studiesBean.setCompletion(0);
                        }
                        if(participantStudies.getAdherence() != null){
                            studiesBean.setAdherence(participantStudies.getAdherence());
                        }else{
                            studiesBean.setAdherence(0);
                        }
                        if(participantStudies.getParticipantId() != null){
                            studiesBean.setParticipantId(participantStudies.getParticipantId());
                        }else{
                            studiesBean.setParticipantId("");
                        }

                        studiesBeenList.add(studiesBean);
                    }
               // }

            }
            response.put(FdahpUserRegUtil.ErrorCodes.STUDIES.getValue(),studiesBeenList);
            /*List<ParticipantActivities> participantActivitiesList = getParticipantActivitiesList(userId);
            List<ActivitiesBean> activitiesBeanList = new ArrayList<ActivitiesBean>();
            if(null!=participantActivitiesList && participantActivitiesList.size() > 0){

                for (ParticipantActivities participantActivities : participantActivitiesList)
                {

                    ActivitiesBean activitiesBean = new ActivitiesBean();
                    if(participantActivities.getStudyId()!=null)
                        activitiesBean.setStudyId(participantActivities.getStudyId());
                    if(participantActivities.getActivityId()!=null)
                        activitiesBean.setActivityId(participantActivities.getActivityId());
                    if(participantActivities.getStatus() != null)
                        activitiesBean.setStatus(participantActivities.getStatus());
                    if(participantActivities.getActivityVersion()!=null)
                        activitiesBean.setActivityVersion(participantActivities.getActivityVersion());
                    if(participantActivities.getBookmark()!=null)
                        activitiesBean.setBookmarked(participantActivities.getBookmark());
                    if(participantActivities.getActivityRunId() != null)
                        activitiesBean.setActivityRunId(participantActivities.getActivityRunId());
                    activitiesBeanList.add(activitiesBean);
                }

            }*/
           // response.put(FdahpUserRegUtil.ErrorCodes.ACTIVITIES.getValue(),activitiesBeanList);
            response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
        }catch (Exception e){
            _log.error("FdahpUserRegWSManager getPreferences error :",e);
        }
        return response;
    }

    public ParticipantStudies getParticipantStudies(String studyId,String userId){
        ParticipantStudies participantStudies = null;
        try{
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition(FieldKey.fromParts("UserId"), userId);
            filter.addCondition(FieldKey.fromParts("StudyId"), studyId);
            participantStudies =  new TableSelector(FdahpUserRegWSSchema.getInstance().getParticipantStudies(),filter,null).getObject(ParticipantStudies.class);
        }catch (Exception e){
            _log.error("FdahpUserRegWSManager getParticipantStudies()",e);
        }
        return  participantStudies;
    }

    public List<ParticipantActivities> getParticipantActivitiesList(String studyId,String userId){
        List<ParticipantActivities> participantActivitiesList = null;
        try{
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition(FieldKey.fromParts("ParticipantId"), userId);
            filter.addCondition(FieldKey.fromParts("StudyId"), studyId);
            participantActivitiesList = new TableSelector(FdahpUserRegWSSchema.getInstance().getParticipantActivities(),filter,null).getArrayList(ParticipantActivities.class);
        }catch (Exception e){
            _log.error("FdahpUserRegWSManager getParticipantActivitiesList()",e);
        }
        return participantActivitiesList;
    }

    public String withDrawStudy(String studyId,String userId,Boolean deleteData){
        String message = FdahpUserRegUtil.ErrorCodes.FAILURE.getValue();
        DbScope dbScope = FdahpUserRegWSSchema.getInstance().getSchema().getScope();
        DbScope.Transaction transaction = dbScope.ensureTransaction();
        try{
            /*TableInfo table = FdahpUserRegWSSchema.getInstance().getParticipantStudies();
            table.setAuditBehavior(AuditBehaviorType.DETAILED);
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition(FieldKey.fromParts("ParticipantId"), userId);
            filter.addCondition(FieldKey.fromParts("StudyId"), studyId);
            int count = Table.delete(table,filter);
            if(count >0)
                message = FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue();*/


            DbSchema schema = FdahpUserRegWSSchema.getInstance().getSchema();
            TableInfo table = FdahpUserRegWSSchema.getInstance().getParticipantStudies();
            table.setAuditBehavior(AuditBehaviorType.DETAILED);

            SqlExecutor executor = new SqlExecutor(schema);
            SQLFragment sqlUpdateVisitDates = new SQLFragment();

            TableInfo participantActivitiesInfo = FdahpUserRegWSSchema.getInstance().getParticipantActivities();
            participantActivitiesInfo.setAuditBehavior(AuditBehaviorType.DETAILED);

            if(deleteData){
                SimpleFilter filterActivities = new SimpleFilter();
                filterActivities.addCondition(FieldKey.fromParts("ParticipantId"), userId);
                filterActivities.addCondition(FieldKey.fromParts("StudyId"), studyId);
                Table.delete(participantActivitiesInfo,filterActivities);

                sqlUpdateVisitDates.append("UPDATE ").append(table.getSelectName()).append("\n")
                        .append("SET Status = 'Withdrawn', ParticipantId = NULL")
                        .append(" WHERE UserId = '"+userId+"'")
                        .append(" and StudyId = '"+studyId+"'");
                int execute = executor.execute(sqlUpdateVisitDates);
                if (execute > 0){
                    message = FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue();
                }
            }else{
                sqlUpdateVisitDates.append("UPDATE ").append(table.getSelectName()).append("\n")
                        .append("SET Status = 'Withdrawn'")
                        .append(" WHERE UserId = '"+userId+"'")
                        .append(" and StudyId = '"+studyId+"'");
                int execute = executor.execute(sqlUpdateVisitDates);
                if (execute > 0){
                    message = FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue();
                }
            }


        }catch (Exception e){
            _log.error("FdahpUserRegWSManager withDrawStudy()",e);
        }
        transaction.commit();
        return message;
    }

    public AuthInfo getAuthInfo(String authKey,String participantId){
        AuthInfo authInfo = null;
        try{
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition(FieldKey.fromParts("AuthKey"), authKey);
            filter.addCondition(FieldKey.fromParts("ParticipantId"), participantId);
            authInfo  = new TableSelector(FdahpUserRegWSSchema.getInstance().getAuthInfo(),filter,null).getObject(AuthInfo.class);
        }catch (Exception e){
            _log.error("FdahpUserRegWSManger getAuthInfo ()",e);
        }
        return authInfo;
    }
    public AuthInfo updateAuthInfo(AuthInfo authInfo){
        DbScope dbScope = FdahpUserRegWSSchema.getInstance().getSchema().getScope();
        DbScope.Transaction transaction = dbScope.ensureTransaction();
        try{
            TableInfo table = FdahpUserRegWSSchema.getInstance().getAuthInfo();
            table.setAuditBehavior(AuditBehaviorType.DETAILED);
            if(null !=authInfo){
              //  authInfo.setDeviceToken(authInfo.getDeviceToken());
              //  authInfo.setDeviceType(authInfo.getDeviceType());
              //  authInfo.setRemoteNotificationFlag(authInfo.getRemoteNotificationFlag());
                authInfo.setModifiedOn(new Date());
                Table.update(null,table, authInfo,authInfo.getAuthId());
            }

        }catch (Exception e){
            _log.error("updateAuthInfo:",e);
        }
        transaction.commit();
        return authInfo;
    }

    public StudyConsent saveStudyConsent(StudyConsent studyConsent){
        DbScope dbScope = FdahpUserRegWSSchema.getInstance().getSchema().getScope();
        DbScope.Transaction transaction = dbScope.ensureTransaction();
        try{
            TableInfo table = FdahpUserRegWSSchema.getInstance().getStudyConsent();
            table.setAuditBehavior(AuditBehaviorType.DETAILED);

            if(null != studyConsent){
                if(studyConsent.getId() != null){
                    Table.update(null,table, studyConsent,studyConsent.getId());
                }else{
                    Table.insert(null,table,studyConsent);
                }
            }

        }catch (Exception e){
            _log.error("saveStudyConsent:",e);
        }
        transaction.commit();
        return studyConsent;
    }

    public StudyConsent getStudyConsent(String userId,String studyId,String consentVersion){
        StudyConsent studyConsent = null;
        try{
            TableInfo studyConsentInfo = FdahpUserRegWSSchema.getInstance().getStudyConsent();
            SQLFragment sql=null;
            if(consentVersion != null && StringUtils.isNotEmpty(consentVersion)){
                sql =  new SQLFragment("SELECT * FROM " + studyConsentInfo.getSelectName() + " WHERE userid ='"+userId+"' and studyid ='"+studyId+"' and version ='"+consentVersion+"'");
            }else{
                sql =  new SQLFragment("SELECT * FROM " + studyConsentInfo.getSelectName() + " WHERE userid ='"+userId+"' and studyid ='"+studyId+"' order by _ts desc limit 1");
            }
            studyConsent = new SqlSelector(FdahpUserRegWSSchema.getInstance().getSchema(), sql).getObject(StudyConsent.class);

        }catch (Exception e){
            _log.error("getStudyConsent Error",e);
        }
        return studyConsent;
    }
    /*public UserDetails getParticipantDetailsByToken(String token){
        UserDetails participantDetails = null;
        try{
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition(FieldKey.fromParts("SecurityToken"), token);
            participantDetails = new TableSelector(FdahpUserRegWSSchema.getInstance().getParticipantDetails(), filter, null).getObject(UserDetails.class);
        }catch (Exception e){
            _log.error("getParticipantDetailsByToken Error",e);
        }
        return participantDetails;
    }*/

    public UserDetails getParticipantDetailsByToken(String emailId,String token){
        UserDetails participantDetails = null;
        try{
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition(FieldKey.fromParts("Email"), emailId);
            filter.addCondition(FieldKey.fromParts("SecurityToken"), token);
            participantDetails = new TableSelector(FdahpUserRegWSSchema.getInstance().getParticipantDetails(), filter, null).getObject(UserDetails.class);
        }catch (Exception e){
            _log.error("getParticipantDetailsByToken Error",e);
        }
        return participantDetails;
    }

    public String deleteAccount(String userId){
        String message = FdahpUserRegUtil.ErrorCodes.FAILURE.getValue();
        DbScope dbScope = FdahpUserRegWSSchema.getInstance().getSchema().getScope();
        DbScope.Transaction transaction = dbScope.ensureTransaction();
        try{
            TableInfo participantActivitiesInfo = FdahpUserRegWSSchema.getInstance().getParticipantActivities();
            participantActivitiesInfo.setAuditBehavior(AuditBehaviorType.DETAILED);
            SimpleFilter filterActivities = new SimpleFilter();
            filterActivities.addCondition(FieldKey.fromParts("ParticipantId"), userId);
            Table.delete(participantActivitiesInfo,filterActivities);

            TableInfo table = FdahpUserRegWSSchema.getInstance().getParticipantStudies();
            table.setAuditBehavior(AuditBehaviorType.DETAILED);
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition(FieldKey.fromParts("UserId"), userId);
            Table.delete(table,filter);

            TableInfo participantInfo = FdahpUserRegWSSchema.getInstance().getParticipantDetails();
            participantInfo.setAuditBehavior(AuditBehaviorType.DETAILED);
            SimpleFilter participantFilter = new SimpleFilter();
            participantFilter.addCondition(FieldKey.fromParts("UserId"), userId);
            int count = Table.delete(participantInfo,participantFilter);

            TableInfo authInfo = FdahpUserRegWSSchema.getInstance().getAuthInfo();
            authInfo.setAuditBehavior(AuditBehaviorType.DETAILED);
            SimpleFilter authInfoFilter = new SimpleFilter();
            authInfoFilter.addCondition(FieldKey.fromParts("ParticipantId"), userId);
            Table.delete(authInfo,authInfoFilter);

            if(count > 0)
                message = FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue();



        }catch (Exception e){
            _log.error("deleteAccount error:",e);
        }
        transaction.commit();
        return message;
    }

    public String deActivate(String userId, DeactivateForm deactivateForm){


        String message = FdahpUserRegUtil.ErrorCodes.FAILURE.getValue();
        DbScope dbScope = FdahpUserRegWSSchema.getInstance().getSchema().getScope();
        DbScope.Transaction transaction = dbScope.ensureTransaction();
        int count = 0;
        try{

            DbSchema schema = FdahpUserRegWSSchema.getInstance().getSchema();

            SqlExecutor executor = new SqlExecutor(schema);
            SQLFragment sqlUpdateVisitDates = new SQLFragment();
            if(userId != null && !userId.isEmpty()){

                if(deactivateForm != null && deactivateForm.getDeleteData() != null && deactivateForm.getDeleteData().size() > 0)
                {

                    TableInfo table = FdahpUserRegWSSchema.getInstance().getParticipantStudies();
                    table.setAuditBehavior(AuditBehaviorType.DETAILED);
                    sqlUpdateVisitDates.append("UPDATE ").append(table.getSelectName()).append("\n")
                            .append("SET Status = 'Withdrawn', ParticipantId = NULL")
                            .append(" WHERE UserId = '" + userId + "'")
                            .append(" and StudyId IN (" + FdahpUserRegUtil.commaSeparatedString(deactivateForm.getDeleteData()) + ")");
                    int execute = executor.execute(sqlUpdateVisitDates);
                }

                TableInfo participantActivitiesInfo = FdahpUserRegWSSchema.getInstance().getParticipantActivities();
                participantActivitiesInfo.setAuditBehavior(AuditBehaviorType.DETAILED);
                SimpleFilter filterActivities = new SimpleFilter();
                filterActivities.addCondition(FieldKey.fromParts("ParticipantId"), userId);
                Table.delete(participantActivitiesInfo,filterActivities);

                TableInfo authInfo = FdahpUserRegWSSchema.getInstance().getAuthInfo();
                authInfo.setAuditBehavior(AuditBehaviorType.DETAILED);
                SimpleFilter authInfoFilter = new SimpleFilter();
                authInfoFilter.addCondition(FieldKey.fromParts("ParticipantId"), userId);
                Table.delete(authInfo,authInfoFilter);

                TableInfo participantInfo = FdahpUserRegWSSchema.getInstance().getParticipantDetails();
                participantInfo.setAuditBehavior(AuditBehaviorType.DETAILED);
                SimpleFilter participantFilter = new SimpleFilter();
                participantFilter.addCondition(FieldKey.fromParts("UserId"), userId);
                count = Table.delete(participantInfo,participantFilter);

                if(count > 0)
                    message = FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue();
            }


        }catch (Exception e){
            _log.error("deActivate error:",e);
        }
        transaction.commit();
        return message;
    }

    public List<PasswordHistory> getPasswordHistoryList(String userId){
        List<PasswordHistory> passwordHistoryList = null;
        try{
            TableInfo passwordHistoryInfo = FdahpUserRegWSSchema.getInstance().getPasswordHistory();
            SQLFragment sql = new SQLFragment("SELECT * FROM " + passwordHistoryInfo.getSelectName() + " WHERE userId ='"+userId+"' ORDER BY created");
            passwordHistoryList = new SqlSelector(FdahpUserRegWSSchema.getInstance().getSchema(), sql).getArrayList(PasswordHistory.class);
        }catch (Exception e){
            _log.error("getPasswordHistoryList error:",e);
        }
        return passwordHistoryList;
    }

    public String savePasswordHistory(String userId,String password){
        Properties configProp = FdahpUserRegUtil.getProperties();
        String message = FdahpUserRegUtil.ErrorCodes.FAILURE.getValue();
        String passwordHistoryCount = (String) configProp.get("password.history.count");
        List<PasswordHistory> passwordHistories = null;
        DbScope dbScope = FdahpUserRegWSSchema.getInstance().getSchema().getScope();
        DbScope.Transaction transaction = dbScope.ensureTransaction();
        try{
            passwordHistories = getPasswordHistoryList(userId);

            TableInfo table = FdahpUserRegWSSchema.getInstance().getPasswordHistory();
            table.setAuditBehavior(AuditBehaviorType.DETAILED);

            if(passwordHistories != null && passwordHistories.size() > (Integer.parseInt(passwordHistoryCount) - 1)){
                for (int i=0;i<((passwordHistories.size() - Integer.parseInt(passwordHistoryCount)) + 1);i++){
                    SimpleFilter filter = new SimpleFilter();
                    filter.addCondition(FieldKey.fromParts("Id"), passwordHistories.get(i).getId());
                    Table.delete(table,filter);
                }
            }

            PasswordHistory passwordHistory = new PasswordHistory();
            passwordHistory.setUserId(userId);
            passwordHistory.setPassword(password);
            message = FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue();
            Table.insert(null,table,passwordHistory);


        }catch (Exception e){
            _log.error("getPasswordHistoryList error:",e);
        }
        transaction.commit();
        return message;
    }

    public Map<String,JSONArray> getDeviceTokenOfAllUsers(){
        String deviceTokens = null;
        List<AuthInfo> authInfoList = new ArrayList<>();
        List<String> deviceTokenList = new ArrayList<>();
        String[] deviceTokenArr = null;
        JSONArray jsonArray = null;
        JSONArray iosJsonArray = null;
        Map<String,JSONArray> deviceMap = new HashMap<>();
        try{
            TableInfo authTableInfo = FdahpUserRegWSSchema.getInstance().getAuthInfo();
            SQLFragment sql=null;
            sql =  new SQLFragment("SELECT * FROM " + authTableInfo.getSelectName() + " WHERE authkey != '0' and remotenotificationflag=true");
            authInfoList = new SqlSelector(FdahpUserRegWSSchema.getInstance().getSchema(), sql).getArrayList(AuthInfo.class);
            if(authInfoList != null && !authInfoList.isEmpty()){
                jsonArray = new JSONArray();
                iosJsonArray = new JSONArray();
                for (AuthInfo authInfo : authInfoList){
                    if(authInfo.getDeviceToken() != null && authInfo.getDeviceType() != null){
                        if(authInfo.getDeviceType().equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.DEVICE_ANDROID.getValue())){
                            jsonArray.put(authInfo.getDeviceToken());
                        }else  if(authInfo.getDeviceType().equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.DEVICE_IOS.getValue())){
                            iosJsonArray.put(authInfo.getDeviceToken());
                        }
                        System.out.println(authInfo.getDeviceToken());
                    }
                }
                deviceMap.put(FdahpUserRegUtil.ErrorCodes.DEVICE_ANDROID.getValue(),jsonArray);
                deviceMap.put(FdahpUserRegUtil.ErrorCodes.DEVICE_IOS.getValue(),iosJsonArray);


            }
        }catch (Exception e){
            _log.error("getDeviceTokenOfAllUsers error:",e);
        }
        return deviceMap;
    }

    public  Map<String,Map<String,JSONArray>> getStudyLevelDeviceToken(String studyIds){
        //Map<String,JSONArray> studyDeviceTokenMap = new HashMap<>();
        Map<String,Map<String,JSONArray>> studyDeviceTokenMap = new HashMap<>();
        try{

            SQLFragment sql = new SQLFragment();
            sql.append("SELECT sp.studyid, string_agg(a.devicetoken, ',') as devicetoken,string_agg(a.devicetype, ',') as devicetype FROM ").append(FdahpUserRegWSSchema.getInstance().getParticipantStudies(), "sp").append(" , ").append(FdahpUserRegWSSchema.getInstance().getAuthInfo(), "a").append(" where sp.userid = a.participantid and sp.status not in('yetToJoin','withdrawn','notEligible') and a.authkey != '0' and a.remotenotificationflag=true and sp.studyid in ("+studyIds+") GROUP BY sp.studyid");
            ResultSet rs = new SqlSelector(FdahpUserRegWSSchema.getInstance().getSchema(), sql).getResultSet();
            if(rs != null)
            {
                while (rs.next())
                {
                    String studyid = rs.getString(1);
                    String deviceToken = rs.getString(2);
                    String deviceType = rs.getString(3);
                    if (deviceToken != null)
                    {
                        String[] deviceTokens = deviceToken.split(",");
                        String[] deviceTypes = deviceType.split(",");
                        _log.info("deviceTokens length:" + deviceTokens.length);
                        _log.info("deviceTypes length:" + deviceTypes.length);
                        if (((deviceTokens != null && deviceTokens.length > 0) && (deviceType != null && deviceTypes.length > 0)) && (deviceTokens.length == deviceTypes.length))
                        {

                            JSONArray jsonArray = new JSONArray();
                            JSONArray iosJsonArray = new JSONArray();
                            Map<String, JSONArray> deviceMap = new HashMap<>();
                            for (int i = 0; i < deviceTokens.length; i++)
                            {
                                if (deviceTypes[i] != null && deviceTypes[i].equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.DEVICE_ANDROID.getValue()))
                                {
                                    jsonArray.put(deviceTokens[i]);
                                }
                                else if (deviceTypes[i] != null && deviceTypes[i].equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.DEVICE_IOS.getValue()))
                                {
                                    iosJsonArray.put(deviceTokens[i]);
                                }

                            }
                            deviceMap.put(FdahpUserRegUtil.ErrorCodes.DEVICE_ANDROID.getValue(), jsonArray);
                            deviceMap.put(FdahpUserRegUtil.ErrorCodes.DEVICE_IOS.getValue(), iosJsonArray);

                            studyDeviceTokenMap.put(studyid, deviceMap);
                        }
                    }
                }
                rs.close();
            }
        }catch (Exception e){
            _log.error("getStudyLevelDeviceToken error:",e);
        }
        return studyDeviceTokenMap;
    }

   public static void addAuditEvent(String userId,String activity,String activityDetails,String eventType,String container){
        FdaAuditProvider.FdaAuditEvent event = new FdaAuditProvider.FdaAuditEvent(eventType,container,activityDetails);
        try{
            event.setActivity(activity);
            event.setActivityDetails(activityDetails);
            event.setUserId(userId);



            AuditLogService.get().addEvent(null, event);
        }catch (Exception e){
            _log.error("addAuditEvent error:",e);
        }
   }
   public LoginAttempts getLoginAttempts(String email){
       LoginAttempts loginAttempts = null;
       try{
           SimpleFilter filter = new SimpleFilter();
           filter.addCondition(FieldKey.fromParts("Email"), email);
           loginAttempts = new TableSelector(FdahpUserRegWSSchema.getInstance().getLoginAttempts(),filter,null).getObject(LoginAttempts.class);
       }catch (Exception e ){
           _log.error("FdahpUserRegWSManger getLoginAttempts ()",e);
       }
       return loginAttempts;
   }
   public void resetLoginAttempts(String email){
       try{
           TableInfo table = FdahpUserRegWSSchema.getInstance().getLoginAttempts();
           table.setAuditBehavior(AuditBehaviorType.DETAILED);
           SimpleFilter filter = new SimpleFilter();
           filter.addCondition(FieldKey.fromParts("Email"), email);
           Table.delete(table,filter);
       }catch (Exception e)
       {
           _log.error("FdahpUserRegWSManger resetLoginAttempts ()", e);
       }
   }
   public LoginAttempts updateLoginFailureAttempts(String email){
       LoginAttempts loginAttempts = null;
       int count = 0;
       try{
           loginAttempts = getLoginAttempts(email);
           TableInfo table = FdahpUserRegWSSchema.getInstance().getLoginAttempts();
           table.setAuditBehavior(AuditBehaviorType.DETAILED);
           if(loginAttempts != null){
               if(loginAttempts.getAttempts() > 0){
                   count = loginAttempts.getAttempts();
               }
               count++;
               loginAttempts.setAttempts(count);
               loginAttempts.setLastModified(FdahpUserRegUtil.getCurrentUtilDateTime());
               loginAttempts = Table.update(null,table, loginAttempts,loginAttempts.getId());
           }else{
               loginAttempts = new LoginAttempts();
               count++;
               loginAttempts.setAttempts(count);
               loginAttempts.setEmail(email);
               loginAttempts.setLastModified(FdahpUserRegUtil.getCurrentUtilDateTime());
               loginAttempts = Table.insert(null,table,loginAttempts);
           }
       }catch (Exception e)
       {
           _log.error("FdahpUserRegWSManger updateLoginFailureAttempts ()", e);
       }
       return loginAttempts;
   }

    public List<StudyConsent> getStudyConsentList(){
        List<StudyConsent> studyConsent = null;
        try{
            TableInfo studyConsentInfo = FdahpUserRegWSSchema.getInstance().getStudyConsent();
            SQLFragment sql=null;
            sql =  new SQLFragment("SELECT * FROM " + studyConsentInfo.getSelectName() + " WHERE pdf notnull");
            studyConsent = new SqlSelector(FdahpUserRegWSSchema.getInstance().getSchema(), sql).getArrayList(StudyConsent.class);

        }catch (Exception e){
            _log.error("getStudyConsent Error",e);
        }
        return studyConsent;
    }
}



