package dk.stbn.p2peksperiment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.CustomHolder> {

    private Context context;
    private List<Post> Data;

    private User user;

    public CustomAdapter (Context context, List<Post> data, User user) {
        this.context = context;
        this.Data = data;
        this.user = user;
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

        holder.postSubjekt.setText(post.getContens());
        holder.postContens.setText(post.getSubject() + "\n Likes: " + post.getLikeList().size());
        holder.postOwner.setText("Author: " + post.getOwner());

        if (post.getLikeList().contains(user.getUsername())) {
            holder.like.setText("Unlike");
        }

            holder.like.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (view == holder.like) {
                        //replace test with username from var of client
                        if (!post.getLikeList().contains(user.getUsername())) {
                            post.addLike(user.getUsername());
                            holder.like.setText("Unlike");
                            Data.set(post.getId(), post);
                            notifyItemChanged(holder.getAdapterPosition(), 1);
                        } else {
                            post.removeLike(user.getUsername());
                            holder.like.setText("Like");
                            Data.set(post.getId(), post);
                            notifyItemChanged(holder.getAdapterPosition(), 1);
                        }
                    }
                }
            });

            holder.comment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (view == holder.comment) {
                        String input = holder.commentInput.getText().toString();
                        post.addComment(user.getUsername(), input);
                        Data.set(post.getId(), post);
                        System.out.println(input + holder.getAdapterPosition());
                        notifyItemChanged(holder.getAdapterPosition(), 1);
                    }
                }
            });
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
            comment = itemView.findViewById(R.id.CommentButton);

            commentInput = itemView.findViewById(R.id.CommentEditText);

            commentsView = itemView.findViewById(R.id.recyclerView2);
        }
    }
}