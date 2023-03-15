package dk.stbn.p2peksperiment;

import java.util.HashMap;
import java.util.List;

public class Node {
    List<String> NodesLeft;
    List<String> NodesRight;
    SHA256 hash = new SHA256();
    String Id;
    HashMap<String, String> Data = new HashMap<>();
    HashMap<String, String> DatasOwner = new HashMap<>();

    Node(String ip, List<String> nodesLeft, List<String> nodesRight) {
        Id = ip;
        NodesLeft = nodesLeft;
        NodesRight = nodesRight;
    }


    String getId() {
        return Id;
    }

    List<String> newNeighbor(List<String> newNeighborIds, String side) {
        if (side.equalsIgnoreCase("left")) {
            NodesLeft.clear();
            NodesLeft.addAll(newNeighborIds);
            return NodesLeft;
        } else {
            NodesRight.clear();
            NodesRight.addAll(newNeighborIds);
            return NodesRight;
        }
    }

    List<String> GetPhonebookLeft() {
        return NodesLeft; //returns nodes left or right;
    }

    List<String> GetPhonebookRight() {
        return NodesRight; //returns nodes left or right;
    }

    String GetData(String dataId) {
        try {
            if (Data.containsKey(dataId)) {
                return Data.get(dataId);
            } else {
                return null;
            }
        }catch (RuntimeException e){
            System.out.println(dataId);
            return null;
        }
    }

    String RemoveData(String dataId) {
        if (Data.containsKey(dataId)) {
            DatasOwner.remove(dataId);
            return Data.remove(dataId);
        } else {
            return null;
        }
    }

    String AddData(String newData, String ownerIp) {
            String hashedData = hash.hash(newData);
            DatasOwner.put(ownerIp ,hashedData);
            Data.put(hashedData, newData);
            return hashedData;
    }
    String AddData(String newData) {
        String hashedData = hash.hash(newData);
        Data.put(hashedData, newData);
        return hashedData;
    }
}
