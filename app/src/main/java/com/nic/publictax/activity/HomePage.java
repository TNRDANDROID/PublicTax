package com.nic.publictax.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.CompoundButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.android.volley.VolleyError;
import com.nic.publictax.R;
import com.nic.publictax.adapter.CommonAdapter;
import com.nic.publictax.api.Api;
import com.nic.publictax.api.ApiService;
import com.nic.publictax.api.ServerResponse;
import com.nic.publictax.constant.AppConstant;
import com.nic.publictax.dataBase.DBHelper;
import com.nic.publictax.dataBase.dbData;
import com.nic.publictax.databinding.HomeScreenBinding;
import com.nic.publictax.dialog.MyDialog;
import com.nic.publictax.model.PublicTax;
import com.nic.publictax.session.PrefManager;
import com.nic.publictax.support.ProgressHUD;
import com.nic.publictax.utils.UrlGenerator;
import com.nic.publictax.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HomePage extends AppCompatActivity implements Api.ServerResponseListener, View.OnClickListener, MyDialog.myOnClickListener {
    private HomeScreenBinding homeScreenBinding;
    private PrefManager prefManager;
    public dbData dbData = new dbData(this);
    public static DBHelper dbHelper;
    public static SQLiteDatabase db;
    private String isHome;
    Handler myHandler = new Handler();
    private List<PublicTax> Village = new ArrayList<>();
    private List<PublicTax> Habitation = new ArrayList<>();
    String lastInsertedID;
    String isAlive = "", isLegal = "", isMigrated = "";
    private ProgressHUD progressHUD;


    String pref_Village;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        homeScreenBinding = DataBindingUtil.setContentView(this, R.layout.home_screen);
        homeScreenBinding.setActivity(this);
        prefManager = new PrefManager(this);
        try {
            dbHelper = new DBHelper(this);
            db = dbHelper.getWritableDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Bundle bundle = this.getIntent().getExtras();
        if (bundle != null) {
            isHome = bundle.getString("Home");
        }
        villageFilterSpinner(prefManager.getBlockCode());
        homeScreenBinding.villageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {

                    pref_Village = Village.get(position).getPvName();
                    prefManager.setVillageListPvName(pref_Village);
                    prefManager.setPvCode(Village.get(position).getPvCode());
                    habitationFilterSpinner(prefManager.getDistrictCode(),prefManager.getBlockCode(),prefManager.getPvCode());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        homeScreenBinding.habitationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    prefManager.setHabCode(Habitation.get(position).getHabCode());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        homeScreenBinding.takePicLayout.setAlpha(0);
        final Runnable pmgsy = new Runnable() {
            @Override
            public void run() {
                homeScreenBinding.takePicLayout.setAlpha(1);
                homeScreenBinding.takePicLayout.startAnimation(AnimationUtils.loadAnimation(HomePage.this, R.anim.text_view_move_right));

            }
        };

        homeScreenBinding.aliveYes.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    isAlive = "Y";
                    homeScreenBinding.aliveNo.setChecked(false);
                    homeScreenBinding.legalHeirTv.setVisibility(View.GONE);
                    homeScreenBinding.legalLayout.setVisibility(View.GONE);
                    homeScreenBinding.benfMigTv.setVisibility(View.VISIBLE);
                    homeScreenBinding.beneficiaryMigratedLayout.setVisibility(View.VISIBLE);
                    homeScreenBinding.migYes.setChecked(false);
                    homeScreenBinding.migNo.setChecked(false);
                    validateYesNo();
                }

            }
        });


        homeScreenBinding.aliveNo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    isAlive = "N";
                    homeScreenBinding.aliveYes.setChecked(false);
                    homeScreenBinding.legalYes.setChecked(false);
                    homeScreenBinding.legalNo.setChecked(false);
                    homeScreenBinding.legalHeirTv.setVisibility(View.VISIBLE);
                    homeScreenBinding.legalLayout.setVisibility(View.VISIBLE);
                    homeScreenBinding.migYes.setChecked(false);
                    homeScreenBinding.migNo.setChecked(false);
                    validateYesNo();
                } else {
                    homeScreenBinding.legalHeirTv.setVisibility(View.GONE);
                    homeScreenBinding.legalLayout.setVisibility(View.GONE);
                }
            }
        });

        homeScreenBinding.legalYes.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    isLegal = "Y";
                    homeScreenBinding.legalNo.setChecked(false);
                    homeScreenBinding.migYes.setChecked(false);
                    homeScreenBinding.migNo.setChecked(false);
                    homeScreenBinding.benfMigTv.setVisibility(View.VISIBLE);
                    homeScreenBinding.beneficiaryMigratedLayout.setVisibility(View.VISIBLE);
                    validateYesNo();
                }
            }
        });
        homeScreenBinding.legalNo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    isLegal = "N";
                    homeScreenBinding.legalYes.setChecked(false);
                    homeScreenBinding.benfMigTv.setVisibility(View.GONE);
                    homeScreenBinding.beneficiaryMigratedLayout.setVisibility(View.GONE);
                    validateYesNo();
                }
            }
        });

        homeScreenBinding.migYes.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    isMigrated = "Y";
                    homeScreenBinding.migNo.setChecked(false);
                    validateYesNo();
                }
            }
        });

        homeScreenBinding.migNo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    isMigrated = "N";
                    homeScreenBinding.migYes.setChecked(false);
                    validateYesNo();
                }
            }
        });

        myHandler.postDelayed(pmgsy, 1500);
        homeScreenBinding.viewServerData.setAlpha(0);
        final Runnable block = new Runnable() {
            @Override
            public void run() {
                homeScreenBinding.viewServerData.setAlpha(1);
                homeScreenBinding.viewServerData.startAnimation(AnimationUtils.loadAnimation(HomePage.this, R.anim.text_view_move));

            }
        };
        myHandler.postDelayed(block, 2000);

        syncButtonVisibility();
        viewServerData();

    }


    public void validateYesNo() {
        if (isAlive.equalsIgnoreCase("Y")) {
            isLegal = "";
        }
        if (isAlive.equalsIgnoreCase("N") && isLegal.equalsIgnoreCase("N")) {
            isMigrated = "";
        }
        if (isAlive.equalsIgnoreCase("Y") && isMigrated.equalsIgnoreCase("Y")) {
            homeScreenBinding.takePhotoTv.setText("Save details");
        } else if (isAlive.equalsIgnoreCase("Y") && isMigrated.equalsIgnoreCase("N")) {
            homeScreenBinding.takePhotoTv.setText("Take Photo");
        } else if (isAlive.equalsIgnoreCase("N") && isLegal.equalsIgnoreCase("N")) {
            homeScreenBinding.takePhotoTv.setText("Save details");
        } else if (isAlive.equalsIgnoreCase("N") && isLegal.equalsIgnoreCase("Y") && isMigrated.equalsIgnoreCase("Y")) {
            homeScreenBinding.takePhotoTv.setText("Save details");
        } else if (isAlive.equalsIgnoreCase("N") && isLegal.equalsIgnoreCase("Y") && isMigrated.equalsIgnoreCase("N")) {
            homeScreenBinding.takePhotoTv.setText("Take Photo");
        }
    }

