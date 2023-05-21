package dk.stbn.p2peksperiment;
import org.json.*;
public class HandleApi {

    public static Request readHttpRequest(String input) throws RuntimeException{
        try {
            JSONObject json = new JSONObject(input);
            Request request = new Request();
            request.method = json.getString("method");
            request.body = json.getString("body");
            return request;

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static String createHttpRequest(String method, String body) throws RuntimeException{
        try {
            JSONObject json = new JSONObject();
            Request request = new Request();
            request.method = method;
            request.body = body;
            json.put("method", request.method);
            json.put("body", request.body);

            return json.toString();

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }


    public static String createHttpResponse(String body, String status) throws RuntimeException{
        Response response = new Response();
        response.status = status;
        response.body = body;

        try {
            JSONObject json = new JSONObject();
            json.put("status", response.status);
            json.put("body", response.body);
            return json.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static Response readHttpResponse(String input) throws RuntimeException{
        try {
            JSONObject json = new JSONObject(input);
            Response response = new Response();
            response.status = json.getString("status");
            response.body = json.getString("body");
            return response;

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

}
