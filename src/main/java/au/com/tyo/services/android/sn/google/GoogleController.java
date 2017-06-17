package au.com.tyo.services.android.sn.google;

import com.google.android.gms.drive.MetadataBuffer;

/**
 * Created by Eric Tang (eric.tang@tyo.com.au) on 2/6/17.
 */

public interface GoogleController {

    GoogleGmsClient getGoogleClient();

    GoogleDrive getGoogleDriveClient();

    void onGoogleDriveFilesMetadataReceived(MetadataBuffer metadataBuffer);

}
