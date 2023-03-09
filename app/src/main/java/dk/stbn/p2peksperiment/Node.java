package dk.stbn.p2peksperiment;

import java.util.List;

public class Node {
    List<String> NodesLeft;
    List<String> NodesRight;

    String Id;

    List<String> DataIndex;
    List<String> Data;


    Node(String ip, List<String> nodesLeft, List<String> nodesRight){
        Id = ip;
        NodesLeft = nodesLeft;
        NodesRight = nodesRight;


    }


    String getId(){
        return Id;
    }

    List<String> newNeighbor(List<String> newNeighborIds, String side){
        if(side.equals("left")){
            NodesLeft.addAll(newNeighborIds);
            return NodesLeft;
        }else{
            NodesRight.addAll(newNeighborIds);
            return NodesRight;
        }
    }

    List<String> GetPhonebookLeft(){
        return NodesLeft; //returns nodes left or right;
    }

    List<String> GetPhonebookRight(){
        return NodesRight; //returns nodes left or right;
    }

    String GetData(String dataId){
        for (int i = 0; i < DataIndex.size(); i++) {
            if(dataId.equals(DataIndex.get(i))){
                return Data.get(i);
            }
        }
        return null;
    }

    void RemoveData(String dataId){
        for (int i = 0; i < DataIndex.size(); i++) {
            if(dataId.equals(DataIndex.get(i))){
                Data.remove(i);
                break;
            }
        }
    }

    String AddData(String newData){
        Data.add(newData);
        return "datas index"; //TODO figur way to make this
    }

}
