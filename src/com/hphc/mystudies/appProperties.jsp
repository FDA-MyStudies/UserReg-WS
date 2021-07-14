<%@ page import="org.labkey.api.admin.AdminUrls" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
To reset the Registration Server properties, enter JSON text in the box and click "Submit". This will replace all existing properties.
<textarea id="json" rows="30" cols="80" style="width:100%"></textarea>
<%= button("Done").href(urlProvider(AdminUrls.class).getAdminConsoleURL()) %>
<%= button("Submit").onClick("submit();")%>
<script>
    function submit() {
        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL("fdahpuserregws", "appPropertiesUpdate.api"),
            method: "POST",
            jsonData: JSON.parse(document.getElementById("json").value),
            success: LABKEY.Utils.getCallbackWrapper(function(response)
            {
                if (response.success)
                    window.location = response.returnUrl;
                else if (response.message){
                    LABKEY.Utils.alert("Update failed", response.message);
                }
            })
        });
    }
</script>