/*
 * Copyright © 2017-2019 Harvard Pilgrim Health Care Institute (HPHCI) and its Contributors. Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * Funding Source: Food and Drug Administration (“Funding Agency”) effective 18 September 2014 as Contract no. HHSF22320140030I/HHSF22301006T (the “Prime Contract”).
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package com.hphc.mystudies.model;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import com.hphc.mystudies.FdahpUserRegWSController;
import com.hphc.mystudies.FdahpUserRegWSManager;
import com.hphc.mystudies.FdahpUserRegWSModule;
import com.hphc.mystudies.bean.NotificationBean;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.files.FileContentService;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.module.ModuleProperty;
import org.labkey.api.resource.Resource;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.util.MailHelper;
import org.labkey.api.util.StringUtilsLabKey;

import javax.mail.Message;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;

import static org.labkey.api.util.StringUtilsLabKey.DEFAULT_CHARSET;

/**
 * Created by Ravinder on 2/1/2017.
 */
public class FdahpUserRegUtil
{
    private static final Logger _log = LogManager.getLogger(FdahpUserRegUtil.class);
    static String Email = "", password = "";
    static MailHelper.MultipartMessage msg;

    public enum ErrorCodes
    {
        UNKNOWN("UNKNOWN", "Desconocida"),
        SESSION_EXPIRED_MSG("Session expired.", "Sesión expirada"),
        ACCOUNT_DEACTIVATE_ERROR_MSG("Your account has been deactivated", "Tu cuenta ha sido desactivada"),
        INVALID_USERNAME_PASSWORD_MSG("Invalid username or password", "Usuario o contraseña invalido"),
        EMAIL_EXISTS("This email has already been used. Please try with different email address.", "Este correo electrónico ya ha sido utilizado. Intente con una dirección de correo electrónico diferente."),
        INVALID_INPUT_ERROR_MSG("Invalid input.", "Entrada inválida"),
        NO_DATA_AVAILABLE("No data available", "Datos no disponibles"),
        CONSENT_VERSION_REQUIRED("Consent version is required", "Se requiere la versión de consentimiento"),
        CONNECTION_ERROR_MSG("Oops, something went wrong. Please try again after sometime", "Huy! Algo salió mal. Inténtelo de nuevo después de algún tiempo"),
        WITHDRAWN_STUDY("You are already Withdrawn from study", "Ya estás retirado del estudio"),
        EMAIL_NOT_EXISTS("Email Doesn't Exists", "El correo electrónico no existe"),
        FAILURE_TO_SENT_MAIL("Oops, something went wrong. Failed to send Email", "Huy! Algo salió mal. No se pudo enviar el correo electrónico"),
        OLD_PASSWORD_NOT_EXISTS("Invalid old password", "Contra seña antigua no válida"),
        OLD_PASSWORD_AND_NEW_PASSWORD_NOT_SAME("Current Password and New Password cannot be same", "La contraseña actual y la nueva contraseña no pueden ser la misma"),
        NEW_PASSWORD_NOT_SAME_LAST_PASSWORD("New Password should not be the same as the last 10 passwords.", "La nueva contraseña no debe ser la misma que las últimas 10 contraseñas"),
        USER_ALREADY_VERIFIED("User already verified", "Usuario ya verificada"),
        INVALID_CODE("Invalid code", "Código inválido"),
        CODE_EXPIRED("Code Expired", "Código caducado"),
        INVALID_CREDENTIALS("Invalid credentials", "Credenciales no válidas"),
        ACCOUNT_LOCKED("As a security measure, this account has been locked for 15 minutes.", "Como medida de seguridad, esta cuenta ha estado bloqueada durante 15 minutos."),
        ACCOUNT_TEMP_LOCKED("As a security measure, this account has been locked for 15 minutes.", "Como medida de seguridad, esta cuenta ha estado bloqueada durante 15 minutos."),
        EMAIL_NOT_VERIFIED("Your account is not verified. Please verify your account by clicking on verification link which has been sent to your registered email. If not received, would you like to resend verification link?", "Su cuenta no está verificada. Verifique su cuenta haciendo clic en el enlace de verificación que se envió a su correo electrónico registrado. Si no lo recibe,¿le gustaría volver a enviar el enlace de verificación?"),
        INVALID_REFRESHTOKEN("Invalid refresh token.", "Token de actualización no válido"),
        APP_EXIST_NOTEXIST("You already have a valid account for this app. Please directly sign in using the same email and associated password.", "Ya tienes una cuenta válida para esta aplicación. Inicie sesión directamente con el mismo correo electrónico y la contraseña asociada."),
        ORG_NOTEXIST("Sorry, this email is already in use for platform-powered app(s) belonging to another organization. Please use another email to sign up for this app.", "Lo sentimos, este correo electrónico ya está en uso para aplicaciones de plataforma que pertenecen a otra organización. Utilice otro correo electrónico para registrarse en esta aplicación."),
        LOGIN_ORG_NOTEXIST("Sorry, this account is in use for platform-powered app(s) belonging to another organization. Please sign up with a different email and try again.", "Lo sentimos, esta cuenta está en uso para aplicaciones de plataforma que pertenecen a orta organización. "),
        FEEDBACK_NOT_SENT("Sorry, an error occurred and your feedback could not be sent to the organization. Please retry in some time.", "Lo sentimos, se produjo un error y sus comentarios no se pudieron enviar a la organización. Vuelva a intentarlo en algún momento."),
        INJUIRY_NOT_SENT("Sorry, an error occurred and your inquiry could not be sent to the organization. Please retry in some time.", "Lo sentimos, se produjo un error y su consulta no se pudo enviar a la organización. Vuelva a intentarlo en algún momento."),

