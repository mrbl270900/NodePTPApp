package dk.stbn.p2peksperiment;


import android.annotation.SuppressLint;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // Global data
    private final int PORT = 4444;
    int clientNumber = 0;
    // UI-elements
    private Button startClient, submitIP;
    private TextView serverInfoTv, clientInfoTv;
    private EditText ipInputField;
    // Logging/status messages
    private String serverinfo = "SERVER LOG:";
    private String clientinfo = "CLIENT LOG: ";
    private String THIS_IP_ADDRESS = "";
    private String REMOTE_IP_ADDRESS = "";
    private Thread serverThread = new Thread(new MyServerThread());
    private Thread clientThread = new Thread(new MyClientThread());
    private String command = "getId";
    // Some state
    private String nodeCommand;
    private boolean ip_submitted = false;
    private boolean serverCarryOn = true;
    private String nodeIp;
    private boolean clientStarted = false;
    private String dataFromOtherNode;
    private Node node;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //UI boilerplate
        startClient = findViewById(R.id.button);
        serverInfoTv = findViewById(R.id.serveroutput);
        clientInfoTv = findViewById(R.id.clientoutput);
        submitIP = findViewById(R.id.sendclient);
        ipInputField = findViewById(R.id.clientmessagefield);

        //Setting click-listeners on buttons
        startClient.setOnClickListener(this);
        submitIP.setOnClickListener(this);

        //Setting some UI state
        ipInputField.setHint("Submit IP-address");
        startClient.setEnabled(false); //deactivates the button

        //Getting the IP address of the device
        THIS_IP_ADDRESS = getLocalIpAddress();
        sUpdate("This IP is " + THIS_IP_ADDRESS);

        //setting up a node
        List<String> nodesLeft = new ArrayList<>();
        nodesLeft.add(THIS_IP_ADDRESS);
        nodesLeft.add(THIS_IP_ADDRESS);
        nodesLeft.add(THIS_IP_ADDRESS);
        List<String> nodesRight = new ArrayList<>();
        nodesRight.add(THIS_IP_ADDRESS);
        nodesRight.add(THIS_IP_ADDRESS);
        nodesRight.add(THIS_IP_ADDRESS);
        node = new Node(THIS_IP_ADDRESS, nodesLeft, nodesRight);


        //Starting the server thread
        serverThread.start();
        serverinfo += "- - - SERVER STARTED - - -\n";

    }

    @Override
    public void onClick(View view) {

        if (view == startClient) {
            if (!clientStarted) {
                //her should bed logic for making a new node in chain
                clientStarted = true;
                command = HandleApi.createHttpRequest("Get", "getId", "empty");
                System.out.println(command);
                clientThread.start();
                clientinfo += "- - - CLIENT STARTED - - - \n";
                startClient.setText("Resend");
            } else {
                if (!ipInputField.getText().toString().equals(REMOTE_IP_ADDRESS)) {
                    String newCommand = ipInputField.getText().toString();
                    String[] newCommandList = newCommand.split(",");
                    command = HandleApi.createHttpRequest(newCommandList[0], newCommandList[1], newCommandList[2]);
                } else {
                    command = HandleApi.createHttpRequest("Get", "getId", "empty");
                    System.out.println(command);
                }
                Thread clientThread = new Thread(new MyClientThread());
                clientThread.start();
            }
        } else if (view == submitIP) {
            if (!ip_submitted) {
                ip_submitted = true;
                REMOTE_IP_ADDRESS = ipInputField.getText().toString();
                startClient.setEnabled(true);
                submitIP.setEnabled(false);
            }
        }

    }//onclick

        // !!! Returns 0.0.0.0 on emulator
        //Modified from https://www.tutorialspoint.com/sending-and-receiving-data-with-sockets-in-android
        private String getLocalIpAddress() {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            assert wifiManager != null;
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipInt = wifiInfo.getIpAddress();
            String address = null;
            try {
                address = InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            return address;
        }

        //Wait by setting the thread to sleep for 1,5 seconds
        private void waitABit() {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        //Server update TexView
        private void sUpdate(String message) {
            //Run this code on UI-thread
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    serverinfo = message + "\n" + serverinfo;
                    serverInfoTv.setText(serverinfo);
                }
            });

        }

        //Client update TextView
        private void cUpdate(String message) {
            System.out.println(message);

            //Run this code on UI-thread
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    clientinfo = message + "\n" + clientinfo;
                    clientInfoTv.setText(clientinfo);
                }
            });
        }


        private void setNeighbor(String ip, Boolean left, Integer nodeNr){
            nodeIp = ip;

            if(left) {
                nodeCommand = HandleApi.createHttpRequest("Get", "GetPhonebookLeft", "tom");
            }else{
                nodeCommand = HandleApi.createHttpRequest("Get", "GetPhonebookRight", "tom");
            }

            Thread nodeThread1 = new Thread(new MyNodeThread());
            nodeThread1.start();
            waitABit();

            Response data = HandleApi.readHttpResponse(dataFromOtherNode);
            data.body = data.body.replace("[", "");
            data.body = data.body.replace("]", "");
            data.body = data.body.replace(" ", "");
            String[] dataSplit = data.body.split(",");
            String leftList;

            if(nodeNr == 1) {
                leftList = THIS_IP_ADDRESS + "," + dataSplit[0] + "," + dataSplit[1];
            }else if(nodeNr == 2) {
                leftList = dataSplit[0] + "," + THIS_IP_ADDRESS + "," + dataSplit[1];
            }else {
                leftList = dataSplit[0] + "," + dataSplit[1] + "," + THIS_IP_ADDRESS;
            }

            if(left) {
                nodeCommand = HandleApi.createHttpRequest("Get", "newNeighbor", leftList + ",left");
            }else{
                nodeCommand = HandleApi.createHttpRequest("Get", "newNeighbor", leftList + ",right");
            }

            Thread nodeThread4 = new Thread(new MyNodeThread());
            nodeThread4.start();
            waitABit();

        }
    class MyServerThread implements Runnable { //TODO implemnt multi thread for server
        @SuppressLint("SuspiciousIndentation")
        @Override
        public void run() {
            //Always be ready for next client
            boolean running = true;
            while (true) {
                try {
                    ServerSocket serverSocket = new ServerSocket(4444);

                    //Always be ready for next client
                    while (true) {
                        sUpdate("SERVER: start listening..");
                        Socket clientSocket = serverSocket.accept();
                        sUpdate("SERVER connection accepted");
                        clientNumber++;
                        new RemoteClient(clientSocket, clientNumber).start();

                    }//while listening for clients

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


        //The below two methods are for updating UI-elements on the main thread

    class RemoteClient extends Thread {
        private final Socket client;
        private int number;

        public RemoteClient(Socket clientSocket, int number) {
            this.client = clientSocket;
            this.number = number;
        }

        public void run() {
            try {
                DataInputStream inNodeStream = new DataInputStream(client.getInputStream());
                DataOutputStream outNodeStream = new DataOutputStream(client.getOutputStream());
                String str;
                String response = "ok";
                String status = "200 ok";
                serverCarryOn = true;
                //Start conversation
                while (serverCarryOn) {
                    try {
                        str = (String) inNodeStream.readUTF();
                        try {
                            Request input = HandleApi.readHttpRequest(str);

                            sUpdate("Client says: " + str);
                            System.out.println("client to server " + str);
                            //logic to handle things
                            if (input.path.equalsIgnoreCase("getId")) {
                                //run with getId
                                response = node.getId();
                                status = "200 ok";
                            } else if (input.path.equalsIgnoreCase("startnetwork")) {
                                //run with path=startnetwork and body=ip of know running node
                                nodeIp = input.body;
                                nodeCommand = HandleApi.createHttpRequest("Get", "GetPhonebookLeft", "tom");
                                Thread nodeThread1 = new Thread(new MyNodeThread());
                                nodeThread1.start();
                                waitABit();
                                Response data = HandleApi.readHttpResponse(dataFromOtherNode);
                                data.body = data.body.replace("[", "");
                                data.body = data.body.replace("]", "");
                                data.body = data.body.replace(" ", "");
                                String[] dataSplit = data.body.split(",");

                                nodeCommand = HandleApi.createHttpRequest("Get", "GetPhonebookRight", "tom");
                                Thread nodeThread2 = new Thread(new MyNodeThread());
                                nodeThread2.start();
                                waitABit();
                                data = HandleApi.readHttpResponse(dataFromOtherNode);
                                data.body = data.body.replace("[", "");
                                data.body = data.body.replace("]", "");
                                data.body = data.body.replace(" ", "");
                                String[] dataSplit2 = data.body.split(",");

                                if(dataSplit[0].equals(nodeIp)){
                                    ArrayList<String> leftArrayList = new ArrayList<>();
                                    leftArrayList.add(nodeIp);
                                    leftArrayList.add(nodeIp);
                                    leftArrayList.add(nodeIp);
                                    node.newNeighbor(leftArrayList, "left");
                                    ArrayList<String> rightArrayList = new ArrayList<>();
                                    rightArrayList.add(nodeIp);
                                    rightArrayList.add(nodeIp);
                                    rightArrayList.add(nodeIp);
                                    node.newNeighbor(rightArrayList, "right");
                                    String List = THIS_IP_ADDRESS + "," + THIS_IP_ADDRESS + "," + THIS_IP_ADDRESS;
                                    nodeCommand = HandleApi.createHttpRequest("Get", "newNeighbor", List + ",right");
                                    Thread nodeThread4 = new Thread(new MyNodeThread());
                                    nodeThread4.start();
                                    waitABit();

                                    nodeCommand = HandleApi.createHttpRequest("Get", "newNeighbor", List + ",left");
                                    Thread nodeThread5 = new Thread(new MyNodeThread());
                                    nodeThread5.start();
                                    waitABit();

                                }else if(dataSplit[0].equals(dataSplit2[0])){
                                    ArrayList<String> leftArrayList = new ArrayList<>();
                                    leftArrayList.add(dataSplit[0]);
                                    leftArrayList.add(nodeIp);
                                    leftArrayList.add(THIS_IP_ADDRESS);
                                    node.newNeighbor(leftArrayList, "left");

                                    ArrayList<String> rightArrayList = new ArrayList<>();
                                    rightArrayList.add(nodeIp);
                                    rightArrayList.add(dataSplit[0]);
                                    rightArrayList.add(THIS_IP_ADDRESS);
                                    node.newNeighbor(rightArrayList, "right");

                                    String List = THIS_IP_ADDRESS + "," + dataSplit[0] + "," + nodeIp;
                                    nodeCommand = HandleApi.createHttpRequest("Get", "newNeighbor", List + ",left");
                                    Thread nodeThread5 = new Thread(new MyNodeThread());
                                    nodeThread5.start();
                                    waitABit();

                                    List = dataSplit[0] + "," + THIS_IP_ADDRESS + "," + nodeIp;
                                    nodeCommand = HandleApi.createHttpRequest("Get", "newNeighbor", List + ",right");
                                    Thread nodeThread4 = new Thread(new MyNodeThread());
                                    nodeThread4.start();
                                    waitABit();

                                    // mangler at snakke med det andet node
                                    String oldIp = nodeIp;
                                    nodeIp = dataSplit[0];
                                    List = oldIp + "," + THIS_IP_ADDRESS + "," + nodeIp;
                                    nodeCommand = HandleApi.createHttpRequest("Get", "newNeighbor", List + ",left");
                                    Thread nodeThread6 = new Thread(new MyNodeThread());
                                    nodeThread6.start();
                                    waitABit();
                                    List = THIS_IP_ADDRESS + "," + oldIp + "," + nodeIp;
                                    nodeCommand = HandleApi.createHttpRequest("Get", "newNeighbor", List + ",right");
                                    Thread nodeThread7 = new Thread(new MyNodeThread());
                                    nodeThread7.start();
                                    waitABit();
                                }else if(nodeIp.equals(dataSplit[2])){
                                    ArrayList<String> leftArrayList = new ArrayList<>();
                                    leftArrayList.add(dataSplit[0]);
                                    leftArrayList.add(dataSplit[1]);
                                    leftArrayList.add(nodeIp);
                                    node.newNeighbor(leftArrayList, "left");

                                    ArrayList<String> rightArrayList = new ArrayList<>();
                                    rightArrayList.add(nodeIp);
                                    rightArrayList.add(dataSplit2[0]);
                                    rightArrayList.add(dataSplit2[1]);
                                    node.newNeighbor(rightArrayList, "right");

                                    String List = dataSplit2[0] + "," + dataSplit2[1] + "," + THIS_IP_ADDRESS;
                                    nodeCommand = HandleApi.createHttpRequest("Get", "newNeighbor", List + ",right");
                                    Thread nodeThread4 = new Thread(new MyNodeThread());
                                    nodeThread4.start();
                                    waitABit();

                                    List = THIS_IP_ADDRESS + "," + dataSplit[0] + "," + dataSplit[1];
                                    nodeCommand = HandleApi.createHttpRequest("Get", "newNeighbor", List + ",left");
                                    Thread nodeThread5 = new Thread(new MyNodeThread());
                                    nodeThread5.start();
                                    waitABit();

                                    // mangler at snakke med det andet node
                                    String oldIp = nodeIp;
                                    nodeIp = dataSplit[0];
                                    List = dataSplit[1] + "," + dataSplit[2] + "," + THIS_IP_ADDRESS;
                                    nodeCommand = HandleApi.createHttpRequest("Get", "newNeighbor", List + ",left");
                                    Thread nodeThread6 = new Thread(new MyNodeThread());
                                    nodeThread6.start();
                                    waitABit();

                                    List = THIS_IP_ADDRESS + "," + oldIp + "," + dataSplit[1];
                                    nodeCommand = HandleApi.createHttpRequest("Get", "newNeighbor", List + ",left");
                                    Thread nodeThread7 = new Thread(new MyNodeThread());
                                    nodeThread7.start();
                                    waitABit();

                                    // mangler at snakke med det tredje node
                                    nodeIp = dataSplit[1];
                                    List = oldIp + "," + THIS_IP_ADDRESS + "," + dataSplit[0];
                                    nodeCommand = HandleApi.createHttpRequest("Get", "newNeighbor", List + ",left");
                                    Thread nodeThread8 = new Thread(new MyNodeThread());
                                    nodeThread8.start();
                                    waitABit();

                                    List = dataSplit[0] + "," + THIS_IP_ADDRESS + "," + oldIp;
                                    nodeCommand = HandleApi.createHttpRequest("Get", "newNeighbor", List + ",left");
                                    Thread nodeThread9 = new Thread(new MyNodeThread());
                                    nodeThread9.start();
                                    waitABit();
                                }else {
                                    ArrayList<String> leftArrayList = new ArrayList<>();
                                    leftArrayList.add(nodeIp);
                                    leftArrayList.add(dataSplit[0]);
                                    leftArrayList.add(dataSplit[1]);
                                    node.newNeighbor(leftArrayList, "left");
                                    ArrayList<String> rightArrayList = new ArrayList<>();
                                    rightArrayList.add(dataSplit2[0]);
                                    rightArrayList.add(dataSplit2[1]);
                                    rightArrayList.add(dataSplit2[2]);
                                    node.newNeighbor(rightArrayList, "right");

                                    setNeighbor(nodeIp, true, 1);
                                    setNeighbor(dataSplit2[0], true, 2);
                                    setNeighbor(dataSplit2[1], true, 3);
                                    setNeighbor(dataSplit[0], false, 1);
                                    setNeighbor(dataSplit[1], false, 2);
                                    setNeighbor(dataSplit[2], false, 3);
                                }


                            }else if (input.path.equalsIgnoreCase("newNeighbor")) {
                                //run with path=newNeighbor and body=id1,id2,id3,left
                                status = "200 ok";
                                String[] bodySplit = input.body.split(",");
                                List<String> neighbors = new ArrayList();
                                neighbors.add(bodySplit[0]);
                                neighbors.add(bodySplit[1]);
                                neighbors.add(bodySplit[2]);
                                node.newNeighbor(neighbors, bodySplit[3]);
                                response = "neighbors set";
                            } else if (input.path.equalsIgnoreCase("GetPhonebookLeft")) {
                                //run with GetPhonebookLeft
                                response = node.GetPhonebookLeft().toString();
                            } else if (input.path.equalsIgnoreCase("GetPhonebookRight")) {
                                //run with GetPhonebookRight
                                status = "200 ok";
                                response = node.GetPhonebookRight().toString();
                            } else if (input.path.equalsIgnoreCase("getdata")) {
                                //run with path=GetData adn body=dataid
                                status = "200 ok";
                                response = node.GetData(input.body);
                                if(response == null){
                                    status = "404 not found";
                                    response = "no data";
                                }
                            } else if (input.path.equalsIgnoreCase("RemoveData")) {
                                //run with path=RemoveData and body=some dataid
                                if(node.NodesLeft.get(0).equals(client.getRemoteSocketAddress().toString())
                                        || node.NodesRight.get(0).equals(client.getRemoteSocketAddress().toString())){
                                    status = "200 ok";
                                    response = node.RemoveData(input.body);
                                    if(response == null){
                                        status = "404 not found";
                                        response = "Data not on node";
                                    }
                                }else{
                                    status = "200 ok";
                                    response = node.RemoveData(input.body);
                                    if(response == null){
                                        status = "404 not found";
                                        response = "Data not on node";
                                    }//TODO missing logic for getting data from nodes futher out then it self
                                }
                            } else if (input.path.equalsIgnoreCase("AddData")) {
                                //run like path=AddData and body = data
                                status = "200 ok";
                                System.out.println(client.getRemoteSocketAddress().toString());
                                if(node.NodesLeft.get(0).equals(client.getRemoteSocketAddress().toString())
                                        || node.NodesRight.get(0).equals(client.getRemoteSocketAddress().toString())){
                                    response = "Data id: " + node.AddData(input.body, client.getRemoteSocketAddress().toString());
                                }else{
                                    response = "Data id: " + node.AddData(input.body, THIS_IP_ADDRESS);
                                    nodeCommand = HandleApi.createHttpRequest("Get", "adddata", input.body);
                                    nodeIp = node.NodesLeft.get(0);
                                    Thread nodeThread1 = new Thread(new MyNodeThread());
                                    nodeThread1.start();
                                    nodeIp = node.NodesRight.get(0);
                                    Thread nodeThread2 = new Thread(new MyNodeThread());
                                    nodeThread2.start();
                                }
                            } else {
                                status = "400 bad rec";
                                response = "Fail";
                            }
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                            System.out.println("server fail on command");
                            status = "400 bad rec";
                            response = "Fail";
                        }

                        String jsonString = HandleApi.createHttpResponse(response, status);
                        sUpdate(jsonString);
                        outNodeStream.writeUTF(jsonString);
                        outNodeStream.flush();
                        waitABit();
                        serverCarryOn = false;
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }//serverCarryOn loop

                //Closing everything down
                client.shutdownInput();
                sUpdate("SERVER: inputstream closed");
                client.shutdownOutput();
                sUpdate("SERVER: outputstream closed");
                client.close();
                sUpdate("SERVER: Client socket closed");
            } catch (IOException e) {
                sUpdate("oops!!");
                throw new RuntimeException(e);
            }
        }
    }

        class MyClientThread implements Runnable {
            @Override
            public void run() {

                try {
                    cUpdate("CLIENT: starting client socket ");
                    Socket connectionToServer = new Socket(REMOTE_IP_ADDRESS, 4444);
                    cUpdate("CLIENT: client connected ");

                    DataInputStream inClientStream = new DataInputStream(connectionToServer.getInputStream());
                    DataOutputStream outClientStream = new DataOutputStream(connectionToServer.getOutputStream());
                    String messageFromServer;
                    outClientStream.writeUTF(command);
                    outClientStream.flush();
                    cUpdate("I said:      " + command);
                    messageFromServer = inClientStream.readUTF();
                    Response response = HandleApi.readHttpResponse(messageFromServer);
                    cUpdate("Server says: " + response);
                    waitABit();
                    connectionToServer.shutdownInput();
                    cUpdate("CLIENT: closed inputstream");
                    connectionToServer.shutdownOutput();
                    cUpdate("CLIENT: closed outputstream");
                    connectionToServer.close();
                    cUpdate("CLIENT: closed socket");

                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }

            }//run()
        } //class MyClientThread


    class MyNodeThread implements Runnable {
        @Override
        public void run() {

            try {
                cUpdate("CLIENT: starting client socket ");
                Socket connectionToServer = new Socket();
                SocketAddress socketAddress = new InetSocketAddress(nodeIp, 4444);
                connectionToServer.connect(socketAddress, 10000);
                cUpdate("CLIENT: client connected ");

                DataInputStream inClientStream = new DataInputStream(connectionToServer.getInputStream());
                DataOutputStream outClientStream = new DataOutputStream(connectionToServer.getOutputStream());
                String messageFromServer;
                outClientStream.writeUTF(nodeCommand);
                outClientStream.flush();
                cUpdate("I said:      " + nodeCommand);
                messageFromServer = inClientStream.readUTF();
                Response response = HandleApi.readHttpResponse(messageFromServer);
                cUpdate("Server says: " + response);
                waitABit();
                dataFromOtherNode = messageFromServer;
                connectionToServer.shutdownInput();
                cUpdate("CLIENT: closed inputstream");
                connectionToServer.shutdownOutput();
                cUpdate("CLIENT: closed outputstream");
                connectionToServer.close();
                cUpdate("CLIENT: closed socket");

            } catch (IOException e) {
                e.printStackTrace();
                if(node.NodesLeft.get(0).equals(nodeIp)){
                    nodeIp = node.NodesLeft.get(1);
                    nodeCommand = HandleApi.createHttpRequest("Get", "getip", "tom");
                    Thread nodeThread1 = new Thread(new MyNodeThread());
                    nodeThread1.start();
                } else if(node.NodesRight.get(0).equals(nodeIp)) {
                    nodeIp = node.NodesRight.get(1);
                    nodeCommand = HandleApi.createHttpRequest("Get", "getip", "tom");
                    Thread nodeThread1 = new Thread(new MyNodeThread());
                    nodeThread1.start();
                }else if(node.NodesLeft.get(1).equals(nodeIp)) {
                    nodeIp = node.NodesRight.get(2);
                    nodeCommand = HandleApi.createHttpRequest("Get", "getip", "tom");
                    Thread nodeThread1 = new Thread(new MyNodeThread());
                    nodeThread1.start();
                }else if(node.NodesRight.get(1).equals(nodeIp)) {
                    nodeIp = node.NodesRight.get(2);
                    nodeCommand = HandleApi.createHttpRequest("Get", "getip", "tom");
                    Thread nodeThread1 = new Thread(new MyNodeThread());
                    nodeThread1.start();
                }else{
                    throw new RuntimeException(e);
                }


                if(node.NodesLeft.contains(nodeIp)){
                    nodeCommand = HandleApi.createHttpRequest("Get", "GetPhonebookLeft", "tom");
                    Thread nodeThread1 = new Thread(new MyNodeThread());
                    nodeThread1.start();
                    waitABit();
                    waitABit();
                    waitABit();
                    Response data = HandleApi.readHttpResponse(dataFromOtherNode);
                    data.body = data.body.replace("[", "");
                    data.body = data.body.replace("]", "");
                    data.body = data.body.replace(" ", "");
                    String[] dataSplit = data.body.split(",");
                    List<String> neighborLeft = new ArrayList<>();
                    neighborLeft.add(nodeIp);
                    neighborLeft.add(dataSplit[0]);
                    neighborLeft.add(dataSplit[1]);
                    node.newNeighbor(neighborLeft, "left");
                    List<String> rightList = new ArrayList<>();
                    rightList.add(THIS_IP_ADDRESS);
                    rightList.add(node.NodesLeft.get(0));
                    rightList.add(node.NodesLeft.get(1));
                    nodeCommand = HandleApi.createHttpRequest("Get", "newNeighbor", rightList + ",right");
                    Thread nodeThread2 = new Thread(new MyNodeThread());
                    nodeThread2.start();
                }else{
                    nodeCommand = HandleApi.createHttpRequest("Get", "GetPhonebookRight", "tom");
                    Thread nodeThread1 = new Thread(new MyNodeThread());
                    nodeThread1.start();
                    waitABit();
                    waitABit();
                    waitABit();
                    Response data = HandleApi.readHttpResponse(dataFromOtherNode);
                    data.body = data.body.replace("[", "");
                    data.body = data.body.replace("]", "");
                    data.body = data.body.replace(" ", "");
                    String[] dataSplit = data.body.split(",");
                    List<String> neighborRight = new ArrayList<>();
                    neighborRight.add(nodeIp);
                    neighborRight.add(dataSplit[0]);
                    neighborRight.add(dataSplit[1]);
                    node.newNeighbor(neighborRight, "right");
                    List<String> leftList = new ArrayList<>();
                    leftList.add(THIS_IP_ADDRESS);
                    leftList.add(node.NodesLeft.get(0));
                    leftList.add(node.NodesLeft.get(1));
                    nodeCommand = HandleApi.createHttpRequest("Get", "newNeighbor", leftList + ",left");
                    Thread nodeThread2 = new Thread(new MyNodeThread());
                    nodeThread2.start();
                }


                // skulle have implmenteret nye naboer for vores ny nabos naboer men er ikke gjort
                nodeCommand = HandleApi.createHttpRequest("Get", "GetPhonebookLeft", "tom");
                Thread nodeThread1 = new Thread(new MyNodeThread());
                nodeThread1.start();
                waitABit();
                waitABit();
                waitABit();
                Response data = HandleApi.readHttpResponse(dataFromOtherNode);
                data.body = data.body.replace("[", "");
                data.body = data.body.replace("]", "");
                data.body = data.body.replace(" ", "");
                String[] dataSplit = data.body.split(",");
                node.newNeighbor(Arrays.asList(dataSplit), "left");
                String leftList = THIS_IP_ADDRESS + ","+ dataSplit[0] + "," + dataSplit[1];
                nodeCommand = HandleApi.createHttpRequest("Get", "GetPhonebookRight", "tom");
                Thread nodeThread2 = new Thread(new MyNodeThread());
                nodeThread2.start();
                waitABit();
                waitABit();
                waitABit();
                data = HandleApi.readHttpResponse(dataFromOtherNode);
                data.body = data.body.replace("[", "");
                data.body = data.body.replace("]", "");
                data.body = data.body.replace(" ", "");
                dataSplit = data.body.split(",");
                List<String> neighborRight = new ArrayList<>();
                neighborRight.add(nodeIp);
                neighborRight.add(dataSplit[0]);
                neighborRight.add(dataSplit[1]);
                node.newNeighbor(neighborRight, "right");
                nodeCommand = HandleApi.createHttpRequest("Get", "newNeighbor", leftList + ",left");
                Thread nodeThread3 = new Thread(new MyNodeThread());
                nodeThread3.start();


            }

        }//run()
    } //class MyNodeThread
    }