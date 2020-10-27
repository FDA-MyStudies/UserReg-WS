https://www.labkey.org/Documentation/wiki-page.view?name=devMachine




for development build
---------------
1.gradlew cleanBuild deployApp
2.click the run icon on IDE to run in localhost


For deploy single module(if you want to do any change in code)
---------------------------
gradlew :server:modules:UserReg-WS:deployModule


for production build
---------------
Change the version on FdahpUserRegWSModule & build.gradle file in distribution folder
1-gradlew cleanBuild deployApp -PdeployMode=prod
2-gradlew -PdeployMode=prod :server:modules:UserReg-WS:distributions:Registration:distribution
