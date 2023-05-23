package dk.stbn.p2peksperiment;

public class Request {
    public String method;
    public String body;

    public String toString(){
        return( "Method: " + this.method + "\n"+
                "Body: {\n" +
                    this.body + "\n" +
                "  }");
    }
}
