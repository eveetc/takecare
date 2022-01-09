package com.mad.takecare.ui;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.DoubleBounce;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mad.takecare.R;
import com.mad.takecare.model.Task;
import com.mad.takecare.model.User;
import com.mad.takecare.ui.login.LoginActivity;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link UserProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserProfileFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    Button logoutButton;
    User loggedInUser;
    private SharedPreferences loginPreferences;
    private SharedPreferences.Editor loginPrefsEditor;
    ImageView profileImage;
    ProgressBar pgsBar;
    String viewedUserID;
    protected View mView;

    public UserProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment UserProfileFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static UserProfileFragment newInstance(String param1, String param2) {
        UserProfileFragment fragment = new UserProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_user_profile, container, false);
        this.mView = v;

        //Progress
        pgsBar = mView.findViewById(R.id.progressBar);
        Sprite doubleBounce = new DoubleBounce();
        pgsBar.setIndeterminateDrawable(doubleBounce);

        FloatingActionButton settings = (FloatingActionButton) mView.findViewById(R.id.SettingsButton);
        settings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            }
        });
        //Get viewed User ID
//        final ConstraintLayout constraintLayout = findViewById(R.id.Layout);
//        constraintLayout.setOnTouchListener(new OnSwipeTouchListener(NutzerkontoActivity.this) {
//            public void onSwipeRight() {
//                Intent intent = new Intent(NutzerkontoActivity.this, Dashboard_Activity.class);
//                intent.putExtra(getString(R.string.usernameParameter), ParseUser.getCurrentUser().getUsername());
//                intent.putExtra(getString(R.string.viewedUserParameter), ParseUser.getCurrentUser().getUsername());
//                startActivity(intent);
//                overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
//            }
//
//        });
        //One way to pass the user by using intents, Another option might be a static reference somewhere or making users Serializable
        //String loggedUserName = this.getIntent().getStringExtra(getString(R.string.usernameParameter));

        viewedUserID = getActivity().getIntent().getStringExtra(getString(R.string.viewedUserParameter));
        if (viewedUserID == null) {
            //was not send via environment, so get current user from database
            viewedUserID = ParseUser.getCurrentUser().getObjectId();
        }
        Button updateButton = mView.findViewById(R.id.updateButton);
        logoutButton = mView.findViewById(R.id.logoutButton);

        //Load userdata and on success assign values.
        new Load_UserData().execute(viewedUserID); //TODO IMPORTANT ACCESS IS BLOCKED BECAUSE EXCEPTIon
        FloatingActionButton backToOwnProfile = (FloatingActionButton) v.findViewById(R.id.backArrow);
        if (viewedUserID != ParseUser.getCurrentUser().getObjectId()) {

            backToOwnProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateButton.setVisibility(View.VISIBLE);
                    logoutButton.setVisibility(View.VISIBLE);
                    new Load_UserData().execute(ParseUser.getCurrentUser().getObjectId());
                    getActivity().getIntent().putExtra(getString(R.string.viewedUserParameter), ParseUser.getCurrentUser().getObjectId());
                    viewedUserID = ParseUser.getCurrentUser().getObjectId();
                    backToOwnProfile.setVisibility(View.INVISIBLE);
                }
            });
        } else {
            backToOwnProfile.setVisibility(View.INVISIBLE);
        }




        profileImage = mView.findViewById(R.id.Profilbild);
        //profileImage.setImageDrawable(viewedUser.getImage()); //TODO Cant nicely test without DB
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhoto, 1);
            }
        });




        //To reset saved credentials
        loginPreferences = getActivity().getSharedPreferences("loginPrefs", MODE_PRIVATE);
        loginPrefsEditor = loginPreferences.edit();

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginPrefsEditor.clear();
                loginPrefsEditor.commit();
                ParseUser.logOut();
                Intent intent = new Intent(view.getContext(), LoginActivity.class);
                startActivity(intent);
            }
        });

        if(!((ParseUser.getCurrentUser().getObjectId()) == (viewedUserID))){
            updateButton.setVisibility(View.GONE);
            logoutButton.setVisibility(View.GONE);
        }
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateUserInformation();
            }
        });

