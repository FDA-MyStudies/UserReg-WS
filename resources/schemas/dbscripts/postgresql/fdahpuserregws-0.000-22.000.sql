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

create schema fdahpUserRegWS;

create table fdahpUserRegWS.AuthInfo
(
    _ts timestamp not null,
    AuthId serial ,
    ParticipantId  varchar(50) null,
    DeviceToken  varchar(1000) null,
    DeviceType  varchar(50) null,
    CreatedOn timestamp null,
    ModifiedOn timestamp null,
    AuthKey  varchar(50) null,
    IosAppVersion  varchar(50) null,
    AndroidAppVersion  varchar(50) null,

    constraint PK_AuthInfo primary key (AuthId)
);

ALTER TABLE fdahpUserRegWS.AuthInfo ADD SessionExpiredDate TIMESTAMP without time zone;
UPDATE fdahpUserRegWS.AuthInfo SET SessionExpiredDate = now();

ALTER TABLE fdahpUserRegWS.AuthInfo ADD RemoteNotificationFlag BOOLEAN NULL;
UPDATE fdahpUserRegWS.AuthInfo SET RemoteNotificationFlag = true;

ALTER TABLE fdahpUserRegWS.AuthInfo ADD RefreshToken VARCHAR(255) NULL;
UPDATE fdahpUserRegWS.AuthInfo SET RefreshToken = ParticipantId;

create table fdahpUserRegWS.ParticipantActivities
(
    _ts timestamp not null,
    Id serial,
    ParticipantId varchar(50) null,
    StudyId varchar(50) null,
    ActivityId varchar(50) null,
    ActivityCompleteId integer null,
    ActivityType  varchar(1000) null,
    Bookmark  boolean  null,
    Status  varchar(50) null,
    ActivityVersion  varchar(50) null,
    ActivityState  varchar(50) null,
    ActivityRunId varchar(50) null,

    constraint PK_ParticipantActivities primary key (Id)
);

ALTER TABLE fdahpUserRegWS.participantactivities ADD Total integer;
ALTER TABLE fdahpUserRegWS.participantactivities ADD Completed integer;
ALTER TABLE fdahpUserRegWS.participantactivities ADD Missed integer;

create table fdahpUserRegWS.UserDetails
(
    _ts timestamp not null,
    Id serial ,
    FirstName  varchar(100) null,
    LastName  varchar(100) null,
    Email  varchar(100) null,
    UsePasscode  boolean null,
    TouchId boolean null,
    LocalNotificationFlag boolean null,
    RemoteNotificationFlag boolean null,
    Status int null,
    Password varchar(100) null,
    EntityId ENTITYID not null,
    ReminderLeadTime varchar(50) null,
    SecurityToken varchar(100) null,
    UserId varchar(50) null,
    TempPassword boolean null,
    Locale varchar(100) null,
    ResetPassword varchar(100) null,
    VerificationDate timestamp without time zone,
    TempPasswordDate timestamp without time zone,

    constraint PK_UserDetails primary key (Id)
);

ALTER TABLE fdahpUserRegWS.UserDetails ADD PasswordUpdatedDate TIMESTAMP without time zone;
UPDATE fdahpUserRegWS.UserDetails SET PasswordUpdatedDate = _ts;

create table fdahpUserRegWS.ParticipantStudies
(
    _ts timestamp not null,
    Id serial,
    StudyId varchar(50) null,
    ConsentStatus  boolean  null,
    Status  varchar(50) null,
    Bookmark  boolean  null,
    Eligbibility  boolean  null,
    ParticipantId  varchar(50) null,
    UserId varchar(50) null,
    EnrolledDate varchar(50) null,
    Sharing text null,

    constraint PK_ParticipantStudies primary key (Id)
);

ALTER TABLE fdahpUserRegWS.participantstudies ADD Completion integer;
ALTER TABLE fdahpUserRegWS.participantstudies ADD Adherence integer;

-- Create a temporary TIMESTAMP column
ALTER TABLE fdahpUserRegWS.participantstudies ADD COLUMN EnrolledDateTemp TIMESTAMP without time zone NULL;

-- Copy casted value over to the temporary column
UPDATE fdahpUserRegWS.participantstudies SET EnrolledDateTemp = EnrolledDate::TIMESTAMP;

-- Modify original column using the temporary column
ALTER TABLE fdahpUserRegWS.participantstudies ALTER COLUMN EnrolledDate TYPE TIMESTAMP without time zone USING EnrolledDateTemp;

