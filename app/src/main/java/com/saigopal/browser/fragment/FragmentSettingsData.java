package com.saigopal.browser.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.saigopal.browser.R;
import com.saigopal.browser.task.ExportWhiteListTask;
import com.saigopal.browser.task.ImportWhitelistTask;
import com.saigopal.browser.unit.BrowserUnit;
import com.saigopal.browser.unit.HelperUnit;
import com.saigopal.browser.view.Toasty;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.PreferenceFragmentCompat;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

@SuppressWarnings({"ResultOfMethodCallIgnored"})
public class FragmentSettingsData extends PreferenceFragmentCompat {

    private BottomSheetDialog dialog;
    private View dialogView;
    private TextView textView;
    private Button action_ok;
    private String ExternalPath;
    private final int bookmarksImportCode = 1;
    private final int SettingsImportCode = 0;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_data, rootKey);
        File sd = requireActivity().getExternalFilesDir(null);
        File data = Environment.getDataDirectory();
        String database_app = "//data//" + requireActivity().getPackageName() + "//databases//data.db";
        String database_backup = "BlackBrowser_backup//data.db";
        final File previewsFolder_app = new File(data, database_app);
        final File previewsFolder_backup = new File(sd, database_backup);

        ExternalPath = Environment.getExternalStorageDirectory().toString();

        int WRITE_EXTERNAL_STORAGE = requireActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (WRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
            HelperUnit.grantPermissionsStorage(getActivity());
        }

        findPreference("data_exDB").setOnPreferenceClickListener(preference -> {
            dialog = new BottomSheetDialog(requireActivity());
            dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
            textView = dialogView.findViewById(R.id.dialog_text);
            textView.setText(R.string.toast_backup);
            action_ok = dialogView.findViewById(R.id.action_ok);
            action_ok.setOnClickListener(view -> {
                dialog.cancel();
                try {
                        int hasWRITE_EXTERNAL_STORAGE = requireActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                            HelperUnit.grantPermissionsStorage(getActivity());
                            dialog.cancel();
                        } else {
                            makeBackupDir();
                            BrowserUnit.deleteDir(previewsFolder_backup);
                           // copyDirectory(previewsFolder_app, previewsFolder_backup);
                            backupUserPrefs(requireActivity());
                        }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            dialog.setContentView(dialogView);
            dialog.show();
            HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
            return false;
        });

        findPreference("data_imDB").setOnPreferenceClickListener(preference -> {
            dialog = new BottomSheetDialog(requireActivity());
            dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
            textView = dialogView.findViewById(R.id.dialog_text);
            textView.setText(R.string.hint_database);
            action_ok = dialogView.findViewById(R.id.action_ok);
            action_ok.setOnClickListener(view -> {
                dialog.cancel();
                try {
                        int hasWRITE_EXTERNAL_STORAGE = requireActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                            HelperUnit.grantPermissionsStorage(getActivity());
                            dialog.cancel();
                        } else {
                            BrowserUnit.deleteDir(previewsFolder_app);
                          //  copyDirectory(previewsFolder_backup, previewsFolder_app);
                            restoreUserPrefs();
                            dialogRestart();
                        }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            dialog.setContentView(dialogView);
            dialog.show();
            HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
            return false;
        });
        findPreference("data_imBookmark").setOnPreferenceClickListener(preference -> {

            Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
            chooseFile.setType("*/*");
            chooseFile = Intent.createChooser(chooseFile, "Choose a Bookmarks file");
            startActivityForResult(chooseFile, bookmarksImportCode);

            return false;
        });
        findPreference("data_exBookmark").setOnPreferenceClickListener(preference -> {
            new ExportWhiteListTask(getActivity(), 4).execute();
            return false;
        });
    }

    private void makeBackupDir () {
        File backupDir = new File(ExternalPath+"/BlackBrowser_backup//");

        int hasWRITE_EXTERNAL_STORAGE = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            hasWRITE_EXTERNAL_STORAGE = requireActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                HelperUnit.grantPermissionsStorage(getActivity());
            } else {
                if (!backupDir.exists()) {
                    try {
                        backupDir.mkdirs();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        else{
        if (!backupDir.exists()) {
            try {
                backupDir.mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    }

    private void dialogRestart () {
        SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
        sp.edit().putInt("restart_changed", 1).apply();
    }

    // If targetLocation does not exist, it will be created.
    private void copyDirectory(File sourceLocation, File targetLocation)
            throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists() && !targetLocation.mkdirs()) {
                throw new IOException("Cannot create dir " + targetLocation.getAbsolutePath());
            }
            String[] children = sourceLocation.list();
            for (String aChildren : Objects.requireNonNull(children)) {
                copyDirectory(new File(sourceLocation, aChildren), new File(targetLocation, aChildren));
            }
        } else {
            // make sure the directory we plan to store the recording in exists
            File directory = targetLocation.getParentFile();
            if (directory != null && !directory.exists() && !directory.mkdirs()) {
                throw new IOException("Cannot create dir " + directory.getAbsolutePath());
            }

            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);
            // Copy the bits from InputStream to OutputStream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }

    public boolean backupUserPrefs(Context context) {
        final File prefsFile = new File(context.getFilesDir(), "../shared_prefs/" + context.getPackageName() + "_preferences.xml");
        final File File = new File( ExternalPath+"/BlackBrowser_backup/");
        File backupFile = new File(File+"/Settings_backup.xml");
        if (!File.exists()){
            File.mkdir();
        }
        try {
            FileChannel src = new FileInputStream(prefsFile).getChannel();
            FileChannel dst = new FileOutputStream(backupFile).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();
            Toasty.show(context, "Saved at : BlackBrowser_backup/Settings_backup.xml");
           // shareFile(backupFile);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Toasty.show(context,"Error : "+e);
            return false;
        }
    }

    @SuppressLint("ApplySharedPref")
    public void restoreUserPrefs() {

        Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.setType("*/*");
        chooseFile = Intent.createChooser(chooseFile, "Choose a Settings file");
        startActivityForResult(chooseFile, SettingsImportCode);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == SettingsImportCode){
            if (resultCode == -1) {
                assert data != null;
                Uri fileUri = data.getData();
                assert fileUri != null;
                RestoreUserData(ConvertPath(fileUri));
            }
        }
        if (requestCode == bookmarksImportCode){
            assert data != null;
            Uri fileUri = data.getData();
            assert fileUri != null;
            new ImportWhitelistTask(getActivity(), 4,ConvertPath(fileUri)).execute();
        }
    }

    public void RestoreUserData(String filePath){
        final File backupFile = new File(filePath);
        String error;
        try {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
            SharedPreferences.Editor editor = sharedPreferences.edit();
            InputStream inputStream = new FileInputStream(backupFile);
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(inputStream);
            Element root = doc.getDocumentElement();
            Node child = root.getFirstChild();
            while (child != null) {
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) child;
                    String type = element.getNodeName();
                    String name = element.getAttribute("name");
                    // In my app, all prefs seem to get serialized as either "string" or
                    // "boolean" - this will need expanding if yours uses any other types!
                    if (type.equals("string")) {
                        String value = element.getTextContent();
                        editor.putString(name, value);
                    } else if (type.equals("boolean")) {
                        String value = element.getAttribute("value");
                        editor.putBoolean(name, value.equals("true"));
                    }
                }
                child = child.getNextSibling();
            }
            editor.commit();
            Toasty.show(getContext(), "Restored user preferences from " + backupFile.getAbsolutePath());
            return;
        } catch (IOException | SAXException | ParserConfigurationException e) {
            error = e.getMessage();
            e.printStackTrace();
        }
        Toast toast = Toast.makeText(getActivity(), "Failed to restore user preferences from " + backupFile.getAbsolutePath() + " - " + error, Toast.LENGTH_SHORT);
        toast.show();

    }

    private String ConvertPath(Uri FileUri){
        String filePath = FileUri.getPath();
        filePath = Objects.requireNonNull(filePath).replace("/document/primary:",ExternalPath+"/");
        return filePath;
    }
}
