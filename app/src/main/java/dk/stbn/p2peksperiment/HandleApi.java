package dk.stbn.p2peksperiment;
import org.json.*;
public class HandleApi {

    public Request readHttp(JSONObject input){
        try {
            Request request = new Request();
            request.header = input.getJSONObject("Request").getString("Header");
            request.method = input.getJSONObject("Request").getString("Method");
            request.path = input.getJSONObject("Request").getString("Path");
            request.body = input.getJSONObject("Request").getString("Body");
            return request;
            /*
            Request: {
            Header: HTTP/1.1
            Method: Post / Get / Put / Delete
            Path: (e.g. GetID)
                Body: {
                    ... the json body, if needed
                }
            }
            */

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }


    }


    public JSONObject createHttp(String body, String status){
        Response response = new Response();
        response.header = "HTTP/1.1";
        response.status = status;
        response.body = body;

        try {
            JSONObject obj = new JSONObject(response.toString());
            return obj;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    /*
    Response: {
        Header: HTTP/1.1
        Status: 200 OK / 400 Bad Request / 404 Not Found
        Body : {
        ... the json body, if needed
        }
    }
    */
    }

}
