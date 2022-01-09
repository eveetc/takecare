package com.mad.takecare.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mad.takecare.MainActivity;
import com.mad.takecare.R;
import com.mad.takecare.data.SharedPrefencesManager;
import com.mad.takecare.model.Task;
import com.mad.takecare.model.User;
import com.mad.takecare.ui.login.LoginActivity;
import com.mikepenz.iconics.IconicsColor;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSize;
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.Calendar;
import java.util.Date;

public class AufgabenAnsichtActivity extends AppCompatActivity {
    AufgabenAccess accessLevel;
    User loggedInUser;
    Task viewedTask;
    private Intent mapActivity;
    private TextView ort;
    private String accessStng;
    //Button dueTime;
    private Spinner kategorieSpinner;

    //On app start, if not logged in behave differently
    boolean loggedIn;
    //To choose
    EditText selectDate, selectTime;
    private int mYear, mMonth, mDay, mHour, mMinute;

    MapView map = null;
    private Location recentLocation = new Location("recentLocation");
    private IMapController mapController;
    private Marker taskAdressMarker;
    private Context ctx;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aufgaben);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

//        Toolbar myToolbar = (Toolbar) findViewById(R.id.AufgabenToolbar);
//        setSupportActionBar(myToolbar);

        if (ParseUser.getCurrentUser() != null) {
            loggedIn = true;
        } else {
            loggedIn = false;
        }

        //Determine Layout access level
        viewedTask = (Task) this.getIntent().getSerializableExtra("task");
        if (viewedTask.getLocation() == null) {
            if (viewedTask.getLocationLat() == null) {
                viewedTask.setLocation(new Location("recentLocation"));
            } else {
                Location location = new Location("");
                location.setLatitude(Double.parseDouble(viewedTask.getLocationLat()));
                location.setLongitude(Double.parseDouble(viewedTask.getLocationLong()));
                viewedTask.setLocation(location);
            }
        }


        //Determine Layout access level
        accessStng = this.getIntent().getStringExtra(getString(R.string.accessLevelParameter));
        switch (accessStng) { //TODO Why are we passing the accessLevel as a string instead of an Integer?
            case "FREE":
                accessLevel = AufgabenAccess.FREE;
                break;
            case "OWNED":
                accessLevel = AufgabenAccess.OWNED;
                break;
            case "ACCEPTED":
                accessLevel = AufgabenAccess.ACCEPTED;
                break;
            case "NEW":
                accessLevel = AufgabenAccess.NEW;
                viewedTask = new Task();
                break;
        }

        handleMiniMap();

        //For time
        selectDate = (EditText) findViewById(R.id.date);
        selectTime = (EditText) findViewById(R.id.time);

        FloatingActionButton button = findViewById(R.id.backArrow);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (loggedIn) {
//                    Intent intent = new Intent(AufgabenAnsichtActivity.this, MainActivity.class);
//                    intent.putExtra(getString(R.string.usernameParameter), ParseUser.getCurrentUser().getUsername());
//                    intent.putExtra(getString(R.string.viewedUserParameter), ParseUser.getCurrentUser().getUsername());
//                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//                    startActivity(intent);
//                } else {
//                    Intent intent = new Intent(AufgabenAnsichtActivity.this, MainActivity.class);
//                    startActivity(intent);
//                }
                if (loggedIn) {
                    DashboardFragment.getInstance().refreshLists();
                }
                finish();
            }
        });


        //region basic setup
        final EditText titleText = findViewById(R.id.AufgabenTitel);
        titleText.setText(viewedTask.getTaskName());
        titleText.setEnabled(false);
        titleText.setTextColor(getColor(R.color.primary_text));

        mapActivity = new Intent(AufgabenAnsichtActivity.this, MapActivity.class);