        PROFILE("profile", "profile"),
        SETTINGS("settings", "settings"),
        STUDIES("studies", "studies"),
        ACTIVITIES("activities", "activities"),

        MESSAGE("message", "message"),
        SUCCESS("SUCCESS", "SUCCESS"),
        FAILURE("FAILURE", "FAILURE"),
        INVALID_INPUT("INVALID_INPUT", "INVALID_INPUT"),
        INVALID_AUTH_CODE("INVALID_AUTH_CODE", "INVALID_AUTH_CODE"),

        DEVICE_ANDROID("android", "android"),
        DEVICE_IOS("ios", "ios"),
        GATEWAY("Gateway", "Gateway"),
        STUDY("Study", "Study"),
        STUDY_LEVEL("ST", "ST"),
        GATEWAY_LEVEL("GT", "GT"),
        YET_TO_JOIN("yetToJoin", "yetToJoin"),
        IN_PROGRESS("inProgress", "inProgress"),
        WITHDRAWN("Withdrawn", "Withdrawn"),

        STATUS_100("100", "100"), // OK
        STATUS_101("101", "101"), // Invalid Authentication (authKey is not valid).
        STATUS_102("102", "102"), // Invalid Inputs (If any of the input parameter is missing).
        STATUS_103("103", "103"), // No Data available.
        STATUS_104("104", "104"), // Unknown Error
        STATUS_105("105", "105"), // If there is no data to update.
        STATUS_106("106", "106"), // Failed to generate token.
        STATUS_107("107", "107"); // Failed to complete transaction.

        private final String value;
        private final String valueSP;

        ErrorCodes(final String newValue, final String newValueSP)
        {
            value = newValue;
            valueSP = newValueSP;
        }

        public String getValue(String language)
        {
            if (language != null && language.equalsIgnoreCase(FdahpUserRegWSController.LANGUAGE_SP))
                return valueSP;
            else
                return value;
        }
    }

