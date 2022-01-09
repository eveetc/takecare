package com.mad.takecare.ui;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.DoubleBounce;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.mad.takecare.R;
import com.mad.takecare.data.SharedPrefencesManager;
import com.mad.takecare.model.Task;
import com.mad.takecare.ui.login.LoginActivity;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.parse.Parse.getApplicationContext;
import static java.lang.Integer.parseInt;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EnvironmentFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EnvironmentFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final int MAX_RADIUS = 15000;
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    protected View mView;
    private Location recentLocation = new Location("recentLocation");
    private int radius;
    private Intent mapActivity;
    private EnvironmentAdapter adapter;
    private HashMap<Integer, Comparator<Task>> comparators;
    private DrawerLayout drawerLayout;
    private NavigationView filterView;
    private static EnvironmentFragment instance;

    SwipeRefreshLayout swipeRefresh;
    //On app start, if not logged in behave differently
    boolean loggedIn;

    //List of tasks loaded from db. Does not contain owned tasks of logged in user.
    List<Task> loadedTasks;
    ProgressBar pgsBar;
    //For opening specific detailtaskview (otherwise for loop would be necessary)
    HashMap<String, Task> allTasks;

    BottomNavigationView bottomMenu;
    LinearLayout noTasksfoundLayout;
    MaterialButton noTasksFoundAdjustRadius;
    Boolean dummyTasks;

    public EnvironmentFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment EnvironmentFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static EnvironmentFragment newInstance(String param1, String param2) {
        EnvironmentFragment fragment = new EnvironmentFragment();
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
        instance = this;
    }

    public static EnvironmentFragment getInstance() {
        return instance;
    }

    public void refreshList(){
        new Load_All_Tasks().execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_environment, container, false);
        this.mView = v;

        if (ParseUser.getCurrentUser() != null) {
            loggedIn = true;
        } else {
            loggedIn = false;
        }


        drawerLayout = v.findViewById(R.id.EnviromentDrawerLayout);
        filterView = v.findViewById(R.id.filterView);
        swipeRefresh = (SwipeRefreshLayout) v.findViewById(R.id.swipeRefresh);

        FloatingActionButton editLocationRadius = (FloatingActionButton) mView.findViewById(R.id.edit_location_radius);
        editLocationRadius.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startMapActivity();
            }
        });

        mapActivity = new Intent(getActivity(), MapActivity.class);
        //Get a loading spinner while loading data from server.
        pgsBar = (ProgressBar) v.findViewById(R.id.progressBar);
        Sprite doubleBounce = new DoubleBounce();
        pgsBar.setIndeterminateDrawable(doubleBounce);
        pgsBar.setVisibility(View.VISIBLE);

        dummyTasks = true;
        noTasksfoundLayout = mView.findViewById(R.id.noTasksFound);
        noTasksFoundAdjustRadius = mView.findViewById(R.id.EmptyTasksAdjustRadius);
        noTasksFoundAdjustRadius.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMapActivity();
            }
        });
        //PERMISSIONS
        Dexter.withContext(getActivity())
                .withPermissions(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                ).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