//        ort = findViewById(R.id.Ort);
//        ort.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_location_on_24, 0, 0, 0);
//        ort.setText(viewedTask.getLocationName());

        final EditText description = findViewById(R.id.Beschreibung);
        description.setText(viewedTask.getDescription());
        description.setEnabled(false);
        description.setTextColor(getColor(R.color.primary_text));

        final ScrollView scrollView = findViewById(R.id.content);

        final CheckBox weekly = findViewById(R.id.Wöchentlich_Checkbox);
        weekly.setChecked(viewedTask.isWeekly());
        weekly.setEnabled(false);


        final EditText additionalInfo = findViewById(R.id.Zusatzinformationen);
        additionalInfo.setVisibility(View.GONE);
        additionalInfo.setEnabled(false);
        additionalInfo.setImeOptions(EditorInfo.IME_ACTION_DONE);
        additionalInfo.setRawInputType(InputType.TYPE_CLASS_TEXT);
        additionalInfo.setAlpha(1);
        additionalInfo.setTextColor(getColor(R.color.primary_text));

        Calendar c = Calendar.getInstance();
        if (viewedTask.getDueTime() != null) {
            c.setTime(viewedTask.getDueTime());
            if (c.get(Calendar.MINUTE) < 10) {
                selectTime.setText(c.get(Calendar.HOUR_OF_DAY) + ":0" + c.get(Calendar.MINUTE));
            } else {
                selectTime.setText(c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE));
            }

            if (c.get(Calendar.YEAR) < 10) {
                selectDate.setText(c.get(Calendar.DAY_OF_MONTH) + "-" + (c.get(Calendar.MONTH) + 1) + "-0" + c.get(Calendar.YEAR));
            } else {
                selectDate.setText(c.get(Calendar.DAY_OF_MONTH) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.YEAR));
            }

        }
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        mHour = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE);

        final Button leftLowerButton = findViewById(R.id.TaskLeftButton);
        final Button rightLowerButton = findViewById(R.id.TaskRightButton);
        final View buttons = findViewById(R.id.buttons);
//        map.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mapActivity.putExtra("taskLocation", viewedTask.getLocation());
//                mapActivity.putExtra("access", accessStng);
//                startActivity(mapActivity);// no onresult needed, user just views location
//            }
//        });
        kategorieSpinner = findViewById(R.id.Spinner_Kategorie);
        ArrayAdapter<CharSequence> kategorieSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.categoriesCreation, R.layout.support_simple_spinner_dropdown_item);
        kategorieSpinnerAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        kategorieSpinner.setAdapter(kategorieSpinnerAdapter);

        int numberOfSpinnerItems = kategorieSpinner.getAdapter().getCount();
        for (int i = 0; i < numberOfSpinnerItems; i++) {
            if (kategorieSpinner.getAdapter().getItem(i).toString().equals(viewedTask.getCategory())) {
                kategorieSpinner.setSelection(i);
                break;
            }
        }
        kategorieSpinner.setEnabled(false);



        FloatingActionButton userAccountFromTask = findViewById(R.id.UserAccountFromTask);
        if(ParseUser.getCurrentUser() == null){
            userAccountFromTask.setEnabled(false);
            userAccountFromTask.setScaleX(0);
            userAccountFromTask.setScaleY(0);
        }
        userAccountFromTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AufgabenAnsichtActivity.this, MainActivity.class);
                intent.putExtra(getString(R.string.usernameParameter), ParseUser.getCurrentUser().getObjectId());
                intent.putExtra(getString(R.string.viewedUserParameter), viewedTask.getOwnerID());
                intent.putExtra(getString(R.string.fragmentPositionParameter), 3);
                //intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            }
        });


        //endregion

        //region Stacking Setup
        if (accessLevel.compareTo(AufgabenAccess.ACCEPTED) >= 0) {
            additionalInfo.setVisibility(View.VISIBLE);
            userAccountFromTask.setVisibility(View.VISIBLE);
            findViewById(R.id.task_activity_title).setVisibility(View.GONE);
            additionalInfo.setText(viewedTask.getAdditionalInfo());
        }
        if (accessLevel.compareTo(AufgabenAccess.OWNED) >= 0) {
            userAccountFromTask.setVisibility(View.GONE);
            findViewById(R.id.task_activity_title).setVisibility(View.VISIBLE);
            titleText.setEnabled(true);
            weekly.setEnabled(true);
            description.setEnabled(true);
            additionalInfo.setEnabled(true);
            kategorieSpinner.setEnabled(true);
//            map.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    mapActivity.putExtra("taskLocation", viewedTask.getLocation());
//                    mapActivity.putExtra("access", accessStng);
//                    startActivityForResult(mapActivity, 1); //onresult just if owner of task
//                }
//            });
        }

        //endregion
        //region unique Setup
        if (accessLevel == AufgabenAccess.FREE) {
            userAccountFromTask.setVisibility(View.VISIBLE);
            findViewById(R.id.task_activity_title).setVisibility(View.GONE);

            leftLowerButton.setVisibility(View.GONE);
            if (loggedIn) {
                rightLowerButton.setText("Annehmen");
                rightLowerButton.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.ic_baseline_add_white_24), null, null, null);
                // I shouldn't have to do this. I don't want to do this. But I couldn't figure out how else to do it.
