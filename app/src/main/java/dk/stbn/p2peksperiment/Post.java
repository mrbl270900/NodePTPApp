package dk.stbn.p2peksperiment;

import java.util.List;

public class Post {

    private String owner;

    private int id;

    private String subject;

    private List<Comment> commentsList;

    private String contens;


    public Post(String Owner, String Subject, int Id, String contens){
        this.owner = Owner;
        this.id = Id;
        this.subject = Subject;
        this.contens = contens;
    }

    public void addComment(String inputOwner, String inputContens){
        commentsList.add(new Comment(inputOwner, inputContens, commentsList.toArray().length));
    }

    public String getOwner(){
        return this.owner;
    }

    public String getContens(){
        return this.contens;
    }
    public String getSubject(){
        return this.subject;
    }

    public int getId(){
        return this.id;
    }

    public List<Comment> getComments(){
        return this.commentsList;
    }


    private class Comment{
        private String owner;
        private String contens;
        private int id;

        public Comment(String inputOwner, String inputContens, int inputId){
            owner = inputOwner;
            contens = inputContens;
            id = inputId;
        }
    }


}
