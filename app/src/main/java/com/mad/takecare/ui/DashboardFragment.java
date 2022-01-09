package com.mad.takecare.ui;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.DoubleBounce;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mad.takecare.MainActivity;
import com.mad.takecare.R;
import com.mad.takecare.model.ParseDB;
import com.mad.takecare.model.Task;
import com.mad.takecare.model.User;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DashboardFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static DashboardFragment instance = null;
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    ProgressBar pgsBar;
    //For adapter
    List<Task> ownedTasks;
    //For adapter
    List<Task> acceptedTasks;
    //For intent
    HashMap<String, Task> allTasks;
    ParseDB database;
    AcceptedTaskAdapter ata;
    OwnedTaskAdapter ota;
    User currentLoggedInUser;

    RecyclerView acceptedTasksView;
    RecyclerView.LayoutManager acceptedLayoutManager;
    RecyclerView ownedTasksView;
    RecyclerView.LayoutManager ownedLayoutManager;

    SwipeRefreshLayout swipeRefresh;

    protected View mView;

    public DashboardFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment DashboardFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DashboardFragment newInstance(String param1, String param2) {
        DashboardFragment fragment = new DashboardFragment();
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
        instance = this;
        setHasOptionsMenu(true);
    }
    public static DashboardFragment getInstance() {
        return instance;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);
        this.mView = v;

        currentLoggedInUser = new User();
        currentLoggedInUser.setObjectId(ParseUser.getCurrentUser().getObjectId());
        currentLoggedInUser.setUsrname(ParseUser.getCurrentUser().getUsername());
        swipeRefresh = (SwipeRefreshLayout)v.findViewById(R.id.swipeRefresh);

        //Access database functionality
        //Cant get it working async way with android threads, so functionality is in same activity
        //database = new ParseDB();

        //Get a loading spinner while loading data from server.
        pgsBar = (ProgressBar) v.findViewById(R.id.progressBar);
        Sprite doubleBounce = new DoubleBounce();
        pgsBar.setIndeterminateDrawable(doubleBounce);

        FloatingActionButton newTaskButton = (FloatingActionButton) mView.findViewById(R.id.newTaskButton);
        newTaskButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), AufgabenAnsichtActivity.class);
                intent.putExtra("task", new Task());
                intent.putExtra(getString(R.string.accessLevelParameter), "NEW");
                startActivity(intent);
            }
        });

        ownedTasks = new ArrayList<Task>();
        acceptedTasks = new ArrayList<Task>();
        allTasks = new HashMap<String, Task>();

        new DashboardFragment.Load_Accepted().execute();
        new DashboardFragment.Load_Owned().execute();

        //Setup Accepted Tasks View
        acceptedTasksView = v.findViewById(R.id.AcceptedTasks);
        ata = new DashboardFragment.AcceptedTaskAdapter(acceptedTasks);
        acceptedLayoutManager = new LinearLayoutManager(getContext());
        acceptedTasksView.setLayoutManager(acceptedLayoutManager);
        acceptedTasksView.setAdapter(ata);
        //Setup Owned Tasks View
        ownedTasksView = v.findViewById(R.id.OwnedTasks);
        ota = new DashboardFragment.OwnedTaskAdapter(ownedTasks);
        ownedLayoutManager = new LinearLayoutManager(getContext());
        ownedTasksView.setLayoutManager(ownedLayoutManager);
        ownedTasksView.setAdapter(ota);

        TextView successDisplay = v.findViewById(R.id.CompletedDisplay);
        successDisplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setVisibility(View.GONE);
            }
        });
        return v;
    }



    /**
     * Loads owned&accepted Tasks and updates the adapters
     */
    public  void refreshLists(){
        new DashboardFragment.Load_Accepted().execute();
        new DashboardFragment.Load_Owned().execute();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new DashboardFragment.Load_Accepted().execute();
                new DashboardFragment.Load_Owned().execute();
            }
        });
    }

    /**
     * Loads the owned tasks and updates the adapter with them
     */
    private class Load_Owned extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pgsBar.setVisibility(View.VISIBLE);
            ownedTasks.clear();
        }

        protected Void doInBackground(Void... param) {
            publishProgress(param);

            //get owned Tasks
            ParseQuery<ParseObject> taskQuery = ParseQuery.getQuery("Task");
            taskQuery.whereEqualTo(getString(R.string.keyOwner), ParseUser.getCurrentUser());

            taskQuery.findInBackground(new FindCallback<ParseObject>() {
                public void done(List<ParseObject> taskList, ParseException e) {
                    for (ParseObject t:taskList) {
                        Task task = new Task();
                        task.setTaskName(t.getString(getString(R.string.keyTaskName)));
                        task.setCompleted(t.getBoolean(getString(R.string.keyCompleted)));
                        task.setCompletedMarker(t.getBoolean(getString(R.string.keyCompletedMarker)));
                        task.setID(t.getObjectId());
                        task.setOwnerID(ParseUser.getCurrentUser().getObjectId());
                        task.setAccepterID(t.getString(getString(R.string.keyAccepter)));
                        task.setDescription(t.getString(getString(R.string.keyDescription)));
                        task.setAdditionalInfo(t.getString(getString(R.string.keyAdditionalInfo)));
                        task.setWeekly(t.getBoolean(getString(R.string.keyWeekly)));
                        task.setLocationLat(String.valueOf(t.getDouble(getString(R.string.keyLocationLat))));
                        task.setLocationLong(String.valueOf(t.getDouble(getString(R.string.keyLocationLong))));
                        task.setLocationName(t.getString(getString(R.string.keyLocationName)));
                        task.setCategory(t.getString(getString(R.string.keyCategory)));
                        task.setDueTime(t.getDate(getString(R.string.keyDue)));
                        task.setCreatedAt(t.getCreatedAt());
                        task.setSetPointValue(t.getInt(getString(R.string.keyPointValue)));
                        ownedTasks.add(task);
                        allTasks.put(t.getObjectId(), task);
                    }
                    onPostExecute("done owned");
                }
            });

            return null;
        }

        protected void onPostExecute(String arg) {
            pgsBar.setVisibility(View.GONE);
            TextView ownedTasksEmpty = getView().findViewById(R.id.OwnedTasksEmpty);

            if(ownedTasks.size() == 0){

                ownedTasksEmpty.setVisibility(View.VISIBLE);
            }
            ota.notifyDataSetChanged();
            swipeRefresh.setRefreshing(false);
        }
    }


    /**
     * Loads the accepted Tasks and updates the adapter with them
     */
    private class Load_Accepted extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pgsBar.setVisibility(View.VISIBLE);
            acceptedTasks.clear();
        }

        protected Void doInBackground(Void... param) {
            publishProgress(param);

            final User currentLoggedInUser = new User();
            currentLoggedInUser.setObjectId(ParseUser.getCurrentUser().getObjectId());
            currentLoggedInUser.setUsrname(ParseUser.getCurrentUser().getUsername());

            //get accepted Tasks
            ParseQuery<ParseObject> taskQuery = ParseQuery.getQuery("Task");
            taskQuery.whereEqualTo(getString(R.string.keyAccepter), ParseUser.getCurrentUser().getObjectId());

            taskQuery.findInBackground(new FindCallback<ParseObject>() {
                public void done(List<ParseObject> taskList, ParseException e) {
                    for (ParseObject t:taskList) {
                        Task task = new Task();
                        task.setTaskName(t.getString(getString(R.string.keyTaskName)));
                        task.setCompleted(t.getBoolean(getString(R.string.keyCompleted)));
                        task.setID(t.getObjectId());
                        task.setCompletedMarker(t.getBoolean(getString(R.string.keyCompletedMarker)));
                        ParseUser owner = (ParseUser)t.get(getString(R.string.keyOwner));
                        task.setOwnerID(owner.getObjectId());
                        task.setAccepterID(ParseUser.getCurrentUser().getObjectId());
                        task.setDescription(t.getString(getString(R.string.keyDescription)));
                        task.setAdditionalInfo(t.getString(getString(R.string.keyAdditionalInfo)));
                        task.setWeekly(t.getBoolean(getString(R.string.keyWeekly)));
                        task.setLocationLat(String.valueOf(t.getDouble(getString(R.string.keyLocationLat))));
                        task.setLocationLong(String.valueOf(t.getDouble(getString(R.string.keyLocationLong))));
                        task.setLocationName(t.getString(getString(R.string.keyLocationName)));
                        task.setDueTime(t.getDate(getString(R.string.keyDue)));
                        task.setCreatedAt(t.getCreatedAt());
                        task.setSetPointValue(t.getInt(getString(R.string.keyPointValue)));
                        acceptedTasks.add(task);
                        allTasks.put(t.getObjectId(), task);
                        currentLoggedInUser.setTaskCount(acceptedTasks.size());
                    }
                    ata.update(acceptedTasks); //Im aware this is redundant, but it triggers the display for completedTasks
                    onPostExecute("done accepted");
                }
            });

            return null;
        }

        protected void onPostExecute(String arg) {
            pgsBar.setVisibility(View.GONE);
            TextView acceptedTasksEmpty = getView().findViewById(R.id.AcceptedTasksEmpty);
            if(acceptedTasks.size() == 0){
                acceptedTasksEmpty.setVisibility(View.VISIBLE);
            }else{
                acceptedTasksEmpty.setVisibility(View.GONE);
            }
            ata.notifyDataSetChanged();
        }
    }

    /**
     * Deletes the given Task from the Database
     * @param pTask The Task to be deleted
     */
    public void DeleteTask(Task pTask){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Task");
        //Set on Task
        query.getInBackground(pTask.getID(), new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                object.deleteInBackground();
            }
        });
    }

    /**
     * Adds the given amount of XP to the users Database entry.
     * Does not update Level.
     * @param addedExperience
     */
    public void UpdateUserEXP(int addedExperience){
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo(getString(R.string.uKeyID), ParseUser.getCurrentUser().getObjectId());
        query.findInBackground(new FindCallback<ParseUser>() {
            //response is a list, but can only be one user, so take first
            public void done(List<ParseUser> users, ParseException e) {
                if (e == null) {
                    // The query was successful.
                    ParseUser owner = users.get(0);
                    int oldXP = owner.getInt(getString(R.string.uKeyEXP));
                    owner.put(getString(R.string.uKeyEXP), oldXP + addedExperience);
                    owner.saveInBackground();
                } else {
                    // Something went wrong.
                }
            }
        });
    }


    /**
     * The adapter for Accepted Tasks
     */
    private class AcceptedTaskAdapter extends RecyclerView.Adapter<AcceptedTaskAdapter.ViewHolder> {

        private List<Task> acceptedTasks;

        public AcceptedTaskAdapter(List<Task> pAcceptedTasks) {
            update(pAcceptedTasks);
        }

        @NonNull
        @Override
        public AcceptedTaskAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.accepted_dashboard_entry, parent, false);
            return new AcceptedTaskAdapter.ViewHolder(view);
        }

        /**
         * Opens the AufgabenAnsicht for this view
         */
        public class MoveToAufgabenAnsicht implements View.OnClickListener{

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), AufgabenAnsichtActivity.class);
                Task clickedTask = allTasks.get(view.getTag());
                intent.putExtra("task", clickedTask);
                intent.putExtra(getString(R.string.accessLevelParameter), "ACCEPTED");
                startActivity(intent);
            }
        }



        @Override
        public void onBindViewHolder(@NonNull AcceptedTaskAdapter.ViewHolder holder, int position) {
            holder.text.setText(acceptedTasks.get(position).getTaskName());
            holder.clickableCard.setTag(acceptedTasks.get(position).getID());
            //holder.itemView.setTag(acceptedTasks.get(position).getID());
            holder.clickableCard.setOnClickListener(new AcceptedTaskAdapter.MoveToAufgabenAnsicht());
        }

        @Override
        public int getItemCount() {
            return acceptedTasks.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            public TextView text;
            private MaterialCardView clickableCard;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                clickableCard = itemView.findViewById(R.id.card);
                text = itemView.findViewById(R.id.taskTitleAccepted);
            }
        }

        public void update(List<Task> pAcceptedTasks){
            List<Task> completedTasks = pAcceptedTasks.stream().filter(x -> x.isCompleted()).collect(Collectors.toList());
            if(completedTasks.size() > 0){
                int XPsum = completedTasks.stream().mapToInt(x -> x.getPointValue()).sum();
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                View view =  LayoutInflater.from(getActivity()).inflate(R.layout.alert_dialog_view, null);
                TextView textView = view.findViewById(R.id.AlertDialogView);
                  textView.setText("Du hast " + completedTasks.size() + " Aufgabe(n) abgeschlossen und " +
                          XPsum + " EXP erhalten.");
                AlertDialog dialog = builder.create();
                dialog.setView(view);
                dialog.getWindow().setBackgroundDrawable(getActivity().getDrawable(R.drawable.roundedcorners));
                dialog.show();

                for(Task t : completedTasks){
                    DeleteTask(t);
                }
                pAcceptedTasks.removeAll(completedTasks);
                UpdateUserEXP(XPsum);
            }
            acceptedTasks = pAcceptedTasks;
        }
    }

    /**
     * The Adapter for Owned Tasks
     */
    private class OwnedTaskAdapter extends RecyclerView.Adapter<OwnedTaskAdapter.ViewHolder>{

        public List<Task> ownedTasks;

        public OwnedTaskAdapter(List<Task> pOwnedTasks){
            ownedTasks = pOwnedTasks;
        }


        /**
         * Opens the AufgabenAnsicht for this view
         */
        public class MoveToAufgabenAnsicht implements View.OnClickListener{

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), AufgabenAnsichtActivity.class);

                Task clickedTask = allTasks.get(view.getTag());
                intent.putExtra("task", clickedTask);
                intent.putExtra(getString(R.string.accessLevelParameter), "OWNED");
                startActivity(intent);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.owned_dashboard_entry, parent, false);
            return new OwnedTaskAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Task t = ownedTasks.get(position);
            holder.text.setText(t.getTaskName());
            holder.checkBox.setChecked(t.isCompletedMarker());
            holder.clickableTask.setTag(t.getID());
            holder.clickableTask.setOnClickListener(new OwnedTaskAdapter.MoveToAufgabenAnsicht());
            if(!t.hasValidAccepter()){
                holder.accepterView.setEnabled(false);
                holder.accepterView.setAlpha(0);
            } else {
                holder.accepterView.setEnabled(true);
                //Set image of accepter
                new Load_UserData(holder.accepterView).execute(t.getAccepterID());

                holder.accepterView.setAlpha(255);
                holder.accepterView.setOnClickListener(new View.OnClickListener(){

                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        intent.putExtra(getString(R.string.usernameParameter), ParseUser.getCurrentUser().getObjectId());
                        intent.putExtra(getString(R.string.viewedUserParameter), t.getAccepterID());
                        intent.putExtra(getString(R.string.fragmentPositionParameter), 3);
                        //intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(intent);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return ownedTasks.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            public CheckBox checkBox;
            public TextView text;
            public ImageView accepterView;
            public View clickableTask;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                checkBox = itemView.findViewById(R.id.finishedCheckbox);
                text = itemView.findViewById(R.id.taskTitleOwned);
                accepterView = itemView.findViewById(R.id.showAccepter);
                clickableTask = itemView.findViewById(R.id.card);
            }
        }
    }

    private class Load_UserData extends AsyncTask<String, String, Void> {
        private ImageView pic;
        public Load_UserData(ImageView viewToSetPictureTo){
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
                        if(imageBitmap != null  && imageBitmap.getByteCount() > 0){
//                            pic.setImageBitmap(imageBitmap);
                            Glide.with(getActivity().getApplicationContext())
                                    .load(imageBitmap)
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_baseline_account_circle_24)
                                    .into(pic);
                        }
                        else{
                            pic.setImageDrawable(getActivity().getDrawable(R.drawable.ic_baseline_account_circle_24));
                        }


                    } else {
                        // Something went wrong.
                    }
                }
            });
            return null;
        }
    }
}