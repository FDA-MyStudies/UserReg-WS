ALTER TABLE fdahpuserregws.AppPropertiesDetails ADD COLUMN IF NOT EXISTS regEmailSubSpanish VARCHAR(255) NULL DEFAULT NULL;

ALTER TABLE fdahpuserregws.AppPropertiesDetails ADD COLUMN IF NOT EXISTS regEmailBodySpanish VARCHAR(10000) NULL DEFAULT NULL;

ALTER TABLE fdahpuserregws.AppPropertiesDetails ADD COLUMN IF NOT EXISTS forgotEmailSubSpanish VARCHAR(255) NULL DEFAULT NULL;

ALTER TABLE fdahpuserregws.AppPropertiesDetails ADD COLUMN IF NOT EXISTS forgotEmailBodySpanish VARCHAR(10000) NULL DEFAULT NULL;