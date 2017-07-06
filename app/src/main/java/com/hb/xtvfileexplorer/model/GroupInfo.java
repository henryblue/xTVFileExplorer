package com.hb.xtvfileexplorer.model;


import com.hb.xtvfileexplorer.fragment.RootsFragment;

import java.util.List;

public class GroupInfo {
    public String label;
    public List<RootsFragment.Item> itemList;

    public GroupInfo(String text, List<RootsFragment.Item> list){
        label = text;
        itemList = list;
    }
}
