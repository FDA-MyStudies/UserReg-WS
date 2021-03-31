package com.hphc.mystudies.test.utils;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.junit.Assert.assertEquals;


public class TestReg
{
    private final Map<String,String> props;

    private final String baseURI;
    private final String basePath;
    private final int port;
    private final String base;
    private static final String methodPrefix = "fdahpuserregws-";

    private final String appId;
    private final String studyId;

    private String email;
    private String password;

    private String auth = null;
    private String userId = null;
    private String participantId = UUID.randomUUID().toString();

    private static final boolean verbose = true;
    void verbose(String v)
    {
        if (verbose)
            System.out.println(v);
    }


    public TestReg(Map<String,String> props) throws Exception
    {
        this.props = props;
        baseURI  = defaultString(props.get("baseURI"), "https://hpreg-stage.lkcompliant.net");
        basePath = "";
        port = Integer.parseInt(defaultString(props.get("port"), "443"));
        base = baseURI+":"+port+basePath;

        email = props.get("username");
        password = props.get("password");

        appId = defaultString(props.get("applicationId"), "FMSA001");
        studyId = defaultString(props.get("studyId"), "Arthritis001");

    }

    static final Random rand = new Random();

    String randString()
    {
        StringBuilder s = new StringBuilder();
        while (s.length() < 6)
        {
            int r = rand.nextInt(54);
            int ch;
            if (r < 10)
                ch = 48 + r;
            else
            {
                r -= 10;
                if (r < 26)
                    ch = 65 + r;
                else
                {
                    r -= 26;
                    ch = 97 + r;
                }
            }
            s.append((char)ch);
        }
        return s.toString();
    }

    Map.Entry<String,String> generateEmailPassword(String baseEmail)
    {
        int at = baseEmail.indexOf('@');
        String name = baseEmail.substring(0,at);
        String domain = baseEmail.substring(at);
        if (-1 != name.indexOf('+'))
            name = name.substring(0,name.indexOf('+'));
        return new AbstractMap.SimpleEntry<>(
                name + "+" + randString().toLowerCase() + domain,
                randString()
        );
    }


    public void basicTest() throws Exception
    {
        try (CloseableHttpClient httpclient = HttpClients.createDefault())
        {
            {
                HttpGet httpGet = new HttpGet(base + "/" + methodPrefix + "ping.api");
                execute(httpclient, httpGet);
            }

            // CONSIDER: option to save away registration or create new
            JSONObject registerJson = null;
            boolean newRegistration = true;
            if (newRegistration)
            {
                if (isBlank(email) || isBlank(password))
                {
                    var userpass = generateEmailPassword(defaultString(email, props.get("imap.username")));
                    if (isBlank(email))
                        email = userpass.getKey();
                    if (isBlank(password))
                        password = userpass.getValue();
                }
                verbose("REGISTER " + email);
                HttpPost register = getHttpPost("register.api", Map.of("emailId", email, "password", password));
                registerJson = execute(httpclient, register);

                // TODO: Need to send verification code back to /verify API before invoking any other APIs
                // For now, get verification code by magic
                String verification = getVerificationCode();
                HttpPost verify = getHttpPost("verify.api", Map.of("emailId", email, "code", verification));
                execute(httpclient, verify);
            }

            // Now set auth and userId; these headers are needed for all subsequent calls
            if (null != registerJson)
            {
                auth = (String) registerJson.get("auth");
                userId = (String) registerJson.get("userId");
            }
            else
            {
                auth = props.get("auth");
                userId = props.get("userId");
            }
            verbose("auth=" + auth + " userId=" + userId);

            {
                HttpGet userProfile = getHttpGet("confirmRegistration.api");
                JSONObject json = execute(httpclient, userProfile);
                assertEquals(true, json.get("verified"));
            }

            {
                HttpGet userProfile = getHttpGet("userProfile.api");
                execute(httpclient, userProfile);
            }

            {
                HttpGet studyState = getHttpGet("studyState.api");
                JSONObject response = execute(httpclient, studyState);
                assertEquals(0, ((JSONArray) response.get("studies")).size());
            }

            {
                JSONObject json = new JSONObject();
                JSONArray studies = new JSONArray();
                studies.addAll(List.of(Map.of(
                        "studyId", studyId,
                        "status", "inProgress",
                        "participantId", participantId,
                        "bookmarked", true,
                        "completion", 1,
                        "adherence", 100
                )));
                json.put("studies", studies);
                HttpPost updateStudyState = getHttpPost("updateStudyState.api", Map.of());
                updateStudyState.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));
                execute(httpclient, updateStudyState);
            }

            {
                HttpGet studyState = getHttpGet("studyState.api");
                JSONObject response = execute(httpclient, studyState);
                JSONArray studies = (JSONArray) response.get("studies");
                assertEquals(1, studies.size());
                JSONObject study = (JSONObject)studies.get(0);
                assertEquals("inProgress", study.get("status"));
                assertEquals("Arthritis001", study.get("studyId"));
            }

            {
                String pdf = Base64.getEncoder().encodeToString(Files.readAllBytes(Path.of("consent.pdf")));
                JSONObject consent = new JSONObject();
                consent.put("version", "1.0");
                consent.put("status", "completed");
                consent.put("pdf", pdf);

                JSONObject params = new JSONObject();
                params.put("studyId", studyId);
                params.put("eligibility", true);
                params.put("sharing", "");
                params.put("consent", consent);

                HttpPost updateConsent = getHttpPost("updateEligibilityConsentStatus.api", Map.of());
                StringEntity entity = new StringEntity(params.toString(), ContentType.APPLICATION_JSON);
                updateConsent.setEntity(entity);
                execute(httpclient, updateConsent);
            }

            {
                HttpGet studyState = getHttpGet("studyState.api");
                JSONObject response = execute(httpclient, studyState);
                JSONArray studies = (JSONArray) response.get("studies");
                assertEquals(1, studies.size());
            }
        }
    }

    private String getVerificationCode() throws Exception
    {
        Pattern verificationCodePattern = Pattern.compile("<span>\\s*<strong>\\s*Verification\\s+Code:\\s*</strong>\\s*(\\w\\w\\w\\w\\w\\w)\\s*</span>");
        return null;
    }

    private HttpGet getHttpGet(String endpoint)
    {
        return addHeaders(new HttpGet(base + "/" + methodPrefix + endpoint));
    }

    private HttpPost getHttpPost(String endpoint, Map<String, String> paramsMap) throws UnsupportedEncodingException
    {
        HttpPost httpPost = addHeaders(new HttpPost(base + "/" + methodPrefix + endpoint));

        if (!paramsMap.isEmpty())
        {
            List<NameValuePair> params = paramsMap.entrySet().stream()
                    .map(e->new BasicNameValuePair(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
            httpPost.setEntity(new UrlEncodedFormEntity(params));
        }

        return httpPost;
    }

    private <R extends HttpUriRequest> R addHeaders(R request)
    {
        request.addHeader("applicationId", appId);
        request.addHeader("orgId", "OrgName");

        if (null != auth)
            request.addHeader("auth", auth);
        if (null != userId)
            request.addHeader("userId", userId);

        verbose("email:" + email + " password:" + password + " applicationId:"+appId + " orgId:OrgId " + " auth:" + auth + " userId:" + userId);
        return request;
    }

    private JSONObject execute(CloseableHttpClient httpClient, HttpUriRequest request) throws Exception
    {
        try (CloseableHttpResponse response = httpClient.execute(request))
        {
            ResponseHandler<String> handler = new BasicResponseHandler();
            StatusLine status = response.getStatusLine();
            System.out.println(request.getURI());

            if (status.getStatusCode() == HttpStatus.SC_OK || status.getStatusCode() == HttpStatus.SC_CREATED)
            {
                String stringResponse = handler.handleResponse(response);
                System.out.println(stringResponse);

                JSONObject json = (JSONObject) new JSONParser().parse(stringResponse);

                if (json.containsKey("message"))
                    assertEquals("success", json.get("message"));

                if (json.containsKey("success"))
                    assertEquals(true, json.get("success"));

                return json;
            }
            else
            {
                String message = String.format("Received response status %d using uri %s", status.getStatusCode(), request.getURI());
                System.out.println(message);
                throw new Exception(message);
            }
        }
    }
}