//                startLocationUpdates(); //location manager should start, after permissions are set, otherwise tasks cant be loaded
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {/* ... */}
        }).check();


        //Instantiate to prevent nullpointer
        loadedTasks = new ArrayList<Task>();
        allTasks = new HashMap<String, Task>();
        //Async call to load tasks. On success continue with everything else.
        new Load_All_Tasks().execute();


        mapActivity = new Intent(getActivity(), MapActivity.class);




        RecyclerView recyclerView = v.findViewById(R.id.environment_recycler_view);
        adapter = new EnvironmentAdapter(loadedTasks);
        recyclerView.setAdapter(adapter);

        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override

        public void onScrollStateChanged(RecyclerView recyclerView, int newState){
                try{
            boolean firstPos = recyclerView.getLayoutManager().isViewPartiallyVisible(recyclerView.getLayoutManager().getChildAt(0), true, false);
            if (firstPos) {
                swipeRefresh.setEnabled(true);
            } else {
                swipeRefresh.setEnabled(false);
            }
        }
                catch(Exception e) {
                    Log.println(Log.ERROR, "TAG", "Scroll Error : "+e.getLocalizedMessage());
                }
        }
        });



        SetupSorters(v);

        FloatingActionButton menuButton = (FloatingActionButton) mView.findViewById(R.id.menuButton);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!drawerLayout.isDrawerOpen(filterView)) {
                    drawerLayout.openDrawer(filterView);
                } else {
                    drawerLayout.closeDrawer(filterView);
                }
            }
        });

        MaterialButton signIn = mView.findViewById(R.id.signinFirstTime);
        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
            }
        });

        if (!loggedIn) {
            signIn.setVisibility(View.VISIBLE);
        } else {
            signIn.setVisibility(View.GONE);
        }

        pullRecentPreferences();
        return v;
    }

    /**
     * Puts every available task within max_radius into a list, which is then forwarded to mapactivity to show it
     *
     * @param tasks
     * @return
     */
    public ArrayList<Location> allTasksWithinMaxRadius(HashMap<String, Task> tasks) {
        ArrayList<Location> allTasksInRange = new ArrayList<>();
        if (recentLocation.getLatitude() != 0.00) {
            for (Task t : tasks.values()) {
                if (t.getLocation() != null) { // dont include tasks with no location
                    if (recentLocation.distanceTo(t.getLocation()) <= MAX_RADIUS) {
                        if (!t.hasValidAccepter()) {
                            Location taskLoc = t.getLocation();
                            allTasksInRange.add(taskLoc);
                        }
                    }
                }
            }
        }
        return allTasksInRange;
    }

    void SetupSorters(View v) {
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.categoriesFilter, R.layout.support_simple_spinner_dropdown_item);
        Spinner spinner = (Spinner) filterView.getMenu().findItem(R.id.KategorieMenuSpinner).getActionView();
        spinnerAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                adapter.applyFilter(parent.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        comparators = new HashMap<Integer, Comparator<Task>>();

        comparators.put(R.id.RButtonClosest, new Comparator<Task>() {
            @Override
            public int compare(Task o1, Task o2) {
                Integer d1 = (int) recentLocation.distanceTo(o1.getLocation());
                Integer d2 = (int) recentLocation.distanceTo(o2.getLocation());
                return d1.compareTo(d2);
            }
        });
        comparators.put(R.id.RButtonRecent, new Comparator<Task>() {
            @Override
            public int compare(Task o1, Task o2) {
                return o1.getCreatedAt().compareTo(o2.getCreatedAt());
            }
        });
        comparators.put(R.id.RButtonPoints, new Comparator<Task>() {
            @Override
            public int compare(Task o1, Task o2) {
                return o1.getPointValue() - o2.getPointValue();
            }
        });
        comparators.put(R.id.RButtonUrgent, new Comparator<Task>() {
            @Override
            public int compare(Task o1, Task o2) {
                return o1.getDueTime().compareTo(o2.getDueTime());
            }
        });


        for (Integer i : comparators.keySet()) {
            MenuItem mi = filterView.getMenu().findItem(i);
            RadioButton rb = (RadioButton) mi.getActionView();
            rb.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    ClearRadioButtons(v);
                    adapter.sortTasks(comparators.get(v.getId()));
                }
            });
        }


        CheckBox repeatingCb = (CheckBox) filterView.getMenu().findItem(R.id.repeatingFilterCheckbox).getActionView();
        repeatingCb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.applyFilter();
            }
        });
    }

    public void ClearRadioButtons(View v) {
        for (Integer i : comparators.keySet()) {
            MenuItem mi = filterView.getMenu().findItem(i);
            RadioButton rb = (RadioButton) mi.getActionView();
            if (v.getId() != rb.getId()) {
                rb.setChecked(false);
            }
        }
    }

    /**
     * Called when MapActivity is finished, receives set radius
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
                    if (data.getExtras().containsKey(getString(R.string.radiusParameter))) {//TODO TEST IF WORKS
                        radius = data.getIntExtra(getString(R.string.radiusParameter), 1000);
                        SharedPrefencesManager.getInstance(getActivity().getApplicationContext()).setRecentLocRadius(String.valueOf(radius));
                        forwardTasksWithinRadiusToRecyclerview();
                    }
//                Toast.makeText(this, String.valueOf(radius), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    //TODO https://stackoverflow.com/questions/27378981/how-to-use-searchview-in-toolbar-android
    // Search implementation for Recyclerview




    /**
     * RecyclerView Adapter for Environment Activities tasks
     */
    private class EnvironmentAdapter extends RecyclerView.Adapter<EnvironmentAdapter.ViewHolder> {
        private List<Task> environmentTasks;
        private List<Task> filteredTasks;
        private String filter = "Alle";
        private Comparator<Task> lastComparator;

        public Task getFromFiltered(int position) {
            return filteredTasks.get(position);
        }

        private View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!drawerLayout.isDrawerOpen(filterView)) {
                    Intent intent = new Intent(view.getContext(), AufgabenAnsichtActivity.class);
                    Task clickedTask = adapter.getFromFiltered((Integer) view.getTag());
                    Location store = clickedTask.getLocation();
                    //if needed, otherwise java.lang.NullPointerException: Attempt to invoke virtual method 'double android.location.Location.getLatitude()' on a null object reference
                    if (clickedTask.getLocation() != null) {
                        clickedTask.setLocationLat(String.valueOf(clickedTask.getLocation().getLatitude()));
                        clickedTask.setLocationLong(String.valueOf(clickedTask.getLocation().getLongitude()));
                        //location objects are not serializable
                        clickedTask.setLocation(null);
                    }
                    intent.putExtra("task", clickedTask);
//                owned tasks arent visible in list
//                if(clickedTask.getOwnerID().equals(ParseUser.getCurrentUser().getObjectId())){
//                    //Log.e("accesslevel", "owned");
//                    intent.putExtra(getString(R.string.accessLevelParameter), "OWNED");
//                }

                    //If a user is logged in
                    intent.putExtra(getString(R.string.accessLevelParameter), "FREE");
                    startActivity(intent);
                    clickedTask.setLocation(store);
                }
            }
        };

        /**
         * Applies the last filter to the Tasks
         */
        private void applyFilter() {
            applyFilter(filter);
        }

        /**
         * Applies the given filter for tasks in the adapter.
         * Also filters out accepted Tasks
         *
         * @param pFilter
         */
        private void applyFilter(String pFilter) {
            filter = pFilter;
            filteredTasks.clear();
            if (filter.equals("Alle")) {
                filteredTasks.addAll(environmentTasks);
            } else {
                for (Task t : environmentTasks) {
                    if (t.getCategory().equals(filter)) {
                        filteredTasks.add(t);
                    }
                }
            }
            CheckBox filtercb = (CheckBox) filterView.getMenu().findItem(R.id.repeatingFilterCheckbox).getActionView();
            ArrayList<Task> duplicate = new ArrayList<>(filteredTasks);
            for (Task t : duplicate) {
                if ((filtercb.isChecked() && !t.isWeekly()) || t.hasValidAccepter()) {
                    filteredTasks.remove(t);
                }
            }
            sortTasks();


            notifyDataSetChanged();
        }


        public EnvironmentAdapter(List<Task> environmentTasks) {
            this.environmentTasks = environmentTasks;
            this.filteredTasks = new ArrayList<Task>();
            this.filteredTasks.addAll(environmentTasks);
        }

        /**
         * Sorts the Tasks based on the last selected comparator
         */
        public void sortTasks() {
            if (lastComparator != null) {
                filteredTasks.sort(lastComparator);
            }
            notifyDataSetChanged();
        }

        /**
         * Sorts the tasks by given comparator
         *
         * @param comparator
         */
        public void sortTasks(Comparator<Task> comparator) {
            lastComparator = comparator;
            filteredTasks.sort(lastComparator);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public EnvironmentAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
            return new EnvironmentAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EnvironmentAdapter.ViewHolder holder, int position) {
            holder.taskName.setText(filteredTasks.get(position).getTaskName());
            holder.taskDescription.setText(filteredTasks.get(position).getDescription()); //TODO SHORTENED
            holder.taskDistance.setText(getDistanceAsString(recentLocation, filteredTasks.get(position).getLocation()));
            holder.taskDistance.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_location_on_24, 0, 0, 0);
            holder.clickableCard.setTag(position);
            holder.clickableCard.setOnClickListener(onClickListener);

            new Load_UserData(holder.ownerView).execute(filteredTasks.get(position).getOwnerID());

        }




        @Override
        public int getItemCount() {
            return filteredTasks.size();
        }

        public void update(List<Task> newTasks) {
            environmentTasks.clear();
            environmentTasks.addAll(newTasks);
            if (!dummyTasks) {
                if (newTasks.size() == 0) {
                    noTasksfoundLayout.setVisibility(View.VISIBLE);
                } else {
                    noTasksfoundLayout.setVisibility(View.GONE);
                }
            }
            applyFilter();
            notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView taskName;
            private TextView taskDescription;
            private TextView taskDistance;
            private MaterialCardView clickableCard;
            private ImageView ownerView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                taskName = itemView.findViewById(R.id.taskName);
                taskDescription = itemView.findViewById(R.id.taskDescription);
                taskDistance = itemView.findViewById(R.id.taskDistance);
                clickableCard = itemView.findViewById(R.id.card);
                ownerView = itemView.findViewById(R.id.showOwner);
            }
        }
    }

    /**
     * Calculate Distance between two locations and return Distance String as m or km
     *
     * @param location
     * @param taskLocation
     * @return
     */
    public String getDistanceAsString(Location location, Location taskLocation) {
        double distance = location.distanceTo(taskLocation);
        int distanceInMeters = Math.toIntExact(Math.round(distance));
        String result = Math.toIntExact(Math.round(distance)) + " m";
        if (distance >= 1000) {
            double res = distanceInMeters;
            DecimalFormat value = new DecimalFormat("#.#");
            result = value.format(res / 1000) + " km";
        }
        return result;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new EnvironmentFragment.Load_All_Tasks().execute();

            }
        });
    }

    private class Load_All_Tasks extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //TODO put progressbar for loading in xml
