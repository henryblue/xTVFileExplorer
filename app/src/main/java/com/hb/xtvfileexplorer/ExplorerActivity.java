package com.hb.xtvfileexplorer;


import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.hb.xtvfileexplorer.fragment.RootsFragment;
import com.hb.xtvfileexplorer.model.DocumentInfo;
import com.hb.xtvfileexplorer.model.RootInfo;

import java.io.FileNotFoundException;

import static com.hb.xtvfileexplorer.fragment.RootsFragment.TAG;

public class ExplorerActivity extends BaseActivity {

    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explorer);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        RootsFragment.show(getFragmentManager());
    }

    @Override
    public void onRootPicked(RootInfo root, boolean closeDrawer) {
        if(null == root){
            return;
        }

        new PickRootTask(root).execute();

        if (closeDrawer) {
           // setRootsDrawerOpen(false);
        }
    }

    private class PickRootTask extends AsyncTask<Void, Void, DocumentInfo> {
        private RootInfo mRoot;

        public PickRootTask(RootInfo root) {
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
            Log.i(TAG, "onPostExecute: ====" + result.authority);
            Log.i(TAG, "onPostExecute: ===="+ result.displayName);
            Log.i(TAG, "onPostExecute: ====" + result.documentId);
            Log.i(TAG, "onPostExecute: =====" + result.path);
//            if(!Utils.isActivityAlive(ExplorerActivity.this)){
//                return;
//            }
//            if (result != null) {
//                mState.stack.push(result);
//                mState.stackTouched = true;
//                onCurrentDirectoryChanged(ANIM_SIDE);
//            }
        }
    }
}
