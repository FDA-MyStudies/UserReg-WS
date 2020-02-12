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