//    public boolean validateCheck() {
//        if (isAlive.equalsIgnoreCase("") || isMigrated.equalsIgnoreCase("Y")) {
//            homeScreenBinding.takePhotoTv.setText("Save details");
//        } else if (isAlive.equalsIgnoreCase("Y") && isMigrated.equalsIgnoreCase("N")) {
//            homeScreenBinding.takePhotoTv.setText("Take Photo");
//        } else if (isAlive.equalsIgnoreCase("N") && isLegal.equalsIgnoreCase("N")) {
//            homeScreenBinding.takePhotoTv.setText("Save details");
//        } else if (isAlive.equalsIgnoreCase("N") && isLegal.equalsIgnoreCase("Y") && isMigrated.equalsIgnoreCase("Y")) {
//            homeScreenBinding.takePhotoTv.setText("Save details");
//        } else if (isAlive.equalsIgnoreCase("N") && isLegal.equalsIgnoreCase("Y") && isMigrated.equalsIgnoreCase("N")) {
//            homeScreenBinding.takePhotoTv.setText("Take Photo");
//        }
//    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(Activity.RESULT_CANCELED);
        overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);
    }



    public void villageFilterSpinner(String filterVillage) {
        Cursor VillageList = null;
        VillageList = db.rawQuery("SELECT * FROM " + DBHelper.VILLAGE_TABLE_NAME + " where dcode = "+prefManager.getDistrictCode()+ " and bcode = '" + filterVillage + "'", null);

        Village.clear();
        PublicTax villageListValue = new PublicTax();
        villageListValue.setPvName("Select Village");
        Village.add(villageListValue);
        if (VillageList.getCount() > 0) {
            if (VillageList.moveToFirst()) {
                do {
                    PublicTax villageList = new PublicTax();
                    String districtCode = VillageList.getString(VillageList.getColumnIndexOrThrow(AppConstant.DISTRICT_CODE));
                    String blockCode = VillageList.getString(VillageList.getColumnIndexOrThrow(AppConstant.BLOCK_CODE));
                    String pvCode = VillageList.getString(VillageList.getColumnIndexOrThrow(AppConstant.PV_CODE));
                    String pvname = VillageList.getString(VillageList.getColumnIndexOrThrow(AppConstant.PV_NAME));

                    villageList.setDistictCode(districtCode);
                    villageList.setBlockCode(blockCode);
                    villageList.setPvCode(pvCode);
                    villageList.setPvName(pvname);

                    Village.add(villageList);
                    Log.d("spinnersize", "" + Village.size());
                } while (VillageList.moveToNext());
            }
        }
        homeScreenBinding.villageSpinner.setAdapter(new CommonAdapter(this, Village, "VillageList"));
    }


    public void habitationFilterSpinner(String dcode,String bcode, String pvcode) {
        Cursor HABList = null;
        HABList = db.rawQuery("SELECT * FROM " + DBHelper.HABITATION_TABLE_NAME + " where dcode = '" + dcode + "'and bcode = '" + bcode + "' and pvcode = '" + pvcode + "' order by habitation_name asc", null);

        Habitation.clear();
        PublicTax habitationListValue = new PublicTax();
        habitationListValue.setHabitationName("Select Habitation");
        Habitation.add(habitationListValue);
        if (HABList.getCount() > 0) {
            if (HABList.moveToFirst()) {
                do {
                    PublicTax habList = new PublicTax();
                    String districtCode = HABList.getString(HABList.getColumnIndexOrThrow(AppConstant.DISTRICT_CODE));
                    String blockCode = HABList.getString(HABList.getColumnIndexOrThrow(AppConstant.BLOCK_CODE));
                    String pvCode = HABList.getString(HABList.getColumnIndexOrThrow(AppConstant.PV_CODE));
                    String habCode = HABList.getString(HABList.getColumnIndexOrThrow(AppConstant.HABB_CODE));
                    String habName = HABList.getString(HABList.getColumnIndexOrThrow(AppConstant.HABITATION_NAME));

                    habList.setDistictCode(districtCode);
                    habList.setBlockCode(blockCode);
                    habList.setPvCode(pvCode);
                    habList.setHabCode(habCode);
                    habList.setHabitationName(habName);

                    Habitation.add(habList);
                    Log.d("spinnersize", "" + Habitation.size());
                } while (HABList.moveToNext());
            }
        }
        homeScreenBinding.habitationSpinner.setAdapter(new CommonAdapter(this, Habitation, "HabitationList"));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

        }
    }

    public void logout() {
        dbData.open();
        ArrayList<PublicTax> ImageCount = dbData.getSavedPMAYDetails();
        if (!Utils.isOnline()) {
            Utils.showAlert(this, "Logging out while offline may leads to loss of data!");
        } else {
            if (!(ImageCount.size() > 0)) {
                closeApplication();
            } else {
                Utils.showAlert(this, "Sync all the data before logout!");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncButtonVisibility();
    }


    public void viewServerData() {
        homeScreenBinding.viewServerData.setVisibility(View.VISIBLE);
        homeScreenBinding.viewServerData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Utils.isOnline()) {
                    getPMAYList();
                } else {
                    Utils.showAlert(HomePage.this, "Your Internet seems to be Offline.Data can be viewed only in Online mode.");
                }
            }
        });
    }

    public void getPMAYList() {
        try {
            new ApiService(this).makeJSONObjectRequest("PMAYList", Api.Method.POST, UrlGenerator.getPMAYListUrl(), pmayListJsonParams(), "not cache", this);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONObject pmayListJsonParams() throws JSONException {
        String authKey = Utils.encrypt(prefManager.getUserPassKey(), getResources().getString(R.string.init_vector), Utils.pmayListJsonParams().toString());
        JSONObject dataSet = new JSONObject();
        dataSet.put(AppConstant.KEY_USER_NAME, prefManager.getUserName());
        dataSet.put(AppConstant.DATA_CONTENT, authKey);
        Log.d("PMAYList", "" + authKey);
        return dataSet;
    }

    @Override
    public void OnMyResponse(ServerResponse serverResponse) {

        try {
            String urlType = serverResponse.getApi();
            JSONObject responseObj = serverResponse.getJsonResponse();

            if ("PMAYList".equals(urlType) && responseObj != null) {
                String key = responseObj.getString(AppConstant.ENCODE_DATA);
                String responseDecryptedBlockKey = Utils.decrypt(prefManager.getUserPassKey(), key);
                JSONObject jsonObject = new JSONObject(responseDecryptedBlockKey);
                if (jsonObject.getString("STATUS").equalsIgnoreCase("OK") && jsonObject.getString("RESPONSE").equalsIgnoreCase("OK")) {
                    new InsertPMAYTask().execute(jsonObject);
                }else if(jsonObject.getString("STATUS").equalsIgnoreCase("OK") && jsonObject.getString("RESPONSE").equalsIgnoreCase("NO_RECORD") && jsonObject.getString("MESSAGE").equalsIgnoreCase("NO_RECORD")){
                    Utils.showAlert(this,"No Record Found!");
                }

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void dataFromServer() {
        Intent intent = new Intent(this, ViewServerDataScreen.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
    }

    @Override
    public void OnError(VolleyError volleyError) {

    }



    public void closeApplication() {
        new MyDialog(this).exitDialog(this, "Are you sure you want to Logout?", "Logout");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                new MyDialog(this).exitDialog(this, "Are you sure you want to exit ?", "Exit");
                return false;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onButtonClick(AlertDialog alertDialog, String type) {
        alertDialog.dismiss();
        if ("Exit".equalsIgnoreCase(type)) {
            onBackPressed();
        } else {

            Intent intent = new Intent(getApplicationContext(), LoginScreen.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("EXIT", false);
            startActivity(intent);
            finish();
            overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);
        }
    }

    public void validateFields() {
        if (!"Select Village".equalsIgnoreCase(Village.get(homeScreenBinding.villageSpinner.getSelectedItemPosition()).getPvName())) {
            if (!"Select Habitation".equalsIgnoreCase(Habitation.get(homeScreenBinding.habitationSpinner.getSelectedItemPosition()).getHabitationName())) {
                if (!homeScreenBinding.name.getText().toString().isEmpty()) {
                    if (!homeScreenBinding.fatherName.getText().toString().isEmpty()) {
                        if (!homeScreenBinding.seccId.getText().toString().isEmpty()) {
                            if (Utils.isValidMobile(homeScreenBinding.seccId.getText().toString())) {
                                if ((homeScreenBinding.aliveYes.isChecked()) || homeScreenBinding.aliveNo.isChecked()) {
                                    if (isAlive.equalsIgnoreCase("N")) {
                                        if ((homeScreenBinding.legalYes.isChecked()) || homeScreenBinding.legalNo.isChecked()) {
                                            checkLegalYesNo();
                                        } else {
                                            Utils.showAlert(this, "Check the beneficiary legal heir is available or not!");
                                        }
                                    } else {
                                        checkLegalYesNo();
                                    }
                                } else {
                                    Utils.showAlert(this, "Check the beneficiary is alive or not!");
                                }

                            } else {
                                Utils.showAlert(this, "Seec Id Must be 7 Digit!");
                            }
                        } else {
                            Utils.showAlert(this, "Enter the  Seec Id!");
                        }
                    } else {
                        Utils.showAlert(this, "Enter the Father/Husband Name!");
                    }
                } else {
                    Utils.showAlert(this, "Enter the Beneficiary Name!");
                }
            } else {
                Utils.showAlert(this, "Select Habitation!");
            }
        } else {
            Utils.showAlert(this, "Select Village!");
        }

    }

    public void checkLegalYesNo() {
        if ((isLegal.equalsIgnoreCase("N"))) {
            takePhoto(homeScreenBinding.takePhotoTv.getText().toString());
        } else {
            if ((homeScreenBinding.migYes.isChecked()) || homeScreenBinding.migNo.isChecked()) {
                takePhoto(homeScreenBinding.takePhotoTv.getText().toString());
            } else {
                Utils.showAlert(this, "Check the beneficiary is Migrated or not!");
            }
        }
    }


    public void takePhoto(String buttonTxt) {
        Log.d("buttonTxt",""+buttonTxt);
        String pvcode = Village.get(homeScreenBinding.villageSpinner.getSelectedItemPosition()).getPvCode();
        String habcode = Habitation.get(homeScreenBinding.habitationSpinner.getSelectedItemPosition()).getHabCode();
        String beneficiary_name = homeScreenBinding.name.getText().toString();
        String father_name = homeScreenBinding.fatherName.getText().toString();
        String secc_id = homeScreenBinding.seccId.getText().toString();

        ContentValues registerValue = new ContentValues();
        registerValue.put(AppConstant.DISTRICT_CODE, prefManager.getDistrictCode());
        registerValue.put(AppConstant.BLOCK_CODE, prefManager.getBlockCode());
        registerValue.put(AppConstant.PV_CODE, pvcode);
        registerValue.put(AppConstant.HAB_CODE, habcode);
        registerValue.put(AppConstant.PV_NAME, Village.get(homeScreenBinding.villageSpinner.getSelectedItemPosition()).getPvName());
        registerValue.put(AppConstant.HABITATION_NAME, Habitation.get(homeScreenBinding.habitationSpinner.getSelectedItemPosition()).getHabitationName());
        registerValue.put(AppConstant.BENEFICIARY_NAME, beneficiary_name);
        registerValue.put(AppConstant.BENEFICIARY_FATHER_NAME, father_name);
        registerValue.put(AppConstant.SECC_ID, secc_id);
        registerValue.put(AppConstant.PERSON_ALIVE, isAlive);
        registerValue.put(AppConstant.LEGAL_HEIR_AVAILABLE, isLegal);
        registerValue.put(AppConstant.PERSON_MIGRATED, isMigrated);
        registerValue.put(AppConstant.BUTTON_TEXT, buttonTxt);

        long id = db.insert(DBHelper.SAVE_PMAY_DETAILS, null, registerValue);
        Log.d("insert_id",String.valueOf(id));

        if(buttonTxt.equals("Take Photo")){
            if(id > 0) {

                Cursor cursor = db.rawQuery("SELECT MAX(id) FROM " + DBHelper.SAVE_PMAY_DETAILS, null);
                Log.d("cursor_count", String.valueOf(cursor.getCount()));
                if (cursor.getCount() > 0) {
                    if (cursor.moveToFirst()) {
                        do {
                            lastInsertedID = String.valueOf(cursor.getInt(0));
                            Log.d("lastID", "" + lastInsertedID);
                        } while (cursor.moveToNext());
                    }
                }

                Intent intent = new Intent(this, TakePhotoScreen.class);
                intent.putExtra("lastInsertedID",lastInsertedID);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
            }
        }else {
            Utils.showAlert(this,"Saved");
            syncButtonVisibility();
            emptyValue();
//            finish();
//            startActivity(getIntent());
        }


    }

    public void syncButtonVisibility() {
        dbData.open();
        ArrayList<PublicTax> workImageCount = dbData.getSavedPMAYDetails();

        if (workImageCount.size() > 0) {
            homeScreenBinding.synData.setVisibility(View.VISIBLE);
        }else {
            homeScreenBinding.synData.setVisibility(View.GONE);
        }
    }

    public void emptyValue() {
        homeScreenBinding.villageSpinner.setSelection(0);
        homeScreenBinding.habitationSpinner.setSelection(0);
        homeScreenBinding.fatherName.setText("");
        homeScreenBinding.name.setText("");
        homeScreenBinding.seccId.setText("");
        homeScreenBinding.aliveYes.setChecked(false);
        homeScreenBinding.aliveNo.setChecked(false);
        homeScreenBinding.legalYes.setChecked(false);
        homeScreenBinding.legalNo.setChecked(false);
        homeScreenBinding.migYes.setChecked(false);
        homeScreenBinding.migNo.setChecked(false);
        isLegal = "";
        isAlive = "";
        isMigrated = "";

    }

    public void openPendingScreen() {
        Intent intent = new Intent(this, PendingScreen.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
    }


    public class InsertPMAYTask extends AsyncTask<JSONObject, Void, Void> {

        @Override
        protected Void doInBackground(JSONObject... params) {
            dbData.deletePMAYTable();
            dbData.open();
            ArrayList<PublicTax> all_pmayListCount = dbData.getAll_PMAYList("","");
            if (all_pmayListCount.size() <= 0) {
                if (params.length > 0) {
                    JSONArray jsonArray = new JSONArray();
                    try {
                        jsonArray = params[0].getJSONArray(AppConstant.JSON_DATA);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    for (int i = 0; i < jsonArray.length(); i++) {
                        PublicTax publicTax = new PublicTax();
                        try {
                            publicTax.setPvCode(jsonArray.getJSONObject(i).getString(AppConstant.PV_CODE));
                            publicTax.setHabCode(jsonArray.getJSONObject(i).getString(AppConstant.HAB_CODE));
                            publicTax.setBeneficiaryName(jsonArray.getJSONObject(i).getString(AppConstant.BENEFICIARY_NAME));
                            publicTax.setSeccId(jsonArray.getJSONObject(i).getString(AppConstant.SECC_ID));
                            publicTax.setHabitationName(jsonArray.getJSONObject(i).getString(AppConstant.HABITATION_NAME));
                            publicTax.setPvName(jsonArray.getJSONObject(i).getString(AppConstant.PV_NAME));
                            publicTax.setPersonAlive(jsonArray.getJSONObject(i).getString(AppConstant.PERSON_ALIVE));
                            publicTax.setIsLegel(jsonArray.getJSONObject(i).getString(AppConstant.LEGAL_HEIR_AVAILABLE));
                            publicTax.setIsMigrated(jsonArray.getJSONObject(i).getString(AppConstant.PERSON_MIGRATED));

                            dbData.insertPMAY(publicTax);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }

            }
            return null;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressHUD = ProgressHUD.show(HomePage.this, "Downloading", true, false, null);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(progressHUD!=null){
                progressHUD.cancel();
            }
            dataFromServer();

        }
    }

}