-- Drop the temporary column (after examining altered column values)
ALTER TABLE fdahpUserRegWS.participantstudies DROP COLUMN EnrolledDateTemp;

create table fdahpUserRegWS.StudyConsent
(
    _ts timestamp not null,
    Id serial,
    UserId varchar(50) null,
    StudyId varchar(50) null,
    Version  varchar(50) null,
    Status  varchar(50) null,
    Pdf  text null,

    constraint PK_StudyConsent primary key (Id)
);

ALTER TABLE fdahpUserRegWS.StudyConsent ADD PdfPath VARCHAR(255) NULL;

create table fdahpUserRegWS.PasswordHistory
(
    _ts timestamp not null,
    Id serial,
    UserId  varchar(50) null,
    Password varchar(50) null,
    Created timestamp without time zone,

    constraint PK_PasswordHistory primary key (Id)
);

CREATE TABLE fdahpUserRegWS.LoginAttempts
(
    _ts TIMESTAMP NOT NULL,
    Id SERIAL ,
    Email  VARCHAR(100) NULL,
    LastModified TIMESTAMP WITHOUT TIME ZONE,
    Attempts integer,

    CONSTRAINT PK_LoginAttempts PRIMARY KEY (Id)
);

create table fdahpUserRegWS.VersionInfo
(
    Id serial,
    AndroidVersion  varchar(50) null,
    IosVersion varchar(50) null,
    constraint PK_VersionInfo primary key (Id)

);

create table fdahpUserRegWS.UserAppDetails
(
   UserAppId serial,
   UserId  varchar(50) null,
   ApplicationId varchar(50) null,
   OrgId varchar(50) null,
   CreatedOn timestamp null,
   constraint PK_UserAppDetails primary key (UserAppId)

);

ALTER TABLE fdahpuserregws.authinfo ADD COLUMN applicationId VARCHAR(50) NULL DEFAULT NULL;

ALTER TABLE fdahpuserregws.authinfo ADD COLUMN orgId VARCHAR(50) NULL DEFAULT NULL;

ALTER TABLE fdahpuserregws.participantactivities ADD COLUMN applicationId VARCHAR(50) NULL DEFAULT NULL;

ALTER TABLE fdahpuserregws.participantactivities ADD COLUMN orgId VARCHAR(50) NULL DEFAULT NULL;

ALTER TABLE fdahpuserregws.participantstudies ADD COLUMN applicationId VARCHAR(50) NULL DEFAULT NULL;

ALTER TABLE fdahpuserregws.participantstudies ADD COLUMN orgId VARCHAR(50) NULL DEFAULT NULL;

ALTER TABLE fdahpuserregws.studyconsent ADD COLUMN applicationId VARCHAR(50) NULL DEFAULT NULL;

ALTER TABLE fdahpuserregws.studyconsent ADD COLUMN orgId VARCHAR(50) NULL DEFAULT NULL;




 create table fdahpUserRegWS.AppPropertiesDetails
(
   Id serial,
   AppId varchar(50) null,
   OrgId varchar(50) null,
   IosBundleId varchar(50) null,
   AndroidBundleId varchar(50) null,
   IosCertificate varchar(10000) null,
   IosCertificatePassword varchar(50) null,
   AndroidServerKey varchar(50) null,
   CreatedOn timestamp null,
   constraint PK_AppPropertiesDetails primary key (Id)

);

 alter TABLE fdahpUserRegWS.UserDetails ADD ApplicationId varchar(255);
 update fdahpUserRegWS.UserDetails set ApplicationId = null;


 alter TABLE fdahpUserRegWS.UserDetails ADD OrgId varchar(255);
 update fdahpUserRegWS.UserDetails set OrgId = null;

--alter TABLE fdahpUserRegWS.AppPropertiesDetails ALTER COLUMN IosCertificate TYPE varchar(10000);

alter TABLE fdahpUserRegWS.AppPropertiesDetails ALTER COLUMN AndroidServerKey TYPE varchar(500);

ALTER TABLE fdahpuserregws.AppPropertiesDetails ADD COLUMN email VARCHAR(50) NULL DEFAULT NULL;

ALTER TABLE fdahpuserregws.AppPropertiesDetails ADD COLUMN emailPassword VARCHAR(50) NULL DEFAULT NULL;

ALTER TABLE fdahpuserregws.AppPropertiesDetails ADD COLUMN regEmailSub VARCHAR(255) NULL DEFAULT NULL;

ALTER TABLE fdahpuserregws.AppPropertiesDetails ADD COLUMN regEmailBody VARCHAR(10000) NULL DEFAULT NULL;

