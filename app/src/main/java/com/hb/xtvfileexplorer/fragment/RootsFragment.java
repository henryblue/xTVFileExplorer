package com.hb.xtvfileexplorer.fragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.hb.xtvfileexplorer.Adapter.RootsExpandableAdapter;
import com.hb.xtvfileexplorer.BaseActivity;
import com.hb.xtvfileexplorer.R;
import com.hb.xtvfileexplorer.loader.RootsLoader;
import com.hb.xtvfileexplorer.model.GroupInfo;
import com.hb.xtvfileexplorer.model.RootInfo;
import com.hb.xtvfileexplorer.utils.Utils;

import java.util.Collection;
import java.util.Objects;


public class RootsFragment extends Fragment {

    public static final String TAG = "RootsFragment";
    private ExpandableListView mList;
    private RootsExpandableAdapter mAdapter;
    private LoaderManager.LoaderCallbacks<Collection<RootInfo>> mCallbacks;

    public static void show(FragmentManager fm) {

        final RootsFragment fragment = new RootsFragment();

        final FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_roots, fragment);
        ft.commitAllowingStateLoss();
    }

    public static RootsFragment get(FragmentManager fm) {
        return (RootsFragment) fm.findFragmentById(R.id.container_roots);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_roots, container, false);
        mList = (ExpandableListView) view.findViewById(android.R.id.list);
        mList.setOnChildClickListener(mItemListener);
        mList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = Utils.dpToPx(302);

        boolean rtl = Utils.isRTL();
        int leftPadding = rtl ? 10 : 50;
        int rightPadding = rtl ? 50 : 10;
        int leftWidth = width - Utils.dpToPx(leftPadding);
        int rightWidth = width - Utils.dpToPx(rightPadding);

        mList.setIndicatorBoundsRelative(leftWidth, rightWidth);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Context context = getActivity();
        mCallbacks = new LoaderManager.LoaderCallbacks<Collection<RootInfo>>() {

            @Override
            public Loader<Collection<RootInfo>> onCreateLoader(int id, Bundle args) {
                return new RootsLoader(context);
            }

            @Override
            public void onLoadFinished(Loader<Collection<RootInfo>> loader, Collection<RootInfo> data) {
                if (mAdapter == null) {
                    mAdapter = new RootsExpandableAdapter(context, data);
                    Parcelable state = mList.onSaveInstanceState();
                    mList.setAdapter(mAdapter);
                    mList.onRestoreInstanceState(state);
                } else {
                    mAdapter.setData(data);
                }

                int groupCount = mAdapter.getGroupCount();
                for (int i = 0; i < groupCount; i++) {
                    mList.expandGroup(i);
                }
                mList.requestFocus();
            }

            @Override
            public void onLoaderReset(Loader<Collection<RootInfo>> loader) {
                mAdapter = null;
                mList.setAdapter((BaseExpandableListAdapter)null);
            }
        };
        getLoaderManager().restartLoader(2, null, mCallbacks);
    }

    public void onCurrentRootChanged() {
        if (mAdapter == null || mList == null) return;

        final RootInfo root = ((BaseActivity) getActivity()).getCurrentRoot();
        for (int i = 0; i < mAdapter.getGroupCount(); i++) {
            for (int j = 0; j < mAdapter.getChildrenCount(i); j++) {
                final Object item = mAdapter.getChild(i,j);
                if (item instanceof RootItem) {
                    final RootInfo testRoot = ((RootItem) item).root;
                    if (Objects.equals(testRoot, root)) {
                        try {
                            long id = ExpandableListView.getPackedPositionForChild(i, j);
                            int index = mList.getFlatListPosition(id);
                            mList.setItemChecked(index, true);
                        } catch (Exception e){
                            //
                        }
                        return;
                    }
                }
            }
        }
    }

    private ExpandableListView.OnChildClickListener mItemListener = new ExpandableListView.OnChildClickListener() {
        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                    int childPosition, long id) {
            final BaseActivity activity = BaseActivity.get(RootsFragment.this);
            final Item item = (Item) mAdapter.getChild(groupPosition, childPosition);
            if (item instanceof RootItem) {
                int index = parent.getFlatListPosition(
                        ExpandableListView.getPackedPositionForChild(groupPosition, childPosition));
                parent.setItemChecked(index, true);
                activity.onRootPicked(((RootItem) item).root, true);
            } else {
                throw new IllegalStateException("Unknown root: " + item);
            }
            return false;
        }
    };

    public static abstract class Item {
        private final int mLayoutId;

        Item(int layoutId) {
            mLayoutId = layoutId;
        }

        public View getView(View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(mLayoutId, parent, false);
            }
            bindView(convertView);
            return convertView;
        }

        public abstract void bindView(View convertView);
    }

    public static class RootItem extends Item {
        final RootInfo root;

        public RootItem(RootInfo root) {
            super(R.layout.item_root);
            this.root = root;
        }

        @Override
        public void bindView(View convertView) {
            final ImageView icon = (ImageView) convertView.findViewById(android.R.id.icon);
            final TextView title = (TextView) convertView.findViewById(android.R.id.title);

            final Context context = convertView.getContext();
            icon.setImageDrawable(root.loadDrawerIcon(context));
            title.setText(root.title);
        }
    }

    public static class GroupItem {
        final String mLabel;
        private final int mLayoutId;

        public GroupItem(GroupInfo groupInfo) {
            mLabel = groupInfo.label;
            mLayoutId = R.layout.item_root_header;
        }

        public View getView(View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(mLayoutId, parent, false);
            }
            bindView(convertView);
            return convertView;
        }

        void bindView(View convertView) {
            final TextView title = (TextView) convertView.findViewById(android.R.id.title);
            title.setText(mLabel);
        }

    }
}
