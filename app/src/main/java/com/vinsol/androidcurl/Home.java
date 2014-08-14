package com.vinsol.androidcurl;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class Home extends Activity implements View.OnClickListener {

    private final String TAG = Home.class.getSimpleName();

    private EditText customCommand;
    private LinearLayout customCommandResultLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        findViewById(R.id.curl_custom_command_run_button).setOnClickListener(this);
        customCommand = (EditText) findViewById(R.id.curl_custom_command_edittext);
        customCommandResultLayout = (LinearLayout) findViewById(R.id.curl_custom_command_result_layout);

        new LoadAndTestCurlBinaries().execute();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.curl_custom_command_run_button:
                String customCommandText = customCommand.getText().toString();
                customCommand.setText("");

                if (!TextUtils.isEmpty(customCommandText)) {
                    new RunCustomCommandAsyncTask(customCommandText, new OnCustomCommandPostExecute() {
                        @Override
                        public void performAfterResult(String string) {
                            TextView customCommandOutputTextView = new TextView(Home.this);
                            customCommandOutputTextView.setText(string);
                            customCommandResultLayout.addView(customCommandOutputTextView, 0);
                        }
                    }).execute();
                } else {
                    new AlertDialog.Builder(Home.this)
                            .setMessage("Sorry! You cannot execute empty command")
                            .setPositiveButton(getString(android.R.string.ok), null)
                            .create()
                            .show();
                }

                break;
        }
    }

    private class LoadAndTestCurlBinaries extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            cpuArchHelper cpuArchHelper = new cpuArchHelper();
            String cpuInfoString = cpuArchHelper.cpuArchFromJNI();

            if (cpuArchHelper.isARM_CPU(cpuInfoString)) {
                Log.d(TAG, "CPU String : "+cpuInfoString);
                // choose curl binary name (either arm or arm v7)
                String curlBinaryName = cpuArchHelper.isARM_v7_CPU(cpuInfoString) ? "armeabi-v7a/curl": "armeabi/curl";

                boolean hasFileCopied = FileUtils.copyBinaryFromAssetsToData(Home.this, curlBinaryName, FileUtils.curlFileName);

                // make directory executable
                if (hasFileCopied) {
                    String filesDirectoryPath = FileUtils.getFilesDirectory(Home.this).getAbsolutePath();
                    File curlFile = new File(filesDirectoryPath + File.separator + FileUtils.curlFileName);

                    if(!curlFile.canExecute()) {
                        Log.d(TAG, "Curl File is not executable, trying to make it executable ...");
                        if (curlFile.setExecutable(true)) {
                            return true;
                        }
                    } else {
                        Log.d(TAG, "Curl file is executable");
                        return true;
                    }
                }
            } else {
                Log.d(TAG, "NOT an ARM CPU. ****** NOT SUPPORTED ********** ");
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean loaded) {
            TextView curlInfo = (TextView) findViewById(R.id.curl_info);
            if (loaded) {
                new RunCustomCommandAsyncTask("--version", new OnCustomCommandPostExecute() {
                    @Override
                    public void performAfterResult(String string) {
                        ((TextView) findViewById(R.id.curl_info)).setText(string);
                    }
                }).execute();
                new RunCustomCommandAsyncTask("-I http://google.com", new OnCustomCommandPostExecute() {
                    @Override
                    public void performAfterResult(String string) {
                        ((TextView) findViewById(R.id.curl_with_http)).setText(string);
                    }
                }).execute();
                new RunCustomCommandAsyncTask("-I -k http://google.com", new OnCustomCommandPostExecute() {
                    @Override
                    public void performAfterResult(String string) {
                        ((TextView) findViewById(R.id.curl_with_https)).setText(string);
                    }
                }).execute();

            } else {
                curlInfo.setText("Device Not Supported");
                hideLabels();
            }
        }
    }

    private void hideLabels() {
        findViewById(R.id.curl_version_label).setVisibility(View.GONE);
        findViewById(R.id.curl_http_label).setVisibility(View.GONE);
        findViewById(R.id.curl_https_label).setVisibility(View.GONE);
    }

    private class RunCustomCommandAsyncTask extends AsyncTask<Void, Void, String> {

        private String parameters;
        private OnCustomCommandPostExecute onCustomCommandPostExecute;

        private RunCustomCommandAsyncTask(String parameters, OnCustomCommandPostExecute onCustomCommandPostExecute) {
            this.parameters = parameters;
            this.onCustomCommandPostExecute = onCustomCommandPostExecute;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                Process process = Runtime.getRuntime().exec(FileUtils.getCurl(Home.this) + " " + parameters);
                if (process.waitFor() == 0) {
                    // success
                    String output = new String(FileUtils.inputStreamToByteArray(process.getInputStream()));
                    Log.d(TAG, "process.getInputStream returns : "+output);
                    return "SUCCESS : "+output;
                } else {
                    // failure
                    String error = new String(FileUtils.inputStreamToByteArray(process.getErrorStream()));
                    Log.d(TAG, "process.getErrorStream returns : "+error);
                    return "ERROR : "+error;
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException while running curl command", e);
            } catch (InterruptedException e) {
                Log.e(TAG, "Curl command interrupted", e);
            }
            return "ERROR : UNKNOWN";
        }

        @Override
        protected void onPostExecute(String s) {
            onCustomCommandPostExecute.performAfterResult(s);
        }
    }

    private interface OnCustomCommandPostExecute {
        void performAfterResult(String string);
    }
}
