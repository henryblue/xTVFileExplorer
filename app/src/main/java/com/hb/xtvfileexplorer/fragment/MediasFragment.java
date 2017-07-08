package com.hb.xtvfileexplorer.fragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hb.xtvfileexplorer.R;
import com.hb.xtvfileexplorer.loader.DirectoryLoader;
import com.hb.xtvfileexplorer.model.DirectoryResult;
import com.hb.xtvfileexplorer.model.DocumentInfo;
import com.hb.xtvfileexplorer.model.RootInfo;
import com.hb.xtvfileexplorer.ui.CompatTextView;


public class MediasFragment extends Fragment {

	private static RootInfo mRootInfo;
	private static DocumentInfo mDocInfo;

	private LoaderManager.LoaderCallbacks<DirectoryResult> mCallbacks;

	private CompatTextView mEmptyView;

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
		mEmptyView = (CompatTextView) view.findViewById(android.R.id.empty);
		return view;
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		final Context context = getActivity();
		mCallbacks = new LoaderManager.LoaderCallbacks<DirectoryResult>() {
			@Override
			public Loader<DirectoryResult> onCreateLoader(int id, Bundle args) {

				Uri contentsUri = DocumentsContract.buildChildDocumentsUri(mDocInfo.authority, mDocInfo.documentId);
				return new DirectoryLoader(context, contentsUri);
			}

			@Override
			public void onLoadFinished(Loader<DirectoryResult> loader, DirectoryResult result) {
				if (!isAdded())
					return;
				//mAdapter.swapResult(result);
			}

			@Override
			public void onLoaderReset(Loader<DirectoryResult> loader) {
				//mAdapter.swapResult(null);
			}
		};
		if (!mRootInfo.isManuGen) {
			//mListView.setAdapter(mAdapter);
			getLoaderManager().restartLoader(40, null, mCallbacks);
		} else {
			mEmptyView.setVisibility(View.VISIBLE);
		}
	}

}