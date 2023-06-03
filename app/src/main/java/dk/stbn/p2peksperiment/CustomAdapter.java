package dk.stbn.p2peksperiment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.CustomHolder> {

    private Context context;
    private Network Data;
    private List<Post> postList;
    private User user;

    public CustomAdapter (Context context, Network data, User user) {
        this.context = context;
        this.Data = data;
        postList = data.getPostList();
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
        Post post = postList.get(position);

        holder.postSubjekt.setText(post.getContens());
        holder.postContens.setText(post.getSubject() + "\n Likes: " + post.getLikeList().size());
        holder.postOwner.setText("Author: " + post.getOwner());

        holder.commentsView.setAdapter(new CommentsAdapter(context, post.getComments()));
        holder.commentsView.addItemDecoration(new DividerItemDecoration(context,
                LinearLayoutManager.HORIZONTAL));

        if (post.getLikeList().contains(user.getUsername())) {
            holder.like.setText("Unlike");
        }

            holder.commentsView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (view.getId() == holder.commentsView.getId()) {
                        if (MotionEventCompat.getActionMasked(motionEvent) == MotionEvent.ACTION_UP) {
                            view.getParent().requestDisallowInterceptTouchEvent(false);
                        } else {
                            view.getParent().requestDisallowInterceptTouchEvent(true);
                        }
                    }

                    return view.onTouchEvent(motionEvent);
                }
            });

            holder.like.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (view == holder.like) {
                        //replace test with username from var of client
                        if (!post.getLikeList().contains(user.getUsername())) {
                            post.addLike(user.getUsername());
                            holder.like.setText("Unlike");
                            postList.set(post.getId(), post);
                            notifyItemChanged(holder.getAdapterPosition(), 1);
                        } else {
                            post.removeLike(user.getUsername());
                            holder.like.setText("Like");
                            postList.set(post.getId(), post);
                            notifyItemChanged(holder.getAdapterPosition(), 1);
                        }

                        MainActivity.runClientCommand(HandleApi.createHttpRequest("newData",
                                Data.getPostListString()), Data.networkCode);
                    }
                }
            });

            holder.comment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (view == holder.comment) {
                        String input = holder.commentInput.getText().toString();
                        post.addComment(user.getUsername(), input);
                        postList.set(post.getId(), post);
                        System.out.println(input + holder.getAdapterPosition());
                        notifyItemChanged(holder.getAdapterPosition(), 1);
                    }

                    MainActivity.runClientCommand(HandleApi.createHttpRequest("newData",
                            Data.getPostListString()), Data.networkCode);
                }
            });
    }

    @Override
    public int getItemCount() {
        return postList.size();
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

            commentsView.setLayoutManager(new LinearLayoutManager(context));

            DefaultItemAnimator animator = (DefaultItemAnimator) commentsView.getItemAnimator();
            animator.setSupportsChangeAnimations(false);
        }
    }
}