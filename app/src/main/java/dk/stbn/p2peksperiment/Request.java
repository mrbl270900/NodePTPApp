package dk.stbn.p2peksperiment;

public class Request {
    public String method;
    public String path;
    public String body;

    public String toString(){
        return("Request: {\n" +
                "Header: HTTP/1.1\n" +
                "Method: " + this.method + "\n"+
                "Path: " + this.path + "\n" +
                "Body: {\n" +
                    this.body + "\n" +
                "  }\n" +
                "}");
    }
}
