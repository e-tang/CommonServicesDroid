package au.com.tyo.services.android.sn.google;

import android.app.Activity;
import android.app.PendingIntent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.query.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Eric Tang (eric.tang@tyo.com.au) on 7/4/17.
 */

public class GoogleGmsDrive extends GoogleGmsClient {

    private static final String TAG = "GoogleDrive";

    List buffers;

    public GoogleGmsDrive(GoogleController controller, Activity activity) {
        super(controller, activity);
        buffers = new ArrayList();
    }

    protected void createSignInOptions() {
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Scopes.DRIVE_FILE), new Scope(Scopes.DRIVE_APPFOLDER))
                .requestEmail()
                .build();
    }

//    @Override
//    public void createGoogleClient(FragmentActivity context) {
//        if (mGoogleApiClient == null) {
//            mGoogleApiClient = new GoogleApiClient.Builder(context)
//                    .addApi(Drive.API)
//                    .addScope(Drive.SCOPE_FILE)
//                    .addScope(Drive.SCOPE_APPFOLDER) // required for App Folder sample
//                    .addConnectionCallbacks(this)
//                    .addOnConnectionFailedListener(this)
//                    .build();
//        }
//    }

//    @Override
//    public void signInCheck() {
//
//    }

//    private DataBufferAdapter<Metadata> mResultsAdapter;
    private String mNextPageToken;
    private boolean mHasMore;

    @Override
    public void signIn() {
       connect();
    }

    @Override
    public void signOut() {
        disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        super.onConnected(bundle);

        getFirstBatchMetadata();
    }

    /**
     * Retrieves results for the next page. For the first run,
     * it retrieves results for the first page.
     */
    private void retrieveNextPage() {
        // if there are no more results to retrieve,
        // return silently.
        if (!mHasMore) {
            return;
        }
        // retrieve the results for the next page.
        Query query = new Query.Builder()
                .setPageToken(mNextPageToken)
                .build();
        Drive.DriveApi.query(getGoogleApiClient(), query)
                .setResultCallback(metadataBufferCallback);
    }


    public void getFirstBatchMetadata() {
        mHasMore = true; // initial request assumes there are files results.

//        while(mHasMore)
        // retrieveNextPage();
        // loadFilesForDirectory("0B4fw3ZVb7M-rS2RqckVTSFZocVU");
    }

    /**
     * Appends the retrieved results to the result buffer.
     */
    private final ResultCallback<DriveApi.MetadataBufferResult> metadataBufferCallback = new
            ResultCallback<DriveApi.MetadataBufferResult>() {
                @Override
                public void onResult(DriveApi.MetadataBufferResult result) {
                    if (!result.getStatus().isSuccess()) {
                        int code = result.getStatus().getStatusCode();
                        Log.e(TAG, "Problem while retrieving files");

                        if (code == CommonStatusCodes.SIGN_IN_REQUIRED || code == ConnectionResult.SIGN_IN_REQUIRED)
                            try {
                                // Perform the operation associated with our pendingIntent
                                result.getStatus().getResolution().send(RESULT_OK, onSignInFinished, null);
                            } catch (PendingIntent.CanceledException e) {
                                e.printStackTrace();
                            }

                        return;
                    }

                    controller.onGoogleDriveFilesMetadataReceived(result.getMetadataBuffer());
//                    buffers.add();
                    //mResultsAdapter.append(result.getMetadataBuffer());
                    mNextPageToken = result.getMetadataBuffer().getNextPageToken();
                    mHasMore = mNextPageToken != null;
                }
            };


    public void loadFilesForDirectory(DriveId driveId) {
        loadFilesForDirectory(driveId.encodeToString());
    }

    public void loadFilesForDirectory(String resId) {
        Drive.DriveApi.fetchDriveId(getGoogleApiClient(), resId)
                .setResultCallback(idCallback);
    }

    final private ResultCallback<DriveApi.DriveIdResult> idCallback = new ResultCallback<DriveApi.DriveIdResult>() {
        @Override
        public void onResult(DriveApi.DriveIdResult result) {
            if (!result.getStatus().isSuccess()) {
                showMessage("Cannot find DriveId. Are you authorized to view this file?");
                return;
            }
            DriveId driveId = result.getDriveId();
            DriveFolder folder = driveId.asDriveFolder();
            folder.listChildren(getGoogleApiClient())
                    .setResultCallback(metadataBufferCallback );
        }
    };

    private void showMessage(String s) {
        Log.i(TAG, s);
    }

    final private ResultCallback<DriveApi.MetadataBufferResult> metadataResult = new
            ResultCallback<DriveApi.MetadataBufferResult>() {
                @Override
                public void onResult(DriveApi.MetadataBufferResult result) {
                    if (!result.getStatus().isSuccess()) {
                        showMessage("Problem while retrieving files");
                        return;
                    }
//                    mResultsAdapter.clear();
//                    mResultsAdapter.append(result.getMetadataBuffer());
                    controller.onGoogleDriveFilesMetadataReceived(result.getMetadataBuffer());
                    showMessage("Successfully listed files.");
                }
            };

    public void loadFilesForDirectory(Metadata metadata) {
        loadFilesForDirectory(metadata.getDriveId());
    }
}