    public static String commaSeparatedString(List<String> studyIds)
    {
        if (studyIds.size() > 0)
        {
            StringBuilder studyBuilder = new StringBuilder();
            for (String n : studyIds)
            {
                studyBuilder.append("'").append(n.replace("'", "\\'")).append("',");
            }
            studyBuilder.deleteCharAt(studyBuilder.length() - 1);
            return studyBuilder.toString();
        }
        else
        {
            return "";
        }
    }

    public static void getFailureResponse(String language, String status, String title, String message, HttpServletResponse response)
    {
        try
        {
            response.setHeader("status", status);
            response.setHeader("title", title);
            if (language != null && language.equalsIgnoreCase(FdahpUserRegWSController.LANGUAGE_SP))
            {
                message = Base64.encodeBase64String(message.getBytes(StringUtilsLabKey.DEFAULT_CHARSET));
            }
            response.setHeader("StatusMessage", message);
            if (status.equalsIgnoreCase(ErrorCodes.STATUS_104.getValue(language)))
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
            if (status.equalsIgnoreCase(ErrorCodes.STATUS_102.getValue(language)))
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
            if (status.equalsIgnoreCase(ErrorCodes.STATUS_101.getValue(language)))
                if (message.equalsIgnoreCase(ErrorCodes.SESSION_EXPIRED_MSG.getValue(language)))
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ErrorCodes.SESSION_EXPIRED_MSG.getValue(language));
                else
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);

            if (status.equalsIgnoreCase(ErrorCodes.STATUS_103.getValue(language)))
                response.sendError(HttpServletResponse.SC_FORBIDDEN, message);

        }
        catch (Exception e)
        {
            _log.info("FdahpUserRegUtil - getFailureResponse() :: ERROR ", e);
        }
    }

    public static String getEncryptedString(String input)
    {
        StringBuffer sb = new StringBuffer();
        if (StringUtils.isNotEmpty(input))
        {
            input = input + "StudyGateway";
            try
            {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
                messageDigest.update(input.getBytes("UTF-8"));
                byte[] digestBytes = messageDigest.digest();
                String hex = null;
                for (int i = 0; i < 8; i++)
                {
                    hex = Integer.toHexString(0xFF & digestBytes[i]);
                    if (hex.length() < 2)
                        sb.append("0");
                    sb.append(hex);
                }
            }
            catch (Exception ex)
            {
                _log.error(ex.getMessage());
            }
        }
        return sb.toString();
    }

    public static String getCurrentDate()
    {
        String getToday = "";
        try
        {
            Date today = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            getToday = formatter.format(today.getTime());
        }
        catch (Exception e)
        {
            _log.error(e);
        }
        return getToday;
    }

    public static String getCurrentDateTime()
    {
        String getToday = "";
        try
        {
            Date today = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            getToday = formatter.format(today.getTime());

        }
        catch (Exception e)
        {
            _log.error(e);
        }
        return getToday;
    }

    public static String getFormattedDateTimeZone(String input, String inputFormat, String outputFormat)
    {
        String output = "";
        try
        {
            if (StringUtils.isNotEmpty(input))
            {
                SimpleDateFormat inputSDF = new SimpleDateFormat(inputFormat);
                Date inputDate = inputSDF.parse(input);
                SimpleDateFormat outputSDF = new SimpleDateFormat(outputFormat); //yyyy-MM-dd'T'hh:mm:ssZ, yyyy-MM-dd'T'HH:mm:ss.SSSZ
                output = outputSDF.format(inputDate);
            }
        }
        catch (Exception e)
        {
            _log.error(e);
        }
        return output;
    }

    public static String getEncodeString(String value)
    {
        byte[] encodedBytes = Base64.encodeBase64(value.getBytes(DEFAULT_CHARSET));
        return new String(encodedBytes, DEFAULT_CHARSET);

    }

    public static String getDecodeString(String values)
    {
        byte[] decodedBytes = Base64.decodeBase64(values);
        return new String(decodedBytes, DEFAULT_CHARSET);
    }

    public static Properties getProperties()
    {
        Properties prop = new Properties();
        InputStream input = null;
        try
        {

            Module m = ModuleLoader.getInstance().getModule(FdahpUserRegWSModule.NAME);
            InputStream is = m.getResourceStream("constants/message.properties");
            prop.load(is);

        }
        catch (Exception e)
        {
            _log.error("ERROR:  getProperties() - ", e);
        }
        return prop;
    }

    public static Date getCurrentUtilDateTime()
    {
        Date date = new Date();
        Calendar currentDate = Calendar.getInstance();
        String dateNow = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(currentDate.getTime());
        try
        {
            date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateNow);
        }
        catch (Exception e)
        {
            _log.error("FdahpUserRegUtil - getCurrentUtilDateTime() : ", e);

        }
        return date;
    }

    public static Date addMinutes(String currentDate, int minutes)
    {
        Date futureDate = null;
        try
        {
            Date dt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(currentDate);
            Calendar cal = Calendar.getInstance();
            cal.setTime(dt);
            cal.add(Calendar.MINUTE, minutes);
            Date newDate = cal.getTime();
            futureDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(newDate));
        }
        catch (Exception e)
        {
            _log.error("FdahpUserRegUtil - addHours() : ", e);
        }
        return futureDate;
    }

    public static Date addHours(String currentDate, int hours)
    {
        Date futureDate = null;
        try
        {
            Date dt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(currentDate);
            Calendar cal = Calendar.getInstance();
            cal.setTime(dt);
            cal.add(Calendar.HOUR, hours);
            Date newDate = cal.getTime();
            futureDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(newDate));
        }
        catch (Exception e)
        {
            _log.error("FdahpUserRegUtil - addHours() : ", e);
        }
        return futureDate;
    }

    public static Date addDays(String currentDate, int days)
    {
        Date futureDate = null;
        try
        {
            Date dt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(currentDate);
            Calendar cal = Calendar.getInstance();
            cal.setTime(dt);
            cal.add(Calendar.DATE, days);
            Date newDate = cal.getTime();
            futureDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(newDate));
        }
        catch (Exception e)
        {
            _log.error("FdahpUserRegUtil - addHours() : ", e);
        }
        return futureDate;
    }

    public static void sendMessage(String subject, String bodyHtml, String recipients, AppPropertiesDetails appPropertiesDetails)
    {
        try
        {
            MailHelper.MultipartMessage msg = MailHelper.createMultipartMessage();

            if (appPropertiesDetails.getEmail() != null)
            {
                msg.getSession().getProperties().put("mail.smtp.user", appPropertiesDetails.getEmail());
//                msg.getSession().getProperties().put("mail.smtp.password", AES.decrypt(appPropertiesDetails.getEmailPassword())); //Not needed if any mailing services is used on server
            }

            msg.setFrom(msg.getSession().getProperties().getProperty("mail.smtp.user"));
            msg.setSubject(subject);
            msg.setRecipients(Message.RecipientType.TO, recipients);
            msg.setEncodedHtmlContent(bodyHtml);
            MailHelper.send(msg, null, null);
        }
        catch (Exception e)
        {
            _log.error("Unable to send email MailHelper", e);
        }
    }


