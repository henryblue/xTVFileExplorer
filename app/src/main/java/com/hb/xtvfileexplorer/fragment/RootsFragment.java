package com.hb.xtvfileexplorer.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import com.hb.xtvfileexplorer.Adapter.RootsExpandableAdapter;
import com.hb.xtvfileexplorer.R;


public class RootsFragment extends Fragment {

    private ExpandableListView mList;
    private RootsExpandableAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_roots, container, false);
        mList = (ExpandableListView) view.findViewById(android.R.id.list);
        mList.setOnChildClickListener(mItemListener);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new RootsExpandableAdapter();
        mList.setAdapter(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private ExpandableListView.OnChildClickListener mItemListener = new ExpandableListView.OnChildClickListener() {
        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                    int childPosition, long id) {
            return false;
        }
    };
}
