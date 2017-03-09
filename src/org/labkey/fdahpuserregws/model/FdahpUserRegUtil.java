package org.labkey.fdahpuserregws.model;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.ErrorCode;
import org.labkey.api.mbean.ErrorsMXBean;
import org.labkey.api.util.StringUtilsLabKey;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletResponse;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import static org.labkey.api.util.StringUtilsLabKey.DEFAULT_CHARSET;

/**
 * Created by Ravinder on 2/1/2017.
 */
public class FdahpUserRegUtil
{
    private static final Logger _log = Logger.getLogger(FdahpUserRegUtil.class);



    public enum ErrorCodes{
        INVALID_INPUT("INVALID_INPUT"),
        UNKNOWN("UNKNOWN"),
        STATUS_100("100"), // OK
        STATUS_101("101"), // Invalid Authentication (authKey is not valid).
        STATUS_102("102"), // Invalid Inputs (If any of the input parameter is missing).
        STATUS_103("103"), // No Data available.
        STATUS_104("104"), // Unknown Error
        STATUS_105("105"), // If there is no data to update.
        STATUS_106("106"), // Failed to generate token.
        STATUS_107("107"), // Failed to complete transaction.
        SESSION_EXPIRED_MSG("Session expired."),
        INVALID_AUTH_CODE("INVALID_AUTH_CODE"),
        ACCOUNT_DEACTIVATE_ERROR_MSG("Your account has been deactivated"),
        INVALID_USERNAME_PASSWORD_MSG("Invalid username and password"),
        EMAIL_EXISTS("the email already exists"),
        INVALID_INPUT_ERROR_MSG("Invalid input."),
        INACTIVE("INACTIVE"),
        SUCCESS("SUCCESS"),
        FAILURE("FAILURE"),
        JOINED("Joined"),
        COMPLETED("Completed"),
        STARTED("Started"),
        PAUSED("Paused"),
        PROFILE("profile"),
        SETTINGS("settings"),
        MESSAGE("message"),
        PARTICIPANTINFO("participantInfo"),
        STUDIES("studies"),
        ACTIVITIES("activities"),
        WITHDRAWN("Withdrawn"),
        NO_DATA_AVAILABLE("No data available"),
        CONNECTION_ERROR_MSG("Oops, something went wrong. Please try again after sometime"),
        WITHDRAWN_STUDY("You are already Withdrawn from study"),
        EMAIL_NOT_EXISTS("EMAIL DOESN'T EXISTS"),
        USER_NOT_EXISTS("USER DOESN'T EXISTS"),
        FAILURE_TO_SENT_MAIL("Oops, something went wrong.Failed to sent the Email"),
        OLD_PASSWORD_NOT_EXISTS("old password is wrong"),
        OLD_PASSWORD_AND_NEW_PASSWORD_NOT_SAME("Old password and new password cannot be same");
        private final String value;
        ErrorCodes(final String newValue){
            value=newValue;
        }
        public String getValue() { return value; }
    }



    public static void getFailureResponse(String status, String title, String message ,HttpServletResponse response){
        try {
            response.setHeader("status", status);
            response.setHeader("title", title);
            response.setHeader("StatusMessage", message);
            if(status.equalsIgnoreCase(ErrorCodes.STATUS_104.getValue()))
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
            if(status.equalsIgnoreCase(ErrorCodes.STATUS_102.getValue()))
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
            if(status.equalsIgnoreCase(ErrorCodes.STATUS_101.getValue()))
                if(message.equalsIgnoreCase(ErrorCodes.SESSION_EXPIRED_MSG.getValue()))
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ErrorCodes.INVALID_AUTH_CODE.getValue());
                else
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);

        } catch (Exception e) {
            _log.info("FdahpUserRegUtil - getFailureResponse() :: ERROR " , e);
        }
    }
    public static String getEncryptedString(String input) {
        StringBuffer sb = new StringBuffer();
        if(StringUtils.isNotEmpty(input)){
           input = input + "StudyGateway";
           try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
            messageDigest.update(input.getBytes("UTF-8"));
            byte[] digestBytes = messageDigest.digest();
            String hex = null;
            for (int i = 0; i < 8; i++) {
             hex = Integer.toHexString(0xFF & digestBytes[i]);
             if (hex.length() < 2)
              sb.append("0");
             sb.append(hex);
            }
           }catch (Exception ex) {
            _log.error(ex.getMessage());
           }
        }
        return sb.toString();
    }

    public static String getCurrentDateTime() {
        String getToday = "";
        try {
            Date today = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            getToday = formatter.format(today.getTime());
        } catch (Exception e) {
            _log.error(e);
        }
        return getToday;
    }

    public static String getEncodeString(String value){
        byte[] encodedBytes = Base64.encodeBase64(value.getBytes(DEFAULT_CHARSET));
        return new String(encodedBytes, DEFAULT_CHARSET);

    }
    public static String getDecodeString(String values){
        byte[] decodedBytes = Base64.decodeBase64(values);
        return new String(decodedBytes, DEFAULT_CHARSET);
    }

    public static boolean sendemail(String email, String subject, String messageBody) throws Exception{

        boolean sentMail = false;
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            Session session = Session.getInstance(props,
                    new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication("apps@boston-technology.com", "password789");
                        }
                    });
            Message message = new MimeMessage(session);
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject(subject);
            message.setContent(messageBody, "text/html");
            Transport.send(message);
            sentMail = true;
        } catch (MessagingException e) {
            _log.error("ERROR:  sendemail() - ",e);
            sentMail = false;
        } catch (Exception e) {
            _log.error("ERROR:  sendemail() - ",e);
        }

        return sentMail;
    }
}
