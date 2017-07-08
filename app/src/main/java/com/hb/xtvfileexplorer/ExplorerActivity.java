package com.hb.xtvfileexplorer;


import android.app.FragmentManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.hb.xtvfileexplorer.fragment.AppsFragment;
import com.hb.xtvfileexplorer.fragment.MediasFragment;
import com.hb.xtvfileexplorer.fragment.RootsFragment;
import com.hb.xtvfileexplorer.model.DocumentInfo;
import com.hb.xtvfileexplorer.model.RootInfo;
import com.hb.xtvfileexplorer.utils.Utils;

import java.io.FileNotFoundException;

import static com.hb.xtvfileexplorer.fragment.RootsFragment.TAG;

public class ExplorerActivity extends BaseActivity {

    private Toolbar mToolbar;
    private RootInfo mRoot;
    private DocumentInfo mDocInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explorer);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        RootsFragment.show(getFragmentManager());
    }

    @Override
    public RootInfo getCurrentRoot() {
        return mRoot;
    }

    @Override
    public void onRootPicked(RootInfo root, boolean closeDrawer) {
        if(null == root){
            return;
        }
        mRoot = root;
        new PickRootTask(root).execute();

        if (closeDrawer) {
           // setRootsDrawerOpen(false);
        }
    }

    public void onCurrentDirectoryChanged() {
        if(!Utils.isActivityAlive(ExplorerActivity.this)){
            return;
        }
        final FragmentManager fm = getFragmentManager();
        final RootInfo root = mRoot;
        DocumentInfo cwd = mDocInfo;

        if(cwd == null){
            final Uri uri = DocumentsContract.buildDocumentUri(
                    root.getAuthority(), root.getDocumentId());
            DocumentInfo result;
            try {
                result = DocumentInfo.fromUri(getContentResolver(), uri);
                if (result != null) {
                    cwd = result;
                }
            } catch (FileNotFoundException e) {
                //
            }
        }
        if (mRoot.isApp()) {
            AppsFragment.show(fm, root, cwd);
        } else if (mRoot.isLibraryMedia()){
            MediasFragment.show(fm, root, cwd);
        }

        final RootsFragment roots = RootsFragment.get(fm);
        if (roots != null) {
            roots.onCurrentRootChanged();
        }
    }

    private class PickRootTask extends AsyncTask<Void, Void, DocumentInfo> {
        private RootInfo mRoot;

        PickRootTask(RootInfo root) {
            mRoot = root;
        }

        @Override
        protected DocumentInfo doInBackground(Void... params) {
            try {
                final Uri uri = DocumentsContract.buildDocumentUri(
                        mRoot.getAuthority(), mRoot.getDocumentId());
                return DocumentInfo.fromUri(getContentResolver(), uri);
            } catch (FileNotFoundException e) {
                Log.w(TAG, "Failed to find root", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(DocumentInfo result) {
            mDocInfo = result;
            if(!Utils.isActivityAlive(ExplorerActivity.this)){
                return;
            }
            onCurrentDirectoryChanged();
        }
    }
}