//        BottomNavigationView bottomMenu = findViewById(R.id.BottomMenu);
//        bottomMenu.setSelectedItemId(R.id.Konto);
//        bottomMenu.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
//            @Override
//            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
//                switch(item.getItemId()){
//                    case R.id.Dashboard:
//                        Intent intent = new Intent(getActivity(), Dashboard_Activity.class);
//                        intent.putExtra(getString(R.string.usernameParameter), ParseUser.getCurrentUser().getUsername());
//                        intent.putExtra(getString(R.string.viewedUserParameter), ParseUser.getCurrentUser().getUsername());
//                        startActivity(intent);
//                        return true;
//                    case R.id.Umgebung:
//                        Intent intent1 = new Intent(NutzerkontoActivity.this, EnvironmentActivity.class);
//                        intent1.putExtra(getString(R.string.usernameParameter), ParseUser.getCurrentUser().getUsername());
//                        intent1.putExtra(getString(R.string.viewedUserParameter), ParseUser.getCurrentUser().getUsername());
//                        startActivity(intent1);
//                        return true;
//                }
//                return false;
//            }
//        });

        return v;
    }


    //For saving image on pick
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getActivity().getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String mCurrentPhotoPath = cursor.getString(columnIndex);
            cursor.close();

            byte[] image = null;

            try {
                image = readInFile(mCurrentPhotoPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //Set for now
            Bitmap imageBitmap = BitmapFactory.decodeByteArray(image, 0, image.length); //Bitmap with profile picture
            profileImage.setImageBitmap(imageBitmap);

            ParseFile file = new ParseFile(ParseUser.getCurrentUser().getUsername() + ".png", image);
            file.saveInBackground();
            ParseUser currentUser = ParseUser.getCurrentUser();
            currentUser.put(getString(R.string.uKeyProfileImage), file);
            currentUser.saveInBackground();
        }

    }

    private byte[] readInFile(String path) throws IOException {

        byte[] data = null;
        File file = new File(path);
        InputStream input_stream = new BufferedInputStream(new FileInputStream(file));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        data = new byte[16384]; // 16K
        int bytes_read;

        while ((bytes_read = input_stream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytes_read);
        }

        input_stream.close();
        return buffer.toByteArray();
    }

    public void updateUserInformation() {
        //Get the user form the database
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo(getString(R.string.uKeyID), ParseUser.getCurrentUser().getObjectId());
        query.findInBackground(new FindCallback<ParseUser>() {
            //response is a list, but can only be one user, so take first
            public void done(List<ParseUser> users, ParseException e) {
                if (e == null) {
                    // The query was successful.
                    ParseUser owner = users.get(0);
                    TextView beschreibung = mView.findViewById(R.id.Beschreibung);
                    owner.put(getString(R.string.uKeyDescription), beschreibung.getText().toString());
                    TextView email = mView.findViewById(R.id.EMail);
                    if(!email.getText().toString().equals("")) {
                        owner.put(getString(R.string.uKeyEmail), email.getText().toString());
                    }
                    ProgressBar pg = mView.findViewById(R.id.LevelBar);
                    owner.put(getString(R.string.uKeyEXP), pg.getProgress());
                    TextView levelDisplay = mView.findViewById(R.id.Level);
                    owner.put(getString(R.string.uKeyLevel), Integer.parseInt(levelDisplay.getText().toString()));
                    owner.saveInBackground();
                    Toast.makeText(getContext(), "Speichern Erfolgreich", Toast.LENGTH_SHORT).show();
                    //Technically incorrect, the save could still fail, but thats unlikely at this point.
                } else {
                    // Something went wrong.
                }
            }
        });
    }

    private class Load_UserData extends AsyncTask<String, String, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pgsBar.setVisibility(View.VISIBLE);
        }

        protected Void doInBackground(String... param) {
            //Get the user form the database
            ParseQuery<ParseUser> query = ParseUser.getQuery();
            query.whereEqualTo(getString(R.string.uKeyID), param[0]);
            query.findInBackground(new FindCallback<ParseUser>() {
                //response is a list, but can only be one user, so take first
                public void done(List<ParseUser> users, ParseException e) {
                    if (e == null) {
                        // The query was successful.
                        ParseUser foundUser = users.get(0);
                        //Create User and assign values from database user.
                        User viewedUser = new User();
                        viewedUser.setUsrname(foundUser.getUsername());
                        viewedUser.setDescription(foundUser.getString(getString(R.string.uKeyDescription)));
                        viewedUser.setExpEarned(foundUser.getInt(getString(R.string.uKeyEXP)));
                        viewedUser.setLevel(foundUser.getInt(getString(R.string.uKeyLevel)));
                        viewedUser.setMail(foundUser.getString(getString(R.string.uKeyEmail)));
                        boolean hasLeveled = viewedUser.addEXP(0); // this causes the level to update


                        //Image
                        ParseFile profilePic = foundUser.getParseFile(getString(R.string.uKeyProfileImage));
                        byte[] byteArray = new byte[0];

                        try {
                            byteArray = profilePic.getData();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        Bitmap imageBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length); //Bitmap with profile picture
                        if(imageBitmap != null  && imageBitmap.getByteCount() > 0){
                        profileImage.setImageBitmap(imageBitmap);

                        }
                        else{
                            profileImage.setImageDrawable(getActivity().getDrawable(R.drawable.ic_baseline_account_circle_24_grey));
                        }

                        //Set UI information
                        boolean ownProfile = viewedUser.getUsrname().equals(ParseUser.getCurrentUser().getUsername());

                        //Static information:
                        TextView nameText = mView.findViewById(R.id.Name);
                        nameText.setText(viewedUser.getUsrname());

                        //Editable Information:
                        EditText descriptionText = mView.findViewById(R.id.Beschreibung);
                        descriptionText.setEnabled(ownProfile);
                        descriptionText.setText(viewedUser.getDescription());

                        EditText emailField = mView.findViewById(R.id.EMail);
                        emailField.setText(viewedUser.getMail());
                        emailField.setEnabled(ownProfile);

                        TextView levelDisplay = mView.findViewById(R.id.Level);
                        levelDisplay.setText("" + viewedUser.getLevel());

                        ProgressBar levelBar = mView.findViewById(R.id.LevelBar);
                        levelBar.setMin(0); //To purge potential previous parameters
                        levelBar.setMax(viewedUser.getExpToNextLevel());
                        levelBar.setMin(User.getExpToNextLevel(viewedUser.getLevel() -1));
                        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(levelBar, "progress", levelBar.getMin(), viewedUser.getExpEarned());
                        progressAnimator.setDuration(1000);
                        progressAnimator.setInterpolator(new LinearInterpolator());
                        progressAnimator.start();
                        //levelBar.setProgress(viewedUser.getExpEarned());
                        if(hasLeveled){
                            updateUserInformation();
                        }
                        onPostExecute("");


                    } else {
                        // Something went wrong.
                    }
                }
            });
            return null;
        }

        protected void onPostExecute(String arg) {
            pgsBar.setVisibility(View.GONE);
        }
    }


//    @Override
//    public  void onBackPressed(){
//        Intent startDashboard = new Intent(getActivity(), Dashboard_Activity.class);
//        startDashboard.putExtra(getString(R.string.usernameParameter), ParseUser.getCurrentUser().getUsername());
//        startDashboard.putExtra("viewedUser", ParseUser.getCurrentUser().getUsername());
//        startActivity(startDashboard);
//        overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
//    }

//    /**
//     * Inflate Profile Menu
//     *
//     * @param menu
//     * @return
//     */
//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        inflater.inflate(R.menu.menu_useraccount, menu);
////        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle item selection
//        switch (item.getItemId()) {
//            case R.id.SettingsButton:
//                Intent intent = new Intent(getActivity(), SettingsActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//                startActivity(intent);
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }

//    @Override
//    public void onResume() {
//        super.onResume();
//        BottomNavigationView bottomMenu = mView.findViewById(R.id.BottomMenu);
//        bottomMenu.getMenu().getItem(2).setChecked(true);
//    }
}