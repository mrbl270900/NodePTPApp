package dk.stbn.p2peksperiment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.CustomHolder> {

    private Context context;
    private List<Post> Data;

    public CustomAdapter (Context context, List<Post> data) {
        this.context = context;
        this.Data = data;
    }


    @NonNull
    @Override
    public CustomHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.post_layout, parent, false);

        return new CustomHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomAdapter.CustomHolder holder, int position) {
        Post post = Data.get(position);
        holder.setDetails(post);
    }

    @Override
    public int getItemCount() {
        return Data.size();
    }
    class CustomHolder extends RecyclerView.ViewHolder{

        private TextView postSubjekt, postOwner, postContens;

        private Button like, comment;

        private EditText commentInput;

        private RecyclerView commentsView;

        private Post post;
        public CustomHolder(@NonNull View itemView) {
            super(itemView);

            postSubjekt = itemView.findViewById(R.id.PostSubjektTextView);
            postContens = itemView.findViewById(R.id.PostContensTextView);
            postOwner = itemView.findViewById(R.id.OwnerTextView);

            like = itemView.findViewById(R.id.LikeButton);
            like.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(view == like){
                        //replace test with username from var of client
                        if(post.getLikeList().contains("test")){
                            post.addLike("test");
                            like.setText("Unlike");
                        }else{
                            post.removeLike("test");
                            like.setText("Like");
                        }
                    }
                }
            });
            comment = itemView.findViewById(R.id.CommentButton);
            comment.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(view == comment){
                        String input = commentInput.getText().toString();
                        post.addComment("test", input);
                    }
                }
            });

            commentInput = itemView.findViewById(R.id.CommentEditText);

            commentsView = itemView.findViewById(R.id.recyclerView2);
        }

        void setDetails(Post input){
            post = input;
            postSubjekt.setText(input.getContens());
            postContens.setText(input.getSubject() + "\n Likes: " + input.getLikeList().size());
            postOwner.setText("Author: " + input.getOwner());

            //replace test with username from var of client
            if(post.getLikeList().contains("test")){
                like.setText("Unlike");
            }



        }
    }
}