ALTER TABLE fdahpuserregws.AppPropertiesDetails ADD COLUMN forgotEmailSub VARCHAR(255) NULL DEFAULT NULL;

ALTER TABLE fdahpuserregws.AppPropertiesDetails ADD COLUMN forgotEmailBody VARCHAR(10000) NULL DEFAULT NULL;

--ALTER TABLE fdahpUserRegWS.AppPropertiesDetails ADD COLUMN Container ENTITYID not null;

ALTER TABLE fdahpuserregws.AppPropertiesDetails ADD COLUMN MethodHandler boolean default False;

Delete from fdahpUserRegWS.AppPropertiesDetails;
Delete from fdahpUserRegWS.authInfo;
Delete from fdahpUserRegWS.loginAttempts;
Delete from fdahpUserRegWS.ParticipantActivities;
Delete from fdahpUserRegWS.ParticipantStudies;
Delete from fdahpUserRegWS.passwordHistory;
Delete from fdahpUserRegWS.studyConsent;
Delete from fdahpUserRegWS.userAppDetails;
Delete from fdahpUserRegWS.userDetails;
Delete from fdahpUserRegWS.versionInfo;


ALTER TABLE fdahpUserRegWS.AppPropertiesDetails ADD COLUMN Container ENTITYID default null;
ALTER TABLE fdahpUserRegWS.authInfo ADD COLUMN Container ENTITYID default null;
ALTER TABLE fdahpUserRegWS.loginAttempts ADD COLUMN Container ENTITYID default null;
ALTER TABLE fdahpUserRegWS.ParticipantActivities ADD COLUMN Container ENTITYID default null;
ALTER TABLE fdahpUserRegWS.ParticipantStudies ADD COLUMN Container ENTITYID default null;
ALTER TABLE fdahpUserRegWS.passwordHistory ADD COLUMN Container ENTITYID default null;
ALTER TABLE fdahpUserRegWS.studyConsent ADD COLUMN Container ENTITYID default null;
ALTER TABLE fdahpUserRegWS.userAppDetails ADD COLUMN Container ENTITYID default null;
ALTER TABLE fdahpUserRegWS.userDetails ADD COLUMN Container ENTITYID default null;
ALTER TABLE fdahpUserRegWS.versionInfo ADD COLUMN Container ENTITYID default null;

alter table fdahpUserRegWS.ParticipantActivities add ActivityStartDate VARCHAR(255) NULL;
alter table fdahpUserRegWS.ParticipantActivities add ActivityEndDate VARCHAR(255) NULL;
alter table fdahpUserRegWS.ParticipantActivities add AnchorDateVersion VARCHAR(255) NULL;
alter table fdahpUserRegWS.ParticipantActivities add AnchorDatecreatedDate VARCHAR(255) NULL;
alter table fdahpUserRegWS.ParticipantActivities add LastModifiedDate VARCHAR(255) NULL;

create table fdahpUserRegWS.CustomScheduleRuns
(
    Id serial,
   ActivityId  varchar(50) null,
   StudyId varchar(50) null,
   RunStartDate varchar(50) null,
   RunEndDate varchar(50) null,
   applicationId VARCHAR(255) NULL,
   orgId VARCHAR(255) NULL,
   constraint PK_CustomScheduleRuns primary key (Id)
);

ALTER TABLE fdahpUserRegWS.CustomScheduleRuns ADD COLUMN Container ENTITYID default null;

alter table fdahpUserRegWS.AppPropertiesDetails add FeedbackEmail VARCHAR(255) NULL;
alter table fdahpUserRegWS.AppPropertiesDetails add ContactUsEmail VARCHAR(255) NULL;
alter table fdahpUserRegWS.AppPropertiesDetails add AppName VARCHAR(255) NULL;

/* 21.xxx SQL scripts */

ALTER TABLE fdahpuserregws.AppPropertiesDetails ADD COLUMN IF NOT EXISTS regEmailSubSpanish VARCHAR(255) NULL DEFAULT NULL;

ALTER TABLE fdahpuserregws.AppPropertiesDetails ADD COLUMN IF NOT EXISTS regEmailBodySpanish VARCHAR(10000) NULL DEFAULT NULL;

ALTER TABLE fdahpuserregws.AppPropertiesDetails ADD COLUMN IF NOT EXISTS forgotEmailSubSpanish VARCHAR(255) NULL DEFAULT NULL;

ALTER TABLE fdahpuserregws.AppPropertiesDetails ADD COLUMN IF NOT EXISTS forgotEmailBodySpanish VARCHAR(10000) NULL DEFAULT NULL;