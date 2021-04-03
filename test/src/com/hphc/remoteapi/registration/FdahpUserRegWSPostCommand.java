package com.hphc.remoteapi.registration;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.CommandResponse;

import java.net.URI;

public class FdahpUserRegWSPostCommand<ResponseType extends CommandResponse> extends FdahpUserRegWSCommand<ResponseType>
{
    private JSONObject _jsonObject = null;

    public FdahpUserRegWSPostCommand(String actionName)
    {
        super(actionName);
    }

    @Override
    protected boolean isPost()
    {
        return true;
    }

    public JSONObject getJsonObject()
    {
        return _jsonObject;
    }

    public void setJsonObject(JSONObject jsonObject)
    {
        _jsonObject = jsonObject;
    }

    @Override
    protected HttpUriRequest _createRequest(URI uri)
    {
        HttpPost request = new HttpPost(uri);

        //set the post body based on the supplied JSON object
        JSONObject json = getJsonObject();

        if (null != json)
        {
            request.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));
        }

        return request;
    }
}