//                rightLowerButton.animate().translationX(-275);
//                rightLowerButton.animate().setDuration(0);
                rightLowerButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //loggedInUser.getAcceptedTasks().add(viewedTask);
                        //database.addOrUpdate(loggedInUser);
                        AcceptTask(viewedTask);
                    }
                });
            } else {
                rightLowerButton.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.ic_baseline_add_white_24), null, null, null);
                rightLowerButton.setText("Einloggen zum Aufgabe annehmen.");
//                rightLowerButton.animate().translationX(-275);
//                rightLowerButton.animate().setDuration(0);
                rightLowerButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        startActivity(intent);
                    }
                });
            }


        } else if (accessLevel == AufgabenAccess.ACCEPTED) {
            leftLowerButton.setText("Aufgabe Abgeschlossen");
            leftLowerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //viewedTask.setCompletedMarker(true);
                    //database.addOrUpdate(viewedTask);
                    SetTaskCompleteMarker(viewedTask);
                }
            });

            rightLowerButton.setText("Zusage Zurücknehmen");
            rightLowerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //loggedInUser.getAcceptedTasks().remove(viewedTask);
                    //database.addOrUpdate(loggedInUser);
                    DeacceptTask(viewedTask);
                }
            });
        } else if (accessLevel == AufgabenAccess.OWNED) {
            selectDate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Calendar c = Calendar.getInstance();
                    mYear = c.get(Calendar.YEAR);
                    mMonth = c.get(Calendar.MONTH);
                    mDay = c.get(Calendar.DAY_OF_MONTH);

                    DatePickerDialog datePickerDialog = new DatePickerDialog(AufgabenAnsichtActivity.this,
                            new DatePickerDialog.OnDateSetListener() {

                                @Override
                                public void onDateSet(DatePicker view, int year,
                                                      int monthOfYear, int dayOfMonth) {

                                    selectDate.setText(dayOfMonth + "-" + (monthOfYear + 1) + "-" + year);

                                    if (year < 10) {
                                        selectDate.setText(dayOfMonth + "-" + (monthOfYear + 1) + "-0" + year);
                                    }

                                    mYear = year;
                                    mMonth = monthOfYear;
                                    mDay = dayOfMonth;
                                }
                            }, mYear, mMonth, mDay);
                    datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
                    datePickerDialog.show();
                }
            });
            selectTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Get Current Time
                    Calendar c = Calendar.getInstance();
                    mHour = c.get(Calendar.HOUR_OF_DAY);
                    mMinute = c.get(Calendar.MINUTE);

                    // Launch Time Picker Dialog
                    TimePickerDialog timePickerDialog = new TimePickerDialog(AufgabenAnsichtActivity.this,
                            new TimePickerDialog.OnTimeSetListener() {
                                @Override
                                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                    selectTime.setText(hourOfDay + ":" + minute);
                                    if (minute < 10) {
                                        selectTime.setText(hourOfDay + ":0" + minute);
                                    }
                                    mHour = hourOfDay;
                                    mMinute = minute;
                                }
                            }, mHour, mMinute, true);
                    timePickerDialog.show();
                }
            });

            rightLowerButton.setText(getString(R.string.savechanges));
            rightLowerButton.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.ic_baseline_done_white_24), null, null, null);
            rightLowerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //database.addOrUpdate(viewedTask);
                    viewedTask.setWeekly(weekly.isChecked());
                    viewedTask.setTaskName(titleText.getText().toString());
                    viewedTask.setDescription(description.getText().toString());
                    viewedTask.setCategory(kategorieSpinner.getSelectedItem().toString());
                    viewedTask.setAdditionalInfo(additionalInfo.getText().toString());

                    Date dueDate = getDate(mYear, mMonth, mDay, mHour, mMinute);
                    viewedTask.setDueTime(dueDate);


                    //location update set on result of mapactivity
                    UpdateTask(viewedTask);
                }

            });
            leftLowerButton.setText(R.string.abschliessen);
            leftLowerButton.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.ic_baseline_star_24), null, null, null);
            leftLowerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //viewedTask.setCompleted(true);
                    //database.addOrUpdate(viewedTask);
                    CompleteTask(viewedTask);
                }
            });


        } else if (accessLevel == AufgabenAccess.NEW) {
            leftLowerButton.setText("Verwerfen");
            leftLowerButton.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.ic_baseline_close_24), null, null, null);
            leftLowerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    BackToDashboard();
                }
            });
            rightLowerButton.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.ic_baseline_done_white_24), null, null, null);
            rightLowerButton.setText("Anlegen");
            rightLowerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (ValidateTextEntry(titleText) && ValidateTextEntry(description) && ValidateTextEntry(selectDate) && ValidateTextEntry(selectTime)) {
                        viewedTask.setWeekly(weekly.isChecked());
                        viewedTask.setTaskName(titleText.getText().toString());
                        viewedTask.setDescription(description.getText().toString());
                        viewedTask.setAdditionalInfo(additionalInfo.getText().toString());
                        Date dueDate = getDate(mYear, mMonth, mDay, mHour, mMinute);
                        viewedTask.setDueTime(dueDate);
                        SaveNewTask(viewedTask);
                        //BackToDashboard();
                    }
                }
            });

            selectDate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Calendar c = Calendar.getInstance();
                    mYear = c.get(Calendar.YEAR);
                    mMonth = c.get(Calendar.MONTH);
                    mDay = c.get(Calendar.DAY_OF_MONTH);

                    DatePickerDialog datePickerDialog = new DatePickerDialog(AufgabenAnsichtActivity.this,
                            new DatePickerDialog.OnDateSetListener() {

                                @Override
                                public void onDateSet(DatePicker view, int year,
                                                      int monthOfYear, int dayOfMonth) {

                                    selectDate.setText(dayOfMonth + "-" + (monthOfYear + 1) + "-" + year);
                                    mYear = year;
                                    mMonth = monthOfYear;
                                    mDay = dayOfMonth;
                                }
                            }, mYear, mMonth, mDay);
                    datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
                    datePickerDialog.show();
                }
            });
            selectTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Get Current Time
                    Calendar c = Calendar.getInstance();
                    mHour = c.get(Calendar.HOUR_OF_DAY);
                    mMinute = c.get(Calendar.MINUTE);

                    // Launch Time Picker Dialog
                    TimePickerDialog timePickerDialog = new TimePickerDialog(AufgabenAnsichtActivity.this,
                            new TimePickerDialog.OnTimeSetListener() {
                                @Override
                                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                    UpdateTimeDisplay(hourOfDay, minute);
                                    mHour = hourOfDay;
                                    mMinute = minute;
                                }
                            }, mHour, mMinute, true);
                    timePickerDialog.show();
                }
            });

        }
        //endregion


    }

    /**
     * Updates the Time-display with the given value, adds 0-padding if necessary
     *
     * @param minutes
     */
    public void UpdateTimeDisplay(int hours, int minutes) {
        String minuteString = minutes < 10 ? "0" + minutes : "" + minutes;
        selectTime.setText(hours + ":" + minuteString);
    }


    public boolean ValidateTextEntry(TextView t) {
        if (t.getText().toString().equals("")) {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.shake_error);
            t.startAnimation(animation);
            return false;
        } else {
            return true;
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.menu_tasks, menu);
//        return true;
//    }

