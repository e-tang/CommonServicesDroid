package au.com.tyo.services.android.google.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.support.v4.print.PrintHelper;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import au.com.tyo.android.AndroidUtils;
import au.com.tyo.app.CommonActivityWebView;
import au.com.tyo.app.Controller;
import au.com.tyo.app.ui.page.PageWebView;

import static android.print.PrintDocumentInfo.CONTENT_TYPE_PHOTO;

/**
 * Created by Eric Tang (eric.tang@tyo.com.au) on 9/1/18.
 */

public class CloudPrintDialog extends CommonActivityWebView {

    private static final String TAG = "CloudPrintDialog";

    private static final String PRINT_DIALOG_URL = "https://www.google.com/cloudprint/dialog.html";
    private static final String JS_INTERFACE = "AndroidPrintDialog";
    private static final String CONTENT_TRANSFER_ENCODING = "base64";

    private static final String ZXING_URL = "http://zxing.appspot.com";
    private static final int ZXING_SCAN_REQUEST = 65743;

    /**
     * Post message that is sent by Print Dialog web page when the printing dialog
     * needs to be closed.
     */
    private static final String CLOSE_POST_MESSAGE_NAME = "cp-dialog-on-close";

    @Override
    protected void loadPageClass() {
        getAgent().setPageClass(CloudPrintPage.class);
    }

    public static class CloudPrintPage extends PageWebView<Controller> implements PageWebView.WebPageListener {

        private Intent cloudPrintIntent;

        /**
         * @param controller
         * @param activity
         */
        public CloudPrintPage(Controller controller, Activity activity) {
            super(controller, activity);

            setWebViewClient(new PrintDialogWebClient(activity));
        }

        @Override
        public void bindData(Intent intent) {
            super.bindData(intent);

            cloudPrintIntent = intent;
        }

        @Override
        public void setupComponents() {
            super.setupComponents();

            getWebView().addJavascriptInterface(
                    new PrintDialogJavaScriptInterface(), JS_INTERFACE);

            getWebView().loadUrl(PRINT_DIALOG_URL);
        }

        @Override
        public boolean onActivityResult(int requestCode, int resultCode, Intent intent) {
            if (requestCode == ZXING_SCAN_REQUEST && resultCode == RESULT_OK) {
                getWebView().loadUrl(intent.getStringExtra("SCAN_RESULT"));
                return true;
            }
            return false;
        }

        @Override
        public void onPageFinishedLoading(WebView webView, String url) {
            if (PRINT_DIALOG_URL.equals(url)) {
                // Submit print document.
                call("printDialog.setPrintDocument(printDialog.createPrintDocument("
                        + "window." + JS_INTERFACE + ".getType(),window." + JS_INTERFACE + ".getTitle(),"
                        + "window." + JS_INTERFACE + ".getContent(),window." + JS_INTERFACE + ".getEncoding()))");

                // Add post messages listener.
                call("window.addEventListener('message',"
                        + "function(evt){window." + JS_INTERFACE + ".onPostMessage(evt.data)}, false)");
            }
        }

        final class PrintDialogJavaScriptInterface {
            @JavascriptInterface
            public String toString() { return JS_INTERFACE; }

            @JavascriptInterface
            public String getType() {
                return cloudPrintIntent.getType();
            }

            @JavascriptInterface
            public String getTitle() {
                return cloudPrintIntent.getExtras().getString("title");
            }

            @JavascriptInterface
            public String getContent() {
                try {
                    ContentResolver contentResolver = getActivity().getContentResolver();
                    InputStream is = contentResolver.openInputStream(cloudPrintIntent.getData());
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    byte[] buffer = new byte[4096];
                    int n = is.read(buffer);
                    while (n >= 0) {
                        baos.write(buffer, 0, n);
                        n = is.read(buffer);
                    }
                    is.close();
                    baos.flush();

                    return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return "";
            }

            @JavascriptInterface
            public String getEncoding() {
                return CONTENT_TRANSFER_ENCODING;
            }

            @JavascriptInterface
            public void onPostMessage(String message) {
                if (message.startsWith(CLOSE_POST_MESSAGE_NAME)) {
                    finish();
                }
            }
        }

