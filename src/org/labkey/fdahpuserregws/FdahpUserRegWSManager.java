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
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;

import org.json.JSONObject;
import org.labkey.api.action.ApiSimpleResponse;

import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.gwt.client.AuditBehaviorType;
import org.labkey.api.query.FieldKey;
import org.labkey.fdahpuserregws.bean.ActivitiesBean;

import org.labkey.fdahpuserregws.bean.ParticipantForm;
import org.labkey.fdahpuserregws.bean.ParticipantInfoBean;
import org.labkey.fdahpuserregws.bean.ProfileBean;
import org.labkey.fdahpuserregws.bean.SettingsBean;
import org.labkey.fdahpuserregws.bean.StudiesBean;
import org.labkey.fdahpuserregws.model.AuthInfo;
import org.labkey.fdahpuserregws.model.FdahpUserRegUtil;
import org.labkey.fdahpuserregws.model.ParticipantActivities;
import org.labkey.fdahpuserregws.model.StudyConsent;
import org.labkey.fdahpuserregws.model.UserDetails;
import org.labkey.fdahpuserregws.model.ParticipantStudies;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FdahpUserRegWSManager
{
    private static final FdahpUserRegWSManager _instance = new FdahpUserRegWSManager();

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
                Table.update(null,table, authInfo,authInfo.getAuthId());
            }else{
                authInfo = new AuthInfo();
                authInfo.setAuthKey(authKey);
                authInfo.setDeviceToken("");
                authInfo.setDeviceType("");
                authInfo.setParticipantId(userId);
                authInfo.setCreatedOn(new Date());
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
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition(FieldKey.fromParts("AuthKey"), authKey);
            authInfo  = new TableSelector(FdahpUserRegWSSchema.getInstance().getAuthInfo(),filter,null).getObject(AuthInfo.class);
            if(authInfo != null){
                isAuthenticated = true;
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
                System.out.println("participantDetails.getUserId():"+participantDetails.getUserId());
                participantForm.setUserId(participantDetails.getUserId());
                participantForm.setFirstName(participantDetails.getFirstName());
                participantForm.setStatus(participantDetails.getStatus());
                participantForm.setLastName(participantDetails.getLastName());
                participantForm.setEmailId(participantDetails.getEmail());
            }
        }catch (Exception e){
            _log.error("HealthStudiesGatewayManager signingParticipant()",e);
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
            _log.error("HealthStudiesGatewayManager getParticipantDetailsByEmail()",e);
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
                    .append("SET AuthKey = 0, ModifiedOn='"+FdahpUserRegUtil.getCurrentDateTime()+"'")
                    .append(" WHERE ParticipantId = '"+userId+"'");
            int execute = executor.execute(sqlUpdateVisitDates);
            if (execute > 0){
                message = FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue();
            }
        }catch (Exception e){
            _log.error("HealthStudiesGatewayManager signout error:",e);
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
                if(participantDetails.getFirstName()!=null)
                    profileBean.setFirstName(participantDetails.getFirstName());
                if(participantDetails.getLastName() != null)
                    profileBean.setLastName(participantDetails.getLastName());
                if(participantDetails.getEmail() != null)
                    profileBean.setEmailId(participantDetails.getEmail());
                response.put(FdahpUserRegUtil.ErrorCodes.PROFILE.getValue(),profileBean);
                SettingsBean settingsBean = new SettingsBean();
                if(participantDetails.getLocalNotificationFlag() != null)
                    settingsBean.setLocalNotifications(participantDetails.getLocalNotificationFlag());
                if(participantDetails.getUsePassCode())
                    settingsBean.setPasscode(participantDetails.getUsePassCode());
                if(participantDetails.getRemoteNotificationFlag()!=null)
                    settingsBean.setRemoteNotifications(participantDetails.getRemoteNotificationFlag());
                if(participantDetails.getTouchId() != null)
                    settingsBean.setTouchId(participantDetails.getTouchId());
                if(participantDetails.getReminderTime() != null)
                    settingsBean.setRemindersTime(participantDetails.getReminderTime());
                response.put(FdahpUserRegUtil.ErrorCodes.SETTINGS.getValue(),settingsBean);
                response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
            }
            List<ParticipantStudies> participantStudiesList = getParticipantStudiesList(userId);
            if(participantStudiesList != null && participantStudiesList.size() > 0){
                List<ParticipantInfoBean> participantInfoBeanList = new ArrayList<ParticipantInfoBean>();
                for (ParticipantStudies participantStudies : participantStudiesList){
                    if(participantStudies.getAppToken() != null)
                    {
                        ParticipantInfoBean participantInfoBean = new ParticipantInfoBean();
                        if(participantStudies.getParticipantId() != null)
                            participantInfoBean.setParticipantId(participantStudies.getParticipantId());
                        if(participantStudies.getStudyId() != null)
                            participantInfoBean.setStudyId(participantStudies.getStudyId());
                        if(participantStudies.getAppToken() != null)
                            participantInfoBean.setAppToken(participantStudies.getAppToken());
                        participantInfoBeanList.add(participantInfoBean);
                    }
                }
                response.put(FdahpUserRegUtil.ErrorCodes.PARTICIPANTINFO.getValue(),participantInfoBeanList);
            }
        }catch (Exception e){
            _log.error("HealthStudiesGatewayManager getParticipantDetails error:",e);
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
            _log.error("HealthStudiesGatewayManager getParticipantStudiesList error:",e);
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
            _log.error("HealthStudiesGatewayManager getParticipantActivitiesList error :",e);
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
            }

            if(participantStudiesList.size() > 0){
                message = FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue();
            }
        }catch (Exception e){
            _log.error("HealthStudiesGatewayManager saveParticipantStudies error :",e);
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
            for (ParticipantActivities participantActivities : participantActivitiesList)
                if(participantActivities.getId() != null)
                    Table.update(null,table,participantActivities,participantActivities.getId());
                else
                    Table.insert(null,table,participantActivities);
            if (participantActivitiesList.size() > 0){
                message = FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue();
            }
        }catch (Exception e){
            _log.error("HealthStudiesGatewayManager saveParticipantActivities error :",e);
        }
        transaction.commit();
        return message;
    }

    public ApiSimpleResponse getPreferences(String userId){
        ApiSimpleResponse response = new ApiSimpleResponse();
        try{
            List<ParticipantStudies> participantStudiesList = getParticipantStudiesList(userId);
            if(null!=participantStudiesList && participantStudiesList.size() >0){
                List<StudiesBean> studiesBeenList = new ArrayList<StudiesBean>();
                for (ParticipantStudies participantStudies : participantStudiesList){
                    if(participantStudies.getAppToken() == null && !participantStudies.getStatus().equalsIgnoreCase(FdahpUserRegUtil.ErrorCodes.WITHDRAWN.getValue())){
                        StudiesBean studiesBean = new StudiesBean();
                        if(participantStudies.getStudyId() != null)
                            studiesBean.setStudyId(participantStudies.getStudyId());
                        if(participantStudies.getBookmark() != null)
                            studiesBean.setBookmarked(participantStudies.getBookmark());
                        if(participantStudies.getStatus() != null)
                            studiesBean.setStatus(participantStudies.getStatus());
                        if(participantStudies.getEnrolledDate() != null)
                            studiesBean.setEnrolledDate(participantStudies.getEnrolledDate());
                        studiesBeenList.add(studiesBean);
                    }
                }
                response.put(FdahpUserRegUtil.ErrorCodes.STUDIES.getValue(),studiesBeenList);
            }
            List<ParticipantActivities> participantActivitiesList = getParticipantActivitiesList(userId);
            if(null!=participantActivitiesList && participantActivitiesList.size() > 0){
                List<ActivitiesBean> activitiesBeanList = new ArrayList<ActivitiesBean>();
                for (ParticipantActivities participantActivities : participantActivitiesList)
                {

                    ActivitiesBean activitiesBean = new ActivitiesBean();
                    if(participantActivities.getStudyId()!=null)
                        activitiesBean.setStudyId(participantActivities.getStudyId());
                    if(participantActivities.getActivityId()!=null)
                        activitiesBean.setActivityId(String.valueOf(participantActivities.getActivityId()));
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
                response.put(FdahpUserRegUtil.ErrorCodes.ACTIVITIES.getValue(),activitiesBeanList);
            }
            response.put(FdahpUserRegUtil.ErrorCodes.MESSAGE.getValue(),FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue().toLowerCase());
        }catch (Exception e){
            _log.error("HealthStudiesGatewayManager getPreferences error :",e);
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
            _log.error("HealthStudiesGatewayManager getParticipantStudies()",e);
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
            _log.error("HealthStudiesGatewayManager getParticipantActivitiesList()",e);
        }
        return participantActivitiesList;
    }

    public String withDrawStudy(String studyId,Integer userId){
        String message = FdahpUserRegUtil.ErrorCodes.FAILURE.getValue();
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

            sqlUpdateVisitDates.append("UPDATE ").append(table.getSelectName()).append("\n")
                    .append("SET Status = 'Withdrawn', AppToken = NULL")
                    .append(" WHERE UserId = '"+userId+"'")
                    .append(" and StudyId = '"+studyId+"'");
            int execute = executor.execute(sqlUpdateVisitDates);
            if (execute > 0){
                message = FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue();
            }

        }catch (Exception e){
            _log.error("HealthStudiesGatewayManager withDrawStudy()",e);
        }
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
                authInfo.setDeviceToken(authInfo.getDeviceToken());
                authInfo.setDeviceType(authInfo.getDeviceType());
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
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition(FieldKey.fromParts("UserId"), userId);
            filter.addCondition(FieldKey.fromParts("StudyId"), studyId);
            filter.addCondition(FieldKey.fromParts("Version"), consentVersion);
            System.out.println("userId:"+userId);
            System.out.println("StudyId:"+studyId);
            System.out.println("Version:"+consentVersion);
            studyConsent = new TableSelector(FdahpUserRegWSSchema.getInstance().getStudyConsent(), filter, null).getObject(StudyConsent.class);
            System.out.println(studyConsent);
        }catch (Exception e){
            _log.error("getStudyConsent Error",e);
        }
        return studyConsent;
    }
    public UserDetails getParticipantDetailsByToken(String token){
        UserDetails participantDetails = null;
        try{
            SimpleFilter filter = new SimpleFilter();
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

            if(count > 0)
                message = FdahpUserRegUtil.ErrorCodes.SUCCESS.getValue();

            transaction.commit();

        }catch (Exception e){
            _log.error("deleteAccount error:",e);
        }
        return message;
    }
}