//    /**
//     * Handle EnvironmentMenus Button Clicks
//     *
//     * @param item
//     * @return
//     */
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle item selection
//        switch (item.getItemId()) {
//            case R.id.UserAccountFromTask:
//                Intent intent = new Intent(this, MainActivity.class);
//                intent.putExtra(getString(R.string.usernameParameter), ParseUser.getCurrentUser().getObjectId());
//                intent.putExtra(getString(R.string.viewedUserParameter), viewedTask.getOwnerID());
//                intent.putExtra(getString(R.string.fragmentPositionParameter), 3);
//                //intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//                startActivity(intent);
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }

    private Date getDate(int year, int month, int day, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public void SaveNewTask(Task task) {
        //Save Task itself
        final ParseObject taskObject = new ParseObject("Task");
        taskObject.put(getString(R.string.keyTaskName), task.getTaskName());
        taskObject.put(getString(R.string.keyCompletedMarker), false);
        taskObject.put(getString(R.string.keyCompleted), false);
        //Create a relation, every task has an owner.
        taskObject.put(getString(R.string.keyOwner), ParseUser.getCurrentUser());
        taskObject.put(getString(R.string.keyDescription), task.getDescription());
        taskObject.put(getString(R.string.keyAdditionalInfo), task.getAdditionalInfo());
        taskObject.put(getString(R.string.keyWeekly), task.isWeekly());
        taskObject.put(getString(R.string.keyCategory), kategorieSpinner.getSelectedItem());
        taskObject.put(getString(R.string.keyPointValue), 0);

        //Date can be saved directly into db
        taskObject.put(getString(R.string.keyDue), task.getDueTime());

        //Location values, whole location as an object cant be saved into db
        try {
            taskObject.put(getString(R.string.keyLocationLat), task.getLocation().getLatitude());
            taskObject.put(getString(R.string.keyLocationLong), task.getLocation().getLongitude());
            taskObject.put(getString(R.string.keyLocationName), task.getLocationName());
        } catch (Exception eeee) {
            Log.e("Locationerror", "location null? " + eeee);
        }


        taskObject.saveInBackground(new SaveCallback() {

            @Override
            public void done(ParseException e) {
                // TODO Toast message on failure
                if (e == null) {
                    Log.e("TAG ", "Success .... ");
                    BackToDashboard();
                } else {
                    Log.e("TAG ", "faillll  ....  " + e.getMessage() + " Code  " + e.getCode());
                }
            }
        });

    }

    public void BackToDashboard() {
        DashboardFragment.getInstance().refreshLists();
        finish();
        EnvironmentFragment.getInstance().refreshList();
    }

    public void UpdateTask(final Task pTask) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Task");
        //Set on Task
        query.getInBackground(pTask.getID(), new GetCallback<ParseObject>() {
            public void done(ParseObject task, ParseException e) {
                if (e == null) {
                    task.put(getString(R.string.keyTaskName), pTask.getTaskName());
                    task.put(getString(R.string.keyCompleted), false);
                    //Create a relation, every task has an owner.
                    task.put(getString(R.string.keyOwner), ParseUser.getCurrentUser());
                    task.put(getString(R.string.keyWeekly), pTask.isWeekly());
                    task.put(getString(R.string.keyDescription), pTask.getDescription());
                    task.put(getString(R.string.keyAdditionalInfo), pTask.getAdditionalInfo());
                    task.put(getString(R.string.keyCategory), pTask.getCategory());
                    task.put(getString(R.string.keyAccepter), "none");

                    //Date can be saved directly into db
                    task.put("due", pTask.getDueTime());

                    //Location values, whole location as an object cant be saved into db
                    try {
                        task.put(getString(R.string.keyLocationLat), pTask.getLocation().getLatitude());
                        task.put(getString(R.string.keyLocationLong), pTask.getLocation().getLongitude());
                        task.put(getString(R.string.keyLocationName), pTask.getLocationName());
                    } catch (Exception eeee) {
                        Log.e("Locationerror", "location null? " + eeee);
                    }

                    task.saveInBackground(new SaveCallback() {

                        @Override
                        public void done(ParseException e) {
                            // TODO Toast message on failure
                            if (e == null) {
                                Log.e("TAG ", "Success .... ");
                                BackToDashboard();
                            } else {
                                Log.e("TAG ", "faillll  ....  " + e.getMessage() + " Code  " + e.getCode());
                            }
                        }
                    });
                } else {
                    Log.e("ParseException", "Error: " + e.getMessage());
                }
            }
        });
    }

    public void AcceptTask(Task task) {
        if (ParseUser.getCurrentUser().getObjectId().equals(task.getOwnerID())) {
            return;
        }
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Task");
        //Set on Task
        query.getInBackground(task.getID(), new GetCallback<ParseObject>() {
            public void done(ParseObject taskParseObject, ParseException e) {
                if (e == null) {
                    taskParseObject.put(getString(R.string.keyAccepter), ParseUser.getCurrentUser().getObjectId());
                    taskParseObject.put(getString(R.string.keyPointValue), task.getPointValue());
                    taskParseObject.saveInBackground(new SaveCallback() {

                        @Override
                        public void done(ParseException e) {
                            // TODO Toast message on failure
                            if (e == null) {
                                Log.e("TAG ", "Success .... ");
                                BackToDashboard();
                            } else {
                                Log.e("TAG ", "faillll  ....  " + e.getMessage() + " Code  " + e.getCode());
                            }
                        }
                    });
                } else {
                    Log.e("ParseException", "Error: " + e.getMessage());
                }
            }
        });
    }

    public void SetTaskCompleteMarker(Task task) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Task");
        //Set on Task
        query.getInBackground(task.getID(), new GetCallback<ParseObject>() {
            public void done(ParseObject task, ParseException e) {
                if (e == null) {
                    task.put(getString(R.string.keyCompletedMarker), true);
                    task.saveInBackground(new SaveCallback() {

                        @Override
                        public void done(ParseException e) {
                            // TODO Toast message on failure
                            if (e == null) {
                                Log.e("TAG ", "Success .... ");
                                BackToDashboard();
                            } else {
                                Log.e("TAG ", "faillll  ....  " + e.getMessage() + " Code  " + e.getCode());
                            }
                        }
                    });
                } else {
                    Log.e("ParseException", "Error: " + e.getMessage());
                }
            }
        });
    }

    public void CompleteTask(Task task) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Task");
        //Set on Task
        query.getInBackground(task.getID(), new GetCallback<ParseObject>() {
            public void done(ParseObject task, ParseException e) {
                if (e == null) {
                    task.put(getString(R.string.keyCompleted), true);
                    task.saveInBackground(new SaveCallback() {

                        @Override
                        public void done(ParseException e) {
                            // TODO Toast message on failure
                            if (e == null) {
                                Log.e("TAG ", "Success .... ");
                                BackToDashboard();
                            } else {
                                Log.e("TAG ", "faillll  ....  " + e.getMessage() + " Code  " + e.getCode());
                            }
                        }
                    });
                } else {
                    Log.e("ParseException", "Error: " + e.getMessage());
                }
            }
        });
    }

    public void DeacceptTask(Task task) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Task");
        //Set on Task
        query.getInBackground(task.getID(), new GetCallback<ParseObject>() {
            public void done(ParseObject task, ParseException e) {
                if (e == null) {
                    task.put(getString(R.string.keyAccepter), "none");
                    task.put(getString(R.string.keyPointValue), 0); //Tasks calculate their current Point Value on being accepted
                    task.saveInBackground(new SaveCallback() {

                        @Override
                        public void done(ParseException e) {
                            // TODO Toast message on failure
                            if (e == null) {
                                Log.e("TAG ", "Success .... ");
                                BackToDashboard();
                            } else {
                                Log.e("TAG ", "faillll  ....  " + e.getMessage() + " Code  " + e.getCode());
                            }
                        }
                    });
                } else {
                    Log.e("ParseException", "Error: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Called when MapActivity is finished, receives set location & locationname
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    if (data.getExtras().containsKey(getString(R.string.locationNameParameter))) {
                        String locationName = data.getStringExtra("locationName");
//                        ort.setText(locationName);
                        viewedTask.setLocationName(locationName);
                    }
                    if (data.getExtras().containsKey(getString(R.string.taskLocationParameter))) {
                        Location taskLocation = data.getParcelableExtra(getString(R.string.taskLocationParameter));
                        viewedTask.setLocationLat(String.valueOf(taskLocation.getLatitude()));
                        viewedTask.setLocationLong(String.valueOf(taskLocation.getLongitude()));
                        viewedTask.setLocation(taskLocation);
                    }
                    handleMiniMap();
//                Toast.makeText(this, String.valueOf(radius), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void handleMiniMap() {
        map = findViewById(R.id.Ort);
        final String[] tileURLs = {"http://194.55.13.214:8090/styles/basic-preview/"};
        final ITileSource tileSource = new XYTileSource("Eve's Tile Server",
                6,
                18,
                256,
                ".png",
                tileURLs,
                "from OpenMapTiles Map Server");
        map.setTileSource(tileSource);
        map.getOverlayManager().getTilesOverlay().setLoadingBackgroundColor(android.R.color.white);
        map.setMultiTouchControls(false);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        mapController = map.getController();
        mapController.setZoom(15.0);
        map.setHasTransientState(false);
        map.getOverlays().remove(taskAdressMarker);

        map.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Boolean detachedMode = true;
                if (detachedMode) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (accessLevel.compareTo(AufgabenAccess.OWNED) >= 0) {
                            mapActivity.putExtra("taskLocation", viewedTask.getLocation());
                            mapActivity.putExtra("access", accessStng);
                            startActivityForResult(mapActivity, 1); //onresult just if owner of task

                        } else {
                            String label = viewedTask.getTaskName();
                            String uriBegin = "geo:" + viewedTask.getLocation().getLatitude() +
                                    "," + viewedTask.getLocation().getLongitude();
                            String query = viewedTask.getLocation().getLatitude() +
                                    "," + viewedTask.getLocation().getLongitude() + "(" + label + ")";
                            String encodedQuery = Uri.encode(query);
                            String uriString = uriBegin + "?q=" + encodedQuery;
                            Uri uri = Uri.parse(uriString);
                            Intent intent = new Intent(android.content.Intent.ACTION_VIEW, uri);
                            startActivity(intent);
                        }
                    }
                    // Is detached mode is active all other touch handler
                    // should not be invoked, so just return true
                    return true;
                }
                return false;
            }
        });
        taskAdressMarker = new Marker(map);
        taskAdressMarker.setIcon(new IconicsDrawable(this).icon(FontAwesome.Icon.faw_map_pin)
                .color(IconicsColor.colorInt(getColor(R.color.taskLocation))).size(IconicsSize.dp(20)));

        taskAdressMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        taskAdressMarker.setInfoWindow(null);
        if (viewedTask.getLocation() != null) {
            GeoPoint taskGeopoint = new GeoPoint(viewedTask.getLocation().getLatitude(), viewedTask.getLocation().getLongitude());
            mapController.setCenter(taskGeopoint);
            taskAdressMarker.setPosition(taskGeopoint);
        } else {
            // set default location -> recent location
            String[] sharedRecentLocation = SharedPrefencesManager.getInstance(getApplicationContext()).getRecentLocArray();
            if (sharedRecentLocation[0] != null) {
                double lat = Double.parseDouble(sharedRecentLocation[0]);
                double lon = Double.parseDouble(sharedRecentLocation[1]);
                GeoPoint taskGeopoint = new GeoPoint(lat, lon);
                mapController.setCenter(taskGeopoint);
                taskAdressMarker.setPosition(taskGeopoint);

                Location taskLocation = new Location("taskLocation");
                taskLocation.setLatitude(lat);
                taskLocation.setLongitude(lon);
                viewedTask.setLocationLat(String.valueOf(lat));
                viewedTask.setLocationLong(String.valueOf(lon));
                viewedTask.setLocation(taskLocation);
            }
        }
        map.getOverlays().add(taskAdressMarker);
        map.invalidate();
    }
}


enum AufgabenAccess {
    FREE, ACCEPTED, OWNED, NEW
}


