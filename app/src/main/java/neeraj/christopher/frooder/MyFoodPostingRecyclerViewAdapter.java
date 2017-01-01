package neeraj.christopher.frooder;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import neeraj.christopher.frooder.FoodPostingFragment.OnListFragmentInteractionListener;
import neeraj.christopher.frooder.dummy.DummyContent.DummyItem;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link DummyItem} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MyFoodPostingRecyclerViewAdapter extends RecyclerView.Adapter<MyFoodPostingRecyclerViewAdapter.ViewHolder> {

    private List<FoodPosting> mValues;
    private final OnListFragmentInteractionListener mListener;

    public MyFoodPostingRecyclerViewAdapter(List<FoodPosting> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    public void resetList(List<FoodPosting> foods) {
        mValues = foods;
        this.notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_foodposting, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mTimeView.setText(mValues.get(position).getTime());
        holder.mTitleView.setText(mValues.get(position).getTitle());
        holder.mBodyView.setText(mValues.get(position).getBody());

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
//        public final TextView mPhotoView;
        public final TextView mTitleView;
        public final TextView mTimeView;
        public final TextView mBodyView;
        public FoodPosting mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mTimeView = (TextView) view.findViewById(R.id.food_time);
            mTitleView = (TextView) view.findViewById(R.id.food_title);
            mBodyView = (TextView) view.findViewById(R.id.food_body);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTitleView.getText() + "'";
        }
    }
}
