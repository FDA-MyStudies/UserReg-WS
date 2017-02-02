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

-- Create schema, tables, indexes, and constraints used for FdahpUserRegWS module here
-- All SQL VIEW definitions should be created in fdahpuserregws-create.sql and dropped in fdahpuserregws-drop.sql
CREATE SCHEMA fdahpuserregws;
GO



CREATE TABLE fdahpuserregws.AuthInfo
(
    -- standard fields
    _ts TIMESTAMP,
    AuthId INT IDENTITY(1,1) NOT NULL,
	ParticipantId INT NULL,
	DeviceToken NVARCHAR(1000) NULL,
	DeviceType CHAR(1) NULL,
	CreatedOn DATETIME NULL,
	ModifiedOn DATETIME NULL,
	AuthKey NVARCHAR(50) NULL,
    CONSTRAINT PK_AuthInfo PRIMARY KEY (AuthId)

);

CREATE TABLE fdahpuserregws.ParticipantDetails
(
    -- standard fields
    _ts TIMESTAMP,
    Id INT IDENTITY(1,1) NOT NULL,
	FirstName NVARCHAR(100) NULL,
    LastName NVARCHAR(100) NULL,
	Email NVARCHAR(100) NULL,
	UsePasscode TINYINT NULL,
	TouchId TINYINT NULL,
	LocalNotificationFlag TINYINT NULL,
	RemoteNotificationFlag TINYINT NULL,
	ReminderFlag TINYINT NULL,
	Status INT NULL,
    CONSTRAINT PK_ParticipantDetails PRIMARY KEY (Id)
	
  
);
CREATE TABLE fdahpuserregws.ParticipantStudies
(
    -- standard fields
    _ts TIMESTAMP,
    Id INT IDENTITY(1,1) NOT NULL,
	ParticipantId INT NULL,
    StudyId INT NULL,
    Status TINYINT NULL ,
    Bookmark TINYINT NULL,
	CONSTRAINT PK_ParticipantStudies PRIMARY KEY (Id)
);
CREATE TABLE fdahpuserregws.ParticipantActivities
(
    -- standard fields
    _ts TIMESTAMP,
    Id INT IDENTITY(1,1) NOT NULL,
	ParticipantId INT NULL,
    StudyId INT NULL,
    ActivityId INT NULL ,
    ActivityCompleteId INT NULL,
    ActivityType NVARCHAR(50) NULL ,
	CONSTRAINT PK_ParticipantActivities PRIMARY KEY (Id)
);