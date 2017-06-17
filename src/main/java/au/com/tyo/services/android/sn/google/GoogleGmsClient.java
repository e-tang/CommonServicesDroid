package au.com.tyo.services.android.sn.google;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResolvingResultCallbacks;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;

/**
 * Created by Eric Tang (eric.tang@tyo.com.au) on 7/4/17.
 */

public class GoogleGmsClient implements
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "Google";
    protected final GoogleController controller;
    protected final Activity activity;

    /**
     * Google Stuff
     */
    protected GoogleApiClient mGoogleApiClient;
    protected GoogleSignInOptions gso;
    private boolean mIsResolving = false;
    protected Credential mCredential;
    protected Credential mCredentialToSave;
    protected String accountName;

    public  static final int RC_SIGN_IN = 9001;

    private static final String KEY_IS_RESOLVING = "is_resolving";
    private static final String KEY_CREDENTIAL = "key_credential";
    private static final String KEY_CREDENTIAL_TO_SAVE = "key_credential_to_save";

    // private static final int RC_SIGN_IN = 1;
    private static final int RC_CREDENTIALS_READ = 2;
    private static final int RC_CREDENTIALS_SAVE = 3;

    /** Standard activity result: operation canceled. */
    public static final int RESULT_CANCELED    = 0;
    /** Standard activity result: operation succeeded. */
    public static final int RESULT_OK           = -1;
    /** Start of user-defined activity results. */
    public static final int RESULT_FIRST_USER   = 1;

    private boolean connected;

    public GoogleGmsClient(GoogleController controller, Activity activity) {
        this.activity = activity;
        this.controller = controller;
        this.connected = false;
    }

    /**
     * Getter for the {@code GoogleApiClient}.
     */
    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    protected void createSignInOptions() {
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
    }

    public void createGoogleClient(FragmentActivity context) {
        createSignInOptions();

        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .enableAutoManage(context /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Auth.CREDENTIALS_API)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .addApi(Drive.API)
                .build();
    }

    protected final PendingIntent.OnFinished onSignInFinished = new PendingIntent.OnFinished() {
        public void onSendFinished(PendingIntent pi, Intent intent,
                                   int resultCode, String resultData, Bundle resultExtras) {
            if (resultCode == RC_SIGN_IN) {
                Log.i(TAG, "Signed in successfully");
                onSignInFinished();
            }
        }
    };

    protected void onSignInFinished() {
    }

    protected void sendSignInPendingIntent(PendingIntent intent) throws PendingIntent.CanceledException {
        if (null != intent)
            intent.send(RC_SIGN_IN, onSignInFinished, null);
    }

    // [START handleSignInResult]
    private void handleSignInResult(Activity activity, GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            handleGoogleAccount(activity, acct);
            //mStatusTextView.setText(getString(R.string.signed_in_fmt, acct.getDisplayName()));
            //updateUI(true);
        } else {
            // Signed out, show unauthenticated UI.
            //updateUI(false);
//            try {
//                if (null != result.getStatus().getResolution())
//                    result.getStatus().getResolution().send(RC_SIGN_IN, );
//            } catch (PendingIntent.CanceledException e) {
//               Log.e(TAG, "")
//            }
        }
    }
    // [END handleSignInResult]

    public void signIn() {
        startSignInActivity();
    }

    // [START signIn]
    public void startSignInActivity() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(activity, "An error has occurred.", Toast.LENGTH_SHORT).show();
    }
    // [END signIn]

    // [START signOut]
    public void signOut() {
        try {
            Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                    new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            // [START_EXCLUDE]
                            //updateUI(false);
                            // [END_EXCLUDE]
                        }
                    });
        }
        catch (Exception ex) {
            Log.e(TAG, "Unable to sign out");
        }
    }
    // [END signOut]

    // [START revokeAccess]
    public void revokeAccess() {
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        // [START_EXCLUDE]
                        //updateUI(false);
                        // [END_EXCLUDE]
                    }
                });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        connected = true;
        Log.i(TAG, "API client connected.");
        // Toast.makeText(controller.getCurrentActivity(), "Signed into Google Drive", Toast.LENGTH_SHORT).show();
        saveCredentialIfConnected(activity, mCredentialToSave);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
    // [END revokeAccess]

    public void signInCheck(final Activity activity) {
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            Log.d(TAG, "Got cached sign-in");
            GoogleSignInResult result = opr.get();
            handleSignInResult(activity, result);
        } else {
            // If the user has not previously signed in on this device or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user silently.  Cross-device
            // single sign-on will occur in this branch.
            //showProgressDialog();
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(GoogleSignInResult googleSignInResult) {
                    //hideProgressDialog();
                    handleSignInResult(activity, googleSignInResult);
                }
            });
        }
    }

    public void onActivityResult(FragmentActivity activity, int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);

        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult gsr = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleGoogleSignIn(activity, gsr);
        } else if (requestCode == RC_CREDENTIALS_READ) {
            mIsResolving = false;
            if (resultCode == RESULT_OK) {
                Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                handleCredential(activity, credential);
            }
        } else if (requestCode == RC_CREDENTIALS_SAVE) {
            mIsResolving = false;
            if (resultCode == RESULT_OK) {
                Toast.makeText(activity, "Saved", Toast.LENGTH_SHORT).show();
            } else {
                Log.w(TAG, "Credential save failed.");
            }
        }
    }

    private void buildGoogleApiClient(FragmentActivity activity, String accountName) {
        GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail();

        if (accountName != null) {
            gsoBuilder.setAccountName(accountName);
        }

        if (mGoogleApiClient != null) {
            mGoogleApiClient.stopAutoManage(activity);
        }

        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(activity)
                .addConnectionCallbacks(this)
                .enableAutoManage(activity, this)
                .addApi(Auth.CREDENTIALS_API)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gsoBuilder.build());

        mGoogleApiClient = builder.build();
    }

    private void googleSilentSignIn(final FragmentActivity activity) {
        // Try silent sign-in with Google Sign In API
        OptionalPendingResult<GoogleSignInResult> opr =
                Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            GoogleSignInResult gsr = opr.get();
            handleGoogleSignIn(activity, gsr);
        } else {
            //showProgress();
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(GoogleSignInResult googleSignInResult) {
                    //hideProgress();
                    handleGoogleSignIn(activity, googleSignInResult);
                }
            });
        }
    }

    private void handleCredential(FragmentActivity activity, Credential credential) {
        mCredential = credential;

        Log.d(TAG, "handleCredential:" + credential.getAccountType() + ":" + credential.getId());
        if (IdentityProviders.GOOGLE.equals(credential.getAccountType())) {
            // Google account, rebuild GoogleApiClient to set account name and then try
            buildGoogleApiClient(activity, credential.getId());
            googleSilentSignIn(activity);
        } else {
            // Email/password account
            String status = String.format("Signed in as %s", credential.getId());
            //((TextView) findViewById(R.id.text_email_status)).setText(status);
        }
    }

    private void handleGoogleSignIn(Activity activity, GoogleSignInResult gsr) {
        Log.d(TAG, "handleGoogleSignIn:" + (gsr == null ? "null" : gsr.getStatus()));

        boolean isSignedIn = (gsr != null) && gsr.isSuccess();
        if (isSignedIn) {
            // Display signed-in UI
            GoogleSignInAccount gsa = gsr.getSignInAccount();
            handleGoogleAccount(activity, gsa);
        } else {
            // Display signed-out UI
            // ((TextView) findViewById(R.id.text_google_status)).setText(R.string.signed_out);
        }

//        findViewById(R.id.button_google_sign_in).setEnabled(!isSignedIn);
//        findViewById(R.id.button_google_sign_out).setEnabled(isSignedIn);
//        findViewById(R.id.button_google_revoke).setEnabled(isSignedIn);
    }

    protected void handleGoogleAccount(Activity activity, GoogleSignInAccount gsa) {
        String status = String.format("Signed in as %s (%s)", gsa.getDisplayName(),
                gsa.getEmail());
        //((TextView) findViewById(R.id.text_google_status)).setText(status);
        Log.i(TAG, status);

        // Save Google Sign In to SmartLock
        accountName = gsa.getEmail();

        Credential credential = new Credential.Builder(gsa.getEmail())
                .setAccountType(IdentityProviders.GOOGLE)
                .setName(gsa.getDisplayName())
                .setProfilePictureUri(gsa.getPhotoUrl())
                .build();

        saveCredentialIfConnected(activity, credential);
    }

    private void requestCredentials(final FragmentActivity activity, final boolean shouldResolve, boolean onlyPasswords) {
        CredentialRequest.Builder crBuilder = new CredentialRequest.Builder()
                .setPasswordLoginSupported(true);

        if (!onlyPasswords) {
            crBuilder.setAccountTypes(IdentityProviders.GOOGLE);
        }

        //showProgress();
        Auth.CredentialsApi.request(mGoogleApiClient, crBuilder.build()).setResultCallback(
                new ResultCallback<CredentialRequestResult>() {
                    @Override
                    public void onResult(CredentialRequestResult credentialRequestResult) {
                        // hideProgress();
                        Status status = credentialRequestResult.getStatus();

                        if (status.isSuccess()) {
                            // Auto sign-in success
                            handleCredential(activity, credentialRequestResult.getCredential());
                        } else if (status.getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED
                                && shouldResolve) {
                            // Getting credential needs to show some UI, start resolution
                            resolveResult(activity, status, RC_CREDENTIALS_READ);
                        }
                    }
                });
    }

    private void resolveResult(Activity activity, Status status, int requestCode) {
        if (!mIsResolving) {
            try {
                status.startResolutionForResult(activity, requestCode);
                mIsResolving = true;
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Failed to send Credentials intent.", e);
                mIsResolving = false;
            }
        }
    }

    private void saveCredentialIfConnected(Activity activity, Credential credential) {
        if (credential == null) {
            return;
        }

        // Save Credential if the GoogleApiClient is connected, otherwise the
        // Credential is cached and will be saved when onConnected is next called.
        mCredentialToSave = credential;
        if (mGoogleApiClient.isConnected()) {
            Auth.CredentialsApi.save(mGoogleApiClient, mCredentialToSave).setResultCallback(
                    new ResolvingResultCallbacks<Status>(activity, RC_CREDENTIALS_SAVE) {
                        @Override
                        public void onSuccess(Status status) {
                            Log.d(TAG, "save:SUCCESS:" + status);
                            mCredentialToSave = null;
                        }

                        @Override
                        public void onUnresolvableFailure(Status status) {
                            Log.w(TAG, "save:FAILURE:" + status);
                            mCredentialToSave = null;
                        }
                    });
        }
    }

    private void onGoogleSignInClicked(Activity activity) {
        Intent intent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        activity.startActivityForResult(intent, RC_SIGN_IN);
    }

    private void onGoogleRevokeClicked(final Activity activity) {
        if (mCredential != null) {
            Auth.CredentialsApi.delete(mGoogleApiClient, mCredential);
        }
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        handleGoogleSignIn(activity, null);
                    }
                });
    }

    private void onGoogleSignOutClicked(final Activity activity) {
        Auth.CredentialsApi.disableAutoSignIn(mGoogleApiClient);
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        handleGoogleSignIn(activity, null);
                    }
                });
    }

    private void onEmailSignInClicked(FragmentActivity activity) {
        requestCredentials(activity, true, true);
    }

    private void onEmailSaveClicked(Activity activity, String email, String password) {
        Credential credential = new Credential.Builder(email)
                .setPassword(password)
                .build();

        saveCredentialIfConnected(activity, credential);
    }

    public void connect() {
        mGoogleApiClient.connect();
    }

    public void disconnect() {
        mGoogleApiClient.disconnect();
    }

}
