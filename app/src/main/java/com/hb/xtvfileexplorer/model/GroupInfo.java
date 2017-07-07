package com.hb.xtvfileexplorer.model;


import com.hb.xtvfileexplorer.fragment.RootsFragment.Item;

import java.util.List;

;

public class GroupInfo {
    public String label;
    public List<Item> itemList;

    public GroupInfo(String text, List<Item> list){
        label = text;
        itemList = list;
    }
}