//            pgsBar.setVisibility(View.VISIBLE);
//            noTasksfoundLayout.setVisibility(View.GONE);
//            loadedTasks.clear();
        }

        protected Void doInBackground(Void... param) {
            publishProgress(param);
            loadedTasks.clear();
            allTasks.clear();
            //get owned Tasks
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Task");
            query.findInBackground(new FindCallback<ParseObject>() {
                public void done(List<ParseObject> taskList, ParseException e) {
                    Log.d("tasksfound", "" + taskList.size());
                    if (e == null) {
                        for (ParseObject t : taskList) {
                            Task task = new Task();
                            task.setTaskName(t.getString(getString(R.string.keyTaskName)));
                            task.setCompleted(t.getBoolean(getString(R.string.keyCompleted)));
                            task.setID(t.getObjectId());
                            ParseUser owner = (ParseUser) t.get(getString(R.string.keyOwner));
                            task.setOwnerID(owner.getObjectId());
                            task.setAccepterID(t.getString(getString(R.string.keyAccepter)));
                            //Location
                            Location location = new Location("");
                            location.setLatitude(t.getDouble(getString(R.string.keyLocationLat)));
                            location.setLongitude(t.getDouble(getString(R.string.keyLocationLong)));
                            task.setLocation(location);
                            task.setLocationName(t.getString(getString(R.string.keyLocationName)));
                            task.setDescription(t.getString(getString(R.string.keyDescription)));
                            task.setAdditionalInfo(t.getString(getString(R.string.keyAdditionalInfo)));
                            task.setWeekly(t.getBoolean(getString(R.string.keyWeekly)));
                            task.setSetPointValue(t.getInt(getString(R.string.keyPointValue)));
                            task.setCategory(t.getString(getString(R.string.keyCategory)));
                            //If category is missing (likely because of old data), replace it with default
                            if(task.getCategory().equals("none")){
                                task.setCategory(getResources().getStringArray(R.array.categoriesCreation)[0]);
                            }
                            //Time of creation
                            task.setCreatedAt(t.getCreatedAt());

                            //TODO Implement due time, when ready on frontend
                            task.setDueTime(t.getDate(getString(R.string.keyDue)));

                            //Dont add own tasks into this list. If User is logged in
                            try {
                                //Should throw exception if no parseuser available
                                if (!task.getOwnerID().equals(ParseUser.getCurrentUser().getObjectId())) {
//                                    if (!allTasks.isEmpty()) {
                                    if (!allTasks.containsKey(t.getObjectId())) {// we need this so tasks wont be loaded several times into list
                                        loadedTasks.add(task);
                                        allTasks.put(t.getObjectId(), task);
                                    }
//                                    }
//                                    else{
//                                        loadedTasks.add(task);
//                                        allTasks.put(t.getObjectId(), task);
//                                    }
                                }
                            } catch (Exception parseex) {
                                //Show all tasks
                                if (!allTasks.containsKey(t.getObjectId())) {// we need this so tasks wont be loaded several times into list
                                    loadedTasks.add(task);
                                    allTasks.put(t.getObjectId(), task);
                                }
                            }


                        }
                        Log.d("loadedTasks", "" + loadedTasks.size());
                        onPostExecute("");
                    } else {
                        Log.e("ParseException", "Error: " + e.getMessage());
                    }
                }
            });

            return null;
        }

        protected void onPostExecute(String arg) {
            dummyTasks = false;
            pullRecentPreferences();
            swipeRefresh.setRefreshing(false);
            adapter.applyFilter();
        }
    }

    private class Load_UserData extends AsyncTask<String, String, Void> {
        private ImageView pic;

        public Load_UserData(ImageView viewToSetPictureTo) {
            pic = viewToSetPictureTo;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
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

                        //Image
                        ParseFile profilePic = foundUser.getParseFile(getString(R.string.uKeyProfileImage));
                        byte[] byteArray = new byte[0];

                        try {
                            byteArray = profilePic.getData();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        Bitmap imageBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length); //Bitmap with profile picture
                        if (imageBitmap != null && imageBitmap.getByteCount() > 0) {
                            Glide.with(getActivity().getApplicationContext())
                                    .load(imageBitmap)
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_baseline_account_circle_24)
                                    .into(pic);
//                            pic.setImageBitmap(imageBitmap);
                        } else {
                            pic.setImageDrawable(getActivity().getDrawable(R.drawable.ic_baseline_account_circle_24_grey));
                        }


                    } else {
                        // Something went wrong.
                    }
                }
            });
            return null;
        }
    }

    /**
     * Called more than once to start mapactivity
     */
    public void startMapActivity() {
        ArrayList<Location> allTasksExtra = allTasksWithinMaxRadius(allTasks);
        mapActivity.putExtra("location", recentLocation); //TODO not needed if central locationamanger
        mapActivity.putExtra("recentRadius", radius); //keep before selected radius
        mapActivity.putParcelableArrayListExtra("allTasks", allTasksExtra);
        startActivityForResult(mapActivity, 1);
    }

    /**
     * gets recent Prefences from sharedPreferencesManager
     * Location is provided by mainactivity and saved in it
     */
    public void pullRecentPreferences() {
        String[] sharedRecentLocation = SharedPrefencesManager.getInstance(getActivity().getApplicationContext()).getRecentLocArray();

        if (sharedRecentLocation[0] != null) {
            recentLocation.setLatitude(Double.parseDouble(sharedRecentLocation[0]));
            recentLocation.setLongitude(Double.parseDouble(sharedRecentLocation[1]));
            forwardTasksWithinRadiusToRecyclerview(); //show values in recyclerview
        }

        String sharedRecentRadius = SharedPrefencesManager.getInstance(getActivity().getApplicationContext()).getRecentLocRadius();

        if (sharedRecentRadius != null) {
            radius = parseInt(sharedRecentRadius);
            if (radius == 0) {
                radius = 10000;
            }
        }
    }

    /**
     * Fill RecyclerView Tasks after new radius or new data was set in MapActivity
     */
    public void forwardTasksWithinRadiusToRecyclerview() {
        List<Task> tasksInRadius = new ArrayList<Task>();

        // fill recyclerview if valid user location is found
        if (recentLocation.getLatitude() != 0.00) {
            if (!dummyTasks && pgsBar.isShown()) {
                //hide loading bar when recyclerview contains valid data
                pgsBar.setVisibility(View.GONE);
            }
            for (Task t : allTasks.values()) {
                if (t.getLocation() != null) { // dont include tasks with no location
                    if (recentLocation.distanceTo(t.getLocation()) <= radius) {
                        tasksInRadius.add(t);
                    }
                }
            }
            //when no tasks are visible, increase radius to maximum
            if (tasksInRadius.size() == 0) {
                //when no tasks are in range and radius is at maximum show no tasks available
                if (!dummyTasks) {
                    noTasksfoundLayout.setVisibility(View.VISIBLE);
                    noTasksFoundAdjustRadius.setVisibility(View.VISIBLE);
                }

            } else {
                //when notasks available is shown hide it, because tasksize is greater than 0
                if (noTasksfoundLayout.isShown()) {
                    noTasksfoundLayout.setVisibility(View.GONE);
                    noTasksFoundAdjustRadius.setVisibility(View.GONE);
                }
            }
            adapter.update(tasksInRadius);
        }
    }
}