gradlew cleanBuild deployApp

./gradlew deployApp

gradlew :server:customModules:fdahpUserRegWS:distributions:Registration:distribution


for production
---------------
1-gradlew cleanBuild
2-gradlew deployApp -PdeployMode=prod
3-for deployment
gradlew -PdeployMode=prod :server:customModules:fdahpUserRegWS:distributions:Registration:distribution


For deploy single module(if you want to do any change in code)
---------------------------
gradlew :server:customModules:fdahpUserRegWS:deployModule
