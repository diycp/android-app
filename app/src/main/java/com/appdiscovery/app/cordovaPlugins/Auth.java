package com.appdiscovery.app.cordovaPlugins;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.appdiscovery.app.EditUserProfileActivity;
import com.appdiscovery.app.MainActivity;
import com.appdiscovery.app.R;
import com.appdiscovery.app.UserInfoAuthorizationDialogFragment;
import com.appdiscovery.app.services.DigitalSignature;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class Auth extends CordovaPlugin {

    private static CallbackContext eventCallbackContext;

    void showAuthDialog(String[] args, final CallbackContext callbackContext) {
        Activity cordovaActivity = cordova.getActivity();
        FragmentTransaction ft = cordovaActivity.getFragmentManager().beginTransaction();
        Fragment prev = cordovaActivity.getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        Bundle bundle = new Bundle();
        bundle.putStringArray("requestedPermissions", args);
        UserInfoAuthorizationDialogFragment newFragment = UserInfoAuthorizationDialogFragment.newInstance(bundle);
        newFragment.mOnAccept = view -> {
            SharedPreferences sharedPref = cordovaActivity.getSharedPreferences(
                    cordovaActivity.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

            try {
                PluginResult result;
                JSONObject callbackObj = new JSONObject();
                List<String> argsList = Arrays.asList(args);
                if (argsList.contains("email")) {
                    callbackObj.put("email", sharedPref.getString(cordovaActivity.getString(R.string.user_email_key), ""));
                }
                if (argsList.contains("name")) {
                    callbackObj.put("name", sharedPref.getString(cordovaActivity.getString(R.string.user_name_key), ""));
                }
                if (argsList.contains("mobile")) {
                    callbackObj.put("mobile", sharedPref.getString(cordovaActivity.getString(R.string.user_mobile_key), ""));
                }

                result = new PluginResult(PluginResult.Status.OK, callbackObj.toString());
                result.setKeepCallback(false);
                callbackContext.sendPluginResult(result);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        };
        newFragment.mOnReject = view -> {
            PluginResult result;
            result = new PluginResult(PluginResult.Status.ERROR);
            result.setKeepCallback(false);
            callbackContext.sendPluginResult(result);
        };
        newFragment.show(ft, "dialog");
    }

    void getUserIdentity(String[] args, final CallbackContext callbackContext) {
        Activity cordovaActivity = cordova.getActivity();

        String privateKey = DigitalSignature.getPrivateKey(cordovaActivity);
        String publicKey = DigitalSignature.getPublicKey(cordovaActivity);

        String signedContent = "APP:" + MainActivity.activeAppName + ":" + System.currentTimeMillis();
        String signature = DigitalSignature.sign(signedContent, privateKey);

        try {
            JSONObject userIdentity = new JSONObject();
            userIdentity.put("publicKey", publicKey);
            userIdentity.put("signature", signature);
            userIdentity.put("signedContent", signedContent);
            PluginResult result = new PluginResult(PluginResult.Status.OK, userIdentity.toString());
            result.setKeepCallback(false);
            callbackContext.sendPluginResult(result);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
//        args.getString()
        int argsLength = args.length();
        String[] argsInString = new String[argsLength];
        for (int i = 0; i < argsLength; i++) {
            argsInString[i] = args.getString(i);
        }
        if ("getUserInfo".equals(action)) {
            showAuthDialog(argsInString, callbackContext);
            return true;
        } else if ("getUserIdentity".equals(action)) {
            getUserIdentity(argsInString, callbackContext);
            return true;
        }
        return false;
    }
}
