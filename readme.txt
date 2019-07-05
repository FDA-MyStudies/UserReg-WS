https://svn.mgt.labkey.host/stedi/branches/release18.3-SNAPSHOT

https://www.labkey.org/Documentation/wiki-page.view?name=devMachine#checklist




gradlew cleanBuild deployApp

./gradlew deployApp

gradlew :server:customModules:fdahpUserRegWS:distributions:Registration:distribution


for production
---------------
1-gradlew cleanBuild deployApp -PdeployMode=prod
2-for deployment
gradlew -PdeployMode=prod :server:customModules:fdahpUserRegWS:distributions:Registration:distribution


For deploy single module(if you want to do any change in code)
---------------------------
gradlew :server:customModules:fdahpUserRegWS:deployModule







