package com.hb.xtvfileexplorer.fragment;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import com.hb.xtvfileexplorer.Adapter.RootsExpandableAdapter;
import com.hb.xtvfileexplorer.R;
import com.hb.xtvfileexplorer.model.RootInfo;

import java.util.Collection;


public class RootsFragment extends Fragment {

    private static final String TAG = "RootsFragment";
    private ExpandableListView mList;
    private RootsExpandableAdapter mAdapter;
    private LoaderManager.LoaderCallbacks<Collection<RootInfo>> mCallbacks;

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
        final Context context = getActivity();

        mCallbacks = new LoaderManager.LoaderCallbacks<Collection<RootInfo>>() {

            @Override
            public Loader<Collection<RootInfo>> onCreateLoader(int id, Bundle args) {
                Log.i(TAG, "onCreateLoader: ================================");
                return new RootsLoader(context);
            }

            @Override
            public void onLoadFinished(Loader<Collection<RootInfo>> loader, Collection<RootInfo> data) {
                Log.i(TAG, "onLoadFinished: ==============================");
                for (RootInfo info : data) {
                    Log.i(TAG, "onLoadFinished: ==" + info.getAuthority());
                    Log.i(TAG, "onLoadFinished: ==" + info.getDocumentId());
                    Log.i(TAG, "onLoadFinished: ==" + info.getMimeTypes());
                    Log.i(TAG, "onLoadFinished: ==" + info.getTitle());
                    Log.i(TAG, "onLoadFinished: ==" + info.getAvailableBytes());
                    Log.i(TAG, "onLoadFinished: ==============================");
                }
            }

            @Override
            public void onLoaderReset(Loader<Collection<RootInfo>> loader) {
                Log.i(TAG, "onLoaderReset: ==================================");
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(2, null, mCallbacks);
    }

    private ExpandableListView.OnChildClickListener mItemListener = new ExpandableListView.OnChildClickListener() {
        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                    int childPosition, long id) {
            return false;
        }
    };
}
