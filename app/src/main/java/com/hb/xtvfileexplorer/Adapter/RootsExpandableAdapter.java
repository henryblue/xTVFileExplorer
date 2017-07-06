package com.hb.xtvfileexplorer.Adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import com.hb.xtvfileexplorer.R;
import com.hb.xtvfileexplorer.fragment.RootsFragment;
import com.hb.xtvfileexplorer.fragment.RootsFragment.Item;
import com.hb.xtvfileexplorer.fragment.RootsFragment.RootItem;
import com.hb.xtvfileexplorer.model.GroupInfo;
import com.hb.xtvfileexplorer.model.RootInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;



public class RootsExpandableAdapter extends BaseExpandableListAdapter {

    private final List<GroupInfo> mGroup = new ArrayList<>();
    private Context mContext;

    public RootsExpandableAdapter(Context context, Collection<RootInfo> roots) {
        mContext = context;
        processRoots(roots);
    }

    private void processRoots(Collection<RootInfo> roots) {
        List<GroupInfo> groupRoots = new ArrayList<>();
        final List<Item> apps = new ArrayList<>();

        for (RootInfo rootInfo : roots) {
            if (rootInfo.isApp()) {
                apps.add(new RootItem(rootInfo));
            }
        }

        if(!apps.isEmpty()){
            String label = mContext.getString(R.string.label_apps);
            groupRoots.add(new GroupInfo(label, apps));
        }

        mGroup.clear();
        mGroup.addAll(groupRoots);
    }

    @Override
    public int getGroupCount() {
        return mGroup.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mGroup.get(groupPosition).itemList.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mGroup.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mGroup.get(groupPosition).itemList.get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                             ViewGroup parent) {
        final RootsFragment.GroupItem item = new RootsFragment.GroupItem((GroupInfo) getGroup(groupPosition));
        return item.getView(convertView, parent);
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        final Item item = (Item) getChild(groupPosition, childPosition);
        return item.getView(convertView, parent);
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    public void setData(Collection<RootInfo> roots){
        processRoots(roots);
        notifyDataSetChanged();
    }
}