        private final class PrintDialogWebClient extends PageWebView.CommonWebViewClient {

            private Activity activity;

            public PrintDialogWebClient(Activity activity) {
                super(CloudPrintPage.this);
                this.activity = activity;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(ZXING_URL)) {
                    Intent intentScan = new Intent("com.google.zxing.client.android.SCAN");
                    intentScan.putExtra("SCAN_MODE", "QR_CODE_MODE");
                    try {
                        activity.startActivityForResult(intentScan, ZXING_SCAN_REQUEST);
                    } catch (ActivityNotFoundException error) {
                        view.loadUrl(url);
                    }
                } else {
                    view.loadUrl(url);
                }
                return false;
            }
        }
    }

    public static void openCloudPrintDialog(Context context, Uri docUri, String docMimeType, String title) {
        Intent printIntent = new Intent(context, CloudPrintDialog.class);
        printIntent.setDataAndType(docUri, docMimeType);
        printIntent.putExtra("title", title);
        context.startActivity(printIntent);
    }
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void printImageAsPdfDocument(final Context context, final String title, final Bitmap bitmap, PdfDocument.PageInfo pageInfo) {

    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void printDocument(final Context context, final Uri docUri, final String docMimeType, final String title, int width, int height) {

        final boolean isPhoto = docMimeType.startsWith("image");
        if (isPhoto) {
            PrintHelper photoPrinter = new PrintHelper(context);
            photoPrinter.setScaleMode(PrintHelper.SCALE_MODE_FIT);
            try {
                photoPrinter.printBitmap(title, docUri);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Cannot find photo: " +  docUri.toString());
            }
            return;
        }

        PrintAttributes attributes = null;
        if (width > -1 && height > -1) {
            attributes = new PrintAttributes.Builder()
                    .setMediaSize(new PrintAttributes.MediaSize("Special" , "Custom", width,height))
                    .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                    .build();
        }

        PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);

        if (AndroidUtils.getAndroidVersion() <=15 || null == printManager) {
            openCloudPrintDialog(context, docUri, docMimeType, title);
            return;
        }

        String jobName = title;

        PrintDocumentAdapter pda = new PrintDocumentAdapter(){
            @Override
            public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras){

                if (cancellationSignal.isCanceled()) {
                    callback.onLayoutCancelled();
                    return;
                }

                // Compute the expected number of printed pages
                int pages = 1; // computePageCount(newAttributes);

                PrintDocumentInfo pdi = new PrintDocumentInfo.Builder(title)
                        .setPageCount(pages)
                        .setContentType(isPhoto ? CONTENT_TYPE_PHOTO : PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build();

                callback.onLayoutFinished(pdi, true);
            }

            @Override
            public void onWrite(PageRange[] pages, ParcelFileDescriptor destination, CancellationSignal cancellationSignal, WriteResultCallback callback){
                InputStream input = null;
                OutputStream output = null;

                try {
                    ContentResolver contentResolver = context.getContentResolver();
                    input = contentResolver.openInputStream(docUri);
                    // input = new FileInputStream(docUri.toString());?
                    output = new FileOutputStream(destination.getFileDescriptor());

                    byte[] buf = new byte[1024];
                    int bytesRead;

                    while ((bytesRead = input.read(buf)) > 0) {
                        output.write(buf, 0, bytesRead);
                    }

                    callback.onWriteFinished(pages);

                } catch (FileNotFoundException ee){
                    //Catch exception
                } catch (Exception e) {
                    //Catch exception
                } finally {
                    try {
                        input.close();
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }


        };
        printManager.print(jobName, pda, attributes);
    }
}
