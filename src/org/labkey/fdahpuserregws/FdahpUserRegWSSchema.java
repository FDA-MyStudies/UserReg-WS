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

import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.dialect.SqlDialect;

public class FdahpUserRegWSSchema
{
    private static final FdahpUserRegWSSchema _instance = new FdahpUserRegWSSchema();
    public static final String NAME = "healthstudiesgateway";
    //public static final String NAME = "fdahpUserRegWS";
    public static FdahpUserRegWSSchema getInstance()
    {
        return _instance;
    }

    private FdahpUserRegWSSchema()
    {
        // private constructor to prevent instantiation from
        // outside this class: this singleton should only be
        // accessed via org.labkey.fdahpuserregws.FdahpUserRegWSSchema.getInstance()
    }

    public DbSchema getSchema()
    {
        return DbSchema.get(NAME, DbSchemaType.Module);
    }

    public SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }

    public TableInfo getParticipantDetails(){
        return getSchema().getTable("UserDetails");
    }

    public TableInfo getAuthInfo(){     return getSchema().getTable("AuthInfo"); }

    public TableInfo getParticipantStudies(){
        return getSchema().getTable("ParticipantStudies");
    }

    public TableInfo getParticipantActivities(){
        return getSchema().getTable("ParticipantActivities");
    }

    public TableInfo getStudyConsent(){
        return getSchema().getTable("StudyConsent");
    }

    public TableInfo getPasswordHistory(){
        return getSchema().getTable("PasswordHistory");
    }


}
