// arthow4n/react-native-file-intent
//
// requestFile() and onActivityResult() method are derived from marcshilling/react-native-image-picker
// ******** LICENSE OF REACT-NATIVE-IMAGE-PICKER START ********
// https://github.com/marcshilling/react-native-image-picker
// The MIT License (MIT)
// Copyright (c) 2015 Marc Shilling
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// ******** LICENSE OF REACT-NATIVE-IMAGE-PICKER END ********

package com.reactnativefileintent;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.webkit.MimeTypeMap;
import android.content.pm.PackageManager;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.net.MalformedURLException;
import java.net.URL;

public class RNFileIntentModule extends ReactContextBaseJavaModule implements ActivityEventListener {

  static final int GET_FILE_BY_MIME = 1001;
  private final ReactApplicationContext mReactContext;
  private Callback mCallback;
  private String mimeType = "*/*";
  WritableArray responseArray;
  WritableMap response;

  public RNFileIntentModule(ReactApplicationContext reactContext) {
    super(reactContext);

    reactContext.addActivityEventListener(this);

    mReactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNFileIntent";
  }

  @ReactMethod
  public void queryFileStat(final String uriString, final Callback callback) {
      Uri uri = Uri.parse(uriString);
      WritableMap respoonse = Arguments.createMap();

      ContentResolver cR = mReactContext.getContentResolver();
      Cursor cRCursor = cR.query(uri, null, null, null, null);
      int nameIndex = cRCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
      int sizeIndex = cRCursor.getColumnIndex(OpenableColumns.SIZE);
      cRCursor.moveToFirst();

      response.putString("name", cRCursor.getString(nameIndex));
      response.putString("size", Long.toString(cRCursor.getLong(sizeIndex)));
      response.putString("mimeType", cR.getType(uri));
      response.putString("uri", uri.toString());
      callback.invoke(response);

      return;
  }

  @ReactMethod
  public void getReceivedFile(final Callback callback) {
    responseArray = Arguments.createArray();
    WritableMap respoonse = Arguments.createMap();
    Intent receivedIntent = getCurrentActivity().getIntent();
    String receivedAction = receivedIntent.getAction();
    String receivedType = receivedIntent.getType();
    ContentResolver cR = mReactContext.getContentResolver();

    if ( Intent.ACTION_SEND.equals(receivedAction) && receivedType != null ) {
        Uri receivedUri = (Uri)receivedIntent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (receivedUri != null) {
            Cursor cRCursor = cR.query(receivedUri, null, null, null, null);
            int nameIndex = cRCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            int sizeIndex = cRCursor.getColumnIndex(OpenableColumns.SIZE);
            cRCursor.moveToFirst();
            WritableMap respMap;
            respMap = Arguments.createMap();
            respMap.putString("action", receivedAction);
            respMap.putString("fileName", cRCursor.getString(nameIndex));
            respMap.putString("fileSize", Long.toString(cRCursor.getLong(sizeIndex)));
            respMap.putString("type", cR.getType(receivedUri));
            respMap.putString("uri", receivedUri.toString());
            responseArray.pushMap(respMap);
            callback.invoke(responseArray);
        } else {
            response.putBoolean("gotNothing", new Boolean("true"));
            callback.invoke(response);
        }
    } else if (Intent.ACTION_SEND_MULTIPLE.equals(receivedAction) && receivedType != null) {
        ArrayList<Uri> receivedUris = receivedIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (receivedUris != null) {
            for (Uri uri : receivedUris) {
                Cursor cRCursor = cR.query(uri, null, null, null, null);
                int nameIndex = cRCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cRCursor.getColumnIndex(OpenableColumns.SIZE);
                cRCursor.moveToFirst();
                WritableMap respMap;
                respMap = Arguments.createMap();
                respMap.putString("action", receivedAction);
                respMap.putString("fileName", cRCursor.getString(nameIndex));
                respMap.putString("fileSize", Long.toString(cRCursor.getLong(sizeIndex)));
                respMap.putString("type", cR.getType(uri));
                respMap.putString("uri", uri.toString());
                responseArray.pushMap(respMap);
            }
            callback.invoke(responseArray);
        } else {
            response.putBoolean("gotNothing", new Boolean("true"));
            callback.invoke(response);
        }
    } else {
        callback.invoke(responseArray);
    }
    return;
  }

  @ReactMethod
  public void requestFile(final String mimeTypeInput, final Callback callback) {
    int requestCode = GET_FILE_BY_MIME;
    Intent libraryIntent;
    Activity currentActivity = getCurrentActivity();

    if (currentActivity == null) {
      response = Arguments.createMap();
      response.putString("error", "Cannot find current Activity");
      callback.invoke(response);
      return;
    }

    mimeType = mimeTypeInput;

    libraryIntent = new Intent(Intent.ACTION_GET_CONTENT);
    libraryIntent.setType(mimeType);

    if (libraryIntent.resolveActivity(mReactContext.getPackageManager()) == null) {
      response = Arguments.createMap();
      response.putString("error", "Failed to resolve intent activity");
      callback.invoke(response);
      return;
    }

    mCallback = callback;

    try {
      currentActivity.startActivityForResult(libraryIntent, requestCode);
    } catch (ActivityNotFoundException e) {
      e.printStackTrace();
      response = Arguments.createMap();
      response.putString("error", "Cannot launch file explorer");
      callback.invoke(response);
    }
  }

  @Override
  public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

    ContentResolver cR = mReactContext.getContentResolver();

    //robustness code
    if (mCallback == null || requestCode != GET_FILE_BY_MIME) {
      return;
    }

    response = Arguments.createMap();

    // user cancel
    if (resultCode != Activity.RESULT_OK) {
      response.putBoolean("didCancel", true);
      mCallback.invoke(response);
      return;
    }


    Uri uri = data.getData();
    Cursor cRCursor = cR.query(uri, null, null, null, null);
    int nameIndex = cRCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
    int sizeIndex = cRCursor.getColumnIndex(OpenableColumns.SIZE);
    cRCursor.moveToFirst();

    response.putString("fileName", cRCursor.getString(nameIndex));
    response.putString("fileSize", Long.toString(cRCursor.getLong(sizeIndex)));
    response.putString("type", cR.getType(uri));
    response.putString("uri", uri.toString());
    mCallback.invoke(response);
  }
}
