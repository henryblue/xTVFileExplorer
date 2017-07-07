package com.hb.xtvfileexplorer.fragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hb.xtvfileexplorer.R;
import com.hb.xtvfileexplorer.model.DocumentInfo;
import com.hb.xtvfileexplorer.model.RootInfo;


public class MediasFragment extends Fragment {

	private static RootInfo mRootInfo;
	private static DocumentInfo mDocInfo;

	public static void show(FragmentManager fm, RootInfo root, DocumentInfo doc) {
		mRootInfo = root;
		mDocInfo = doc;

		final FragmentTransaction ft = fm.beginTransaction();
		final MediasFragment fragment = new MediasFragment();
		ft.replace(R.id.container_directory, fragment);
		ft.commitAllowingStateLoss();
	}

	public static Fragment get(FragmentManager fm) {
		return fm.findFragmentById(R.id.container_directory);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final Context context = inflater.getContext();
        final Resources res = context.getResources();
		final View view = inflater.inflate(R.layout.fragment_media, container, false);


		return view;
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		final Context context = getActivity();

	}

}