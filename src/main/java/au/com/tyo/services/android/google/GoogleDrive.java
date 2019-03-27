package au.com.tyo.services.android.google;

import android.accounts.Account;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import androidx.fragment.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.GoogleUtils;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Eric Tang (eric.tang@tyo.com.au) on 11/4/17.
 */

public class GoogleDrive extends GoogleGmsDrive {

    private static final int QUERY_TYPE_LIST_ROOT = 0;
    private static final int QUERY_TYPE_LIST_FOLDER = 1;
    private static final int QUERY_TYPE_SHARE_WITH_ME = 2;
    private static final int QUERY_TYPE_SEARCH = 3;

    public static final int COMPLETE_AUTHORIZATION_REQUEST_CODE = 9999;
    private static final String TAG = GoogleDrive.class.getSimpleName();
    private String appName = "CommonServicesDroid";

    GoogleAccountCredential credential;

    Drive service;

    private boolean signInRequestSent = false;

    private static HttpTransport httpTransport = null;

    static {
        createHttpTransport();
    }

    private String pageToken;

    private AsyncTask<String, Void, QueryResult> task;

    private boolean stopTask = false;

    public interface GoogleDriveListener {

        void onPreDataLoading();

        void onPostDataLoading(QueryResult result);
    }

    private GoogleDriveListener listener;

    public GoogleDrive(String appName, GoogleController controller, Activity activity) {
        super(controller, activity);

        this.appName = appName;
        credential = GoogleAccountCredential.usingOAuth2(activity, Arrays.asList(DriveScopes.DRIVE));
    }

    public void setEventListener(GoogleDriveListener listener) {
        this.listener = listener;
    }

    public boolean isSignInRequestSent() {
        return signInRequestSent;
    }

    public void setSignInRequestSent(boolean signInRequestSent) {
        this.signInRequestSent = signInRequestSent;
    }

    static HttpTransport newProxyTransport() {
        NetHttpTransport.Builder builder = new NetHttpTransport.Builder();
        boolean success = true;
        try {
            builder.trustCertificates(GoogleUtils.getCertificateTrustStore());
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "Can't create http transport for Google Drive");
            success = false;
        } catch (IOException e) {
            Log.e(TAG, "Can't create http transport for Google Drive");
            success = false;
        }

        if (!success)
            try {
                builder.doNotValidateCertificate();
            } catch (GeneralSecurityException e) {
                Log.e(TAG, "Can't create http transport for Google Drive");
            }
        //builder.trustCertificates(GoogleUtils.getCertificateTrustStore());
        //builder.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.0.0.1", 8080)));
        return builder.build();
    }

    private static void createHttpTransport() {
        if (null == httpTransport) {
            if (Build.VERSION.SDK_INT >= 23) {
                httpTransport = AndroidHttp.newCompatibleTransport();
            }
            else {
                httpTransport = new ApacheHttpTransport(); //AndroidHttp.newCompatibleTransport(); // newProxyTransport(); //new com.google.api.client.http.javanet.NetHttpTransport(); // GoogleNetHttpTransport.newTrustedTransport();
            }
        }
    }

    public boolean isConnected() {
        return service != null;
    }

    public void connect() {
        if (accountName == null)
            accountName = au.com.tyo.android.AndroidUtils.getUserPrimaryAccount(activity);

        if (null == accountName || accountName.length() == 0) {
            startSignInActivity();
            return;
        }

        if (null == credential.getSelectedAccountName()) {
            credential.setSelectedAccountName(accountName);
            if (null == credential.getSelectedAccountName())
                credential.setSelectedAccount(new Account(accountName, "com.google"));
        }

        if (service == null)
            service = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
                    .setApplicationName(appName)
                    .build();

        createHttpTransport();
    }

    public List list(int type, String id, String extra) throws IOException {
        return retrieveFiles(type, id, extra);
    }

    private List<File> retrieveFiles(int type, String id, String extra) throws IOException {
        Drive.Files.List request = null;

        switch (type) {
            case QUERY_TYPE_LIST_FOLDER:
            case QUERY_TYPE_LIST_ROOT:
            default:
                request = setQueryWithFileId(id, extra);
                break;
            case QUERY_TYPE_SHARE_WITH_ME:
                request = setQueryShareWithMe(null, extra);
                break;
            case QUERY_TYPE_SEARCH:
                request = setQueryWithSearchString(id, extra);
                break;
        }

        return execute(request);
    }


    private List<File> execute(Drive.Files.List request) throws IOException {
        List<File> result = new ArrayList<>();
        do {
            try {
                FileList files = request.execute();

                result.addAll(files.getFiles());
                request.setPageToken(pageToken);
            } catch (IOException e) {
                if (e instanceof UserRecoverableAuthIOException)
                    throw e;

//                System.out.println("An error occurred: " + e);
//                String message = e.getMessage() != null ? e.getMessage() : "Something wrong in getting the file list from Google server: " + e.getClass().getSimpleName();
//                Toast.makeText(activity, message, Toast.LENGTH_LONG);
                request.setPageToken(null);
            }
        } while (request.getPageToken() != null &&
                request.getPageToken().length() > 0);

        return result;
    }

    private Drive.Files.List setQueryWithSearchString(String searchString, String extra) throws IOException {
        return setQuery("name contains '" + searchString + "'");
    }

    private Drive.Files.List setQueryWithFileId(String id, String extra) throws IOException {
        return setQueryWithExtra(id, extra);
    }

    private Drive.Files.List setQueryWithExtra(String id, String query) throws IOException {
        return setQuery("'" + id +"' in parents " + (query.length() > 0 ? query : "" ));
    }

    private Drive.Files.List setQuery(String query) throws IOException {
        Drive.Files.List request = createRequest();

        request.setQ(query);

        request.setSpaces("drive")
                .setFields("nextPageToken, files(id, name, size, webContentLink, mimeType, thumbnailLink, webViewLink)")
                .setPageToken(pageToken)
                ;
        return request;
    }

    /**
     * if the id == null, look for the folder that shared with me
     *
     * @param query
     * @param extra
     */
    private Drive.Files.List setQueryShareWithMe(String query, String extra) throws IOException {
        return setQuery("sharedWithMe=true and name contains '" + (query != null && query.length() > 0 ? query : "Videos") + "'");
    }

    private Drive.Files.List createRequest() throws IOException {
        return service.files().list();
    }

    public void cancel() {
        stopTask = true;
    }

    public boolean isTaskStopped() {
        return stopTask;
    }

    public void onPause() {
        cancel();
    }

    public interface QueryCaller {

        void onQueryFinished(QueryResult result);

        void onBeforeQuery();
    }

    public static class QueryResult {

        public enum StatusCode {SUCCESS, REQUIRE_SIGNIN, FAILED};

        public List list;
        public StatusCode status;
        public String message;
        public Intent intent;
        public String query;
        public int queryType;
    }

    @Override
    public void signIn() {
        queryRoot();
    }

    /**
     * As for search, particular in the opensearch, it is handled in the background already
     *
     * @param name
     */
    public List search(String name) {
        QueryResult result = executeQuery(QUERY_TYPE_SEARCH, name, false, "", null);
        return result.list;
    }

    public void queryRoot() {
        executeQuery(QUERY_TYPE_LIST_ROOT, "root", "or sharedWithMe=true");
    }

    public void query(String folderId) {
        query(folderId, null);
    }

    public void query(String folderId, QueryCaller caller) {
        executeQuery(QUERY_TYPE_LIST_FOLDER, folderId, caller);
    }

    private QueryResult executeQuery(int type, String folderId, QueryCaller caller) {
        return executeQuery(type, folderId, true, "", caller);
    }

    private QueryResult executeQuery(int type, String folderId, String extra) {
        return executeQuery(type, folderId, true, extra, null);
    }

    private QueryResult executeQuery(int type, String folderId, String extra, QueryCaller caller) {
        return executeQuery(type, folderId, true, extra, caller);
    }

    private QueryResult executeQuery(int type, String query, boolean runInBackgroud, String extra, QueryCaller caller) {
        if (service == null) {
            connect();

            // if there is no account logged in we will just not proceed
            if (service == null)
                return null;
        }

        if (runInBackgroud) {
            if (!stopTask) {
                task = new BackgroudTask(type, caller);
                task.execute(query);
            }
            return null;
        }
        return sendQuery(type, query, extra);
    }

    public QueryResult sendQuery(int type, String query, String extra) {
        QueryResult result = new QueryResult();
        try {
            result.queryType = type;
            result.query = query;
            result.list = list(type, query, extra);
            result.status = QueryResult.StatusCode.SUCCESS;
        } catch (IOException e) {
            if (e instanceof UserRecoverableAuthIOException) {
                result.status = QueryResult.StatusCode.REQUIRE_SIGNIN;
                result.intent = ((UserRecoverableAuthIOException) e).getIntent();
            }
            else {
                e.printStackTrace();
                Log.e(TAG, "Can't list files");
                result.status = QueryResult.StatusCode.FAILED;
                result.message = e.getMessage();
            }
        }
        return result;
    }

    protected void handleGoogleAccount(Activity activity, GoogleSignInAccount gsa) {
        super.handleGoogleAccount(activity, gsa);
        connect();
    }

    public void loadFilesForDirectory(File file) {
        loadFilesForDirectory(file, null);
    }

    public void loadFilesForDirectory(File file, QueryCaller caller) {
//        File file = category.getFile();
        executeQuery(QUERY_TYPE_LIST_FOLDER, file.getId(), caller);
    }

    /**
     *
     * @param useDirectDownload
     * @param fileToDownload
     * @param toFolder
     * @param listener
     * @throws IOException
     */
    public String downloadFile(boolean useDirectDownload, File fileToDownload, String toFolder, FileDownloadProgressListener listener)
            throws IOException {
        // create parent directory (if necessary)
        java.io.File parentDir = new java.io.File(toFolder);
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Unable to create parent directory");
        }

        java.io.File targetFile = new java.io.File(parentDir, fileToDownload.getName());
        OutputStream out = new FileOutputStream(targetFile);
        String url = fileToDownload.getWebContentLink();

        MediaHttpDownloader downloader =
                new MediaHttpDownloader(httpTransport, service.getRequestFactory().getInitializer());
        downloader.setDirectDownloadEnabled(useDirectDownload);
        downloader.setProgressListener(listener);
        downloader.download(new GenericUrl(url), out);
        return targetFile.getCanonicalPath();
    }

    public String download(boolean useDirectDownload, File fileToDownload, String toFolder, FileDownloadProgressListener listener) throws IOException {
        java.io.File parentDir = new java.io.File(toFolder);
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Unable to create parent directory");
        }

        java.io.File targetFile = new java.io.File(parentDir, fileToDownload.getName());
        OutputStream out = new FileOutputStream(targetFile);
        Drive.Files.Get request = service.files().get(fileToDownload.getId());
        request.getMediaHttpDownloader().setProgressListener(listener);
        request.executeMediaAndDownloadTo(out);
        return targetFile.getCanonicalPath();
    }

    public void onActivityResult(FragmentActivity activity, int requestCode, int resultCode, Intent data) {
        super.onActivityResult(activity, requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN || requestCode == COMPLETE_AUTHORIZATION_REQUEST_CODE) {
            signIn();
        }
    }

    private class BackgroudTask extends AsyncTask<String, Void, QueryResult> {

        private final int type;

        private QueryCaller caller;

        public BackgroudTask(int type, QueryCaller caller) {
            this(type);
            this.caller = caller;
        }

        public BackgroudTask(int type) {
            this.type = type;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (caller != null)
                caller.onBeforeQuery();
            else if (null != listener)
                listener.onPreDataLoading();
        }

        @Override
        protected QueryResult doInBackground(String... params) {
            String id = null;
            if (params.length > 0)
                id = params[0];
            String extra = "";
            if (params.length > 1)
                extra = params[1];
            return sendQuery(this.type, id, extra);
        }

        @Override
        protected void onPostExecute(QueryResult result) {
            if (stopTask)
                return;

            // caller has the priority and it is exclusive with listener
            if (caller != null)
                caller.onQueryFinished(result);
            else if (null != listener)
                listener.onPostDataLoading(result);
        }
    }
}