//    public static void sendMessage(String subject, String bodyHtml, String recipients, AppPropertiesDetails appPropertiesDetails)
//    {
//        try
//        {
//            InitialContext ctx = new InitialContext();
//            Context envCtx = (Context) ctx.lookup("java:comp/env");
//            Session _session = (Session) envCtx.lookup("mail/Session");
//
//            if (appPropertiesDetails == null || appPropertiesDetails.getEmail() == null || appPropertiesDetails.getEmailPassword() == null || appPropertiesDetails.getEmail().equalsIgnoreCase("") || appPropertiesDetails.getEmailPassword().equalsIgnoreCase(""))
//            {
//                Email = _session.getProperty("mail.smtp.user");
//                password = _session.getProperty("mail.smtp.password");
//            }
//            else
//            {
//                Email = appPropertiesDetails.getEmail();
//                password = AES.decrypt(appPropertiesDetails.getEmailPassword());
//            }
//
////            Properties props = new Properties();
////            props.put("mail.smtp.auth", "true");
////            props.put("mail.smtp.host", (String) _session.getProperty("mail.smtp.host"));
////            props.put("mail.smtp.starttls.enable", true);
////            props.put("mail.smtp.socketFactory.class", (String) _session.getProperty("mail.smtp.socketFactory.class"));
////            props.put("mail.smtp.port", (String) _session.getProperty("mail.smtp.port"));
////            props.put("mail.smtp.user", (String) Email);
////            props.put("mail.smtp.password", (String) password);
//
//            Properties props = _session.getProperties();
//            props.put("mail.smtp.user", Email);
////            props.put("mail.smtp.password", password);
//
//            _log.info("smtp Properties -- " + props.toString());
//            System.out.println("smtp Properties -- " + props.toString());
//
//            Session session = Session.getInstance(props,
//                    new javax.mail.Authenticator()
//                    {
//                        protected PasswordAuthentication getPasswordAuthentication()
//                        {
//                            _log.error("getPasswordAuthentication calling");
//                            System.out.println("getPasswordAuthentication calling");
//                            return new PasswordAuthentication(Email, password);
//                        }
//                    });
//            Message message = new MimeMessage(session);
//            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
//            message.setSubject(subject);
//            message.setContent(bodyHtml, "text/html; charset=UTF-8");
//            Transport.send(message);
//        }
//        catch (Exception e)
//        {
//            _log.error("ERROR:  sendemail() - ", e);
//        }
//    }

    public static void pushNotification(NotificationBean notificationBean, String language)
    {
        Properties configProp = FdahpUserRegUtil.getProperties();
        AppPropertiesDetails appPropertiesDetails = null;
        Resource r = null;
        String certificatePassword = "";
        try
        {
            appPropertiesDetails = FdahpUserRegWSManager.get(language).getAppPropertiesDetailsByAppId(notificationBean.getAppId(), notificationBean.getOrgId());
            Module module = ModuleLoader.getInstance().getModule(FdahpUserRegWSModule.NAME);
            ModuleProperty mp = module.getModuleProperties().get("StudyId");
            File file = null;
            if (appPropertiesDetails != null)
            {
                FileContentService fileContentService = ServiceRegistry.get().getService(FileContentService.class);
                Container availableContainer = getContainer_AppID(notificationBean.getAppId(), notificationBean.getOrgId());
                File root = fileContentService.getFileRoot(availableContainer, FileContentService.ContentType.files);
                certificatePassword = appPropertiesDetails.getIosCertificatePassword();
                try
                {
                    byte[] decodedBytes;
                    FileOutputStream fop;
                    decodedBytes = java.util.Base64.getDecoder().decode(appPropertiesDetails.getIosCertificate().replaceAll("\n", ""));
                    file = new File(root, "pushCert_" + appPropertiesDetails.getAppId() + ".p12");
                    fop = new FileOutputStream(file);
                    fop.write(decodedBytes);
                    fop.flush();
                    fop.close();
                }
                catch (Exception e)
                {
                    _log.error("FdahpUserRegWSController pushNotificationCertCreation:", e);
                }
                if (file != null)
                {
                    ApnsClient apnsClient = new ApnsClientBuilder()
                            .setApnsServer(ApnsClientBuilder.PRODUCTION_APNS_HOST)
                            .setClientCredentials(new File(file.getPath()), certificatePassword)
                            .build();
                    String payload = new SimpleApnsPayloadBuilder()
                            .setAlertBody(notificationBean.getNotificationText())
                            .addCustomProperty("subtype", notificationBean.getNotificationSubType())
                            .addCustomProperty("type", notificationBean.getNotificationType())
                            .addCustomProperty("studyId", notificationBean.getCustomStudyId())
                            .setSound("default")
                            .build();
                    if (notificationBean.getDeviceToken() != null)
                    {
                        for (int i = 0; i < notificationBean.getDeviceToken().length(); i++)
                        {
                            String token = TokenUtil.sanitizeTokenString(notificationBean.getDeviceToken().get(i).toString());
                            SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(token, appPropertiesDetails.getIosBundleId(), payload);
                            Future<PushNotificationResponse<SimpleApnsPushNotification>> result = apnsClient.sendNotification(pushNotification);
                            if (result.get().getRejectionReason() != null)
                            {
                                _log.debug(token + "  failed coz of  " + result.get().getRejectionReason());
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            _log.error("pushNotification ", e);
        }
    }

    public static String getStandardFileName(String StudyId, String userId, String version)
    {
        String dateTime = new SimpleDateFormat("MMddyyyyHHmmss").format(new Date());
        return StudyId + "_" + userId + "_" + version + "_" + dateTime + ".pdf";
    }

    public static boolean serverListening(String host, int port)
    {
        Socket s = null;
        try
        {
            s = new Socket(host, port);
//            System.out.println(s.toString());
            _log.error("Socket trying" + s.toString());
            return true;
        }
        catch (Exception e)
        {
            _log.error("Socket not available" + e.toString());
            return false;
        }
        finally
        {
            if (s != null)
                try
                {
                    s.close();
                }
                catch (Exception e)
                {
                    _log.error("not able to close socket" + e.toString());
                }
        }
    }

    public static boolean isSocketAliveUitlitybyCrunchify(String hostName, int port)
    {
        boolean isAlive = false;

        // Creates a socket address from a hostname and a port number
        SocketAddress socketAddress = new InetSocketAddress(hostName, port);
        Socket socket = new Socket();

        // Timeout required - it's in milliseconds
        int timeout = 2000;
        try
        {
            socket.connect(socketAddress, timeout);
            socket.close();
            isAlive = true;
        }
        catch (SocketTimeoutException exception)
        {
            _log.error("SocketTimeoutException " + hostName + ":" + port + ". " + exception.getMessage());
        }
        catch (IOException exception)
        {
            _log.error("IOException - Unable to connect to " + hostName + ":" + port + ". " + exception.getMessage());
        }
        return isAlive;
    }

    private static Container getContainer_AppID(String postedAppId, String postedOrgId)
    {
        Container appIdContainer = null;
        Container orgIdContainer = null;
        Module module = ModuleLoader.getInstance().getModule(FdahpUserRegWSModule.NAME);
        ModuleProperty mp = module.getModuleProperties().get("StudyId");
        List<Container> all = ContainerManager.getChildren(ContainerManager.getRoot());
        for (Container orgContainer : all)
        {
            System.out.println("org - " + mp.getValueContainerSpecific(orgContainer));
            if (postedOrgId.equalsIgnoreCase(mp.getValueContainerSpecific(orgContainer)))
            {
                orgIdContainer = orgContainer;
                List<Container> allApp = ContainerManager.getChildren(orgContainer);
                for (Container appContainer : allApp)
                {
                    System.out.println("app - " + mp.getValueContainerSpecific(appContainer));
                    if (postedAppId.equalsIgnoreCase(mp.getValueContainerSpecific(appContainer)))
                    {
                        appIdContainer = appContainer;
                        break;
                    }
                }
            }
        }
        if (appIdContainer == null)
        {
            _log.error("container not available for AppID " + postedAppId);
            if (orgIdContainer == null)
                _log.error("container not available for OrgID " + postedOrgId);
            return orgIdContainer;
        }
        else
        {
            return appIdContainer;
        }
    }
}
