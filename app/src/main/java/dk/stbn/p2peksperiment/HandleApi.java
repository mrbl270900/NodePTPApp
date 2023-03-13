package dk.stbn.p2peksperiment;
import org.json.*;
public class HandleApi {

    public static Request readHttp(String input){
        try {
            JSONObject obj = new JSONObject(input);
            Request request = new Request();
            request.header = obj.getJSONObject("Request").getString("Header");
            request.method = obj.getJSONObject("Request").getString("Method");
            request.path = obj.getJSONObject("Request").getString("Path");
            request.body = obj.getJSONObject("Request").getString("Body");
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

    public static String createHttpRequest(String method, String path, String body){
        try {
            Request request = new Request();
            request.header = "HTTP/1.1";
            request.method = method;
            request.path = path;
            request.body = body;
            return "{\"Request\": { \"Header\": \"" + request.header + "\" \"Method\": \""+ request.method +
                    "\" \"Path\": \"" + request.path + "\" \"Body\": { \"" + request.body + "\" } }";
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

        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }


    }


    public static JSONObject createHttpResponse(String body, String status){
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
