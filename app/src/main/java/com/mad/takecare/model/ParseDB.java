package com.mad.takecare.model;

import android.util.Log;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

//To access data from parse server. The parse server uses a mongodb database.
///////////DEPRECATED WONT WORK THIS WAY DUE TO ASYNC.´-> Change to AsyncTasks
public class ParseDB {
//TODO DELETE THIS CLASS IF NOT USED, Insert needed functionalities in activites themself
    public User GetUser(String id){
        final User user;
        user = new User();
        //Async request to db
        ParseQuery<ParseObject> query = ParseQuery.getQuery("User");
        query.getInBackground(id, new GetCallback<ParseObject>() {
            public void done(ParseObject userObject, ParseException e) {
                if (e == null) {
                    user.setObjectId(userObject.getObjectId());
                    user.setUsrname(userObject.getString("name"));
                    //if needed get all the tasks of the owner too.
                } else {
                    Log.e("ParseException", "Error: " + e.getMessage());
                }
            }
        });
        return user;
    }

    public List<User> GetAllUsers() {
        final List<User> result = new ArrayList<User>();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("User");
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> userList, ParseException e) {
                if (e == null) {
                    for (ParseObject u:userList) {
                        User user = new User();
                        user.setUsrname(u.getString("name"));

                        result.add(user);
                        //Only use name and id for users, not all tasks are needed here
                    }
                } else {
                    Log.e("ParseException", "Error: " + e.getMessage());
                }
            }
        });

        return result;
    }

    public List<Task> GetAllTasks() {
        final List<Task> result = new ArrayList<Task>();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Task");
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> taskList, ParseException e) {
                if (e == null) {
                    for (ParseObject t:taskList) {
                        Task task = new Task();
                        task.setTaskName(t.getString("taskName"));
                        task.setCompleted(t.getBoolean("completed"));
                        task.setID(t.getObjectId());
                        //task.setOwner(GetOwner(t.getObjectId()));
                        result.add(task);
                    }
                } else {
                    Log.e("ParseException", "Error: " + e.getMessage());
                }
            }
        });

        return result;
    }

    public List<Task> GetAllOwnedTasksOfActiveUser() {
        final List<Task> result = new ArrayList<Task>();

        ParseQuery<ParseObject> taskQuery = ParseQuery.getQuery("Task");
        taskQuery.whereEqualTo("owner", ParseUser.getCurrentUser());

        taskQuery.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> taskList, ParseException e) {
                for (ParseObject t:taskList) {
                    Task task = new Task();
                    task.setTaskName(t.getString("taskName"));
                    task.setCompleted(t.getBoolean("completed"));
                    task.setID(t.getObjectId());
                    //task.setOwner(GetOwner(t.getObjectId()));
                    result.add(task);
                }
            }
        });
        return result;
    }

    public List<Task> GetAllAcceptedTasksOfActiveUser() {
        final List<Task> result = new ArrayList<Task>();

        ParseQuery<ParseObject> taskQuery = ParseQuery.getQuery("Task");
        taskQuery.whereEqualTo("accepter", ParseUser.getCurrentUser());

        taskQuery.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> taskList, ParseException e) {
                for (ParseObject t:taskList) {
                    Task task = new Task();
                    task.setTaskName(t.getString("taskName"));
                    task.setCompleted(t.getBoolean("completed"));
                    task.setID(t.getObjectId());
                    //task.setOwner(GetOwner(t.getObjectId()));
                    result.add(task);
                }
            }
        });

        return result;
    }

    public Task GetTask(String taskID) {
        final Task tsk;
        tsk = new Task();

        //Async request to db
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Task");
        query.getInBackground(taskID, new GetCallback<ParseObject>() {
            public void done(ParseObject object, ParseException e) {
                if (e == null) {
                    tsk.setID(object.getObjectId());
                    tsk.setTaskName(object.getString("taskName"));
                    tsk.setCompleted(object.getBoolean("completed"));
                    //Get owner from DB
                    User owner = GetOwner(tsk.getID());
                    //tsk.setOwner(owner);
                } else {
                    Log.e("ParseException", "Error: " + e.getMessage());
                }
            }
        });

        return tsk;
    }

    public void SaveTask(Task task) {
        //Save Task itself
        final ParseObject taskObject = new ParseObject("Task");
        taskObject.put("taskName", task.taskName);
        //taskObject.put("id", task.ID);
        taskObject.put("completed", false);
        //Create a relation, every task has an owner.
        taskObject.put("owner", ParseUser.getCurrentUser());
        taskObject.put("accepter", "none");
        taskObject.saveInBackground();

/*        //Save relation in user
        ParseQuery<ParseObject> query = ParseQuery.getQuery("User");
        query.getInBackground(owner.objectId, new GetCallback<ParseObject>() {
            public void done(ParseObject userObject, ParseException e) {
                if (e == null) {
                    userObject.put("ownedTasks", ParseObject.createWithoutData("Task", taskObject.getObjectId()));
                } else {
                    Log.e("ParseException", "Error: " + e.getMessage());
                }
            }
        });*/
    }

    public void AcceptTask(final Task task) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Task");
        //Set on Task
        query.getInBackground(task.ID, new GetCallback<ParseObject>() {
            public void done(ParseObject task, ParseException e) {
                if (e == null) {
                    // Now let's update it with some new data. In this case, only cheatMode and score
                    // will get sent to your Parse Server. playerName hasn't changed.
                    task.put("accepter", ParseUser.getCurrentUser());
                    task.saveInBackground();
                } else {
                    Log.e("ParseException", "Error: " + e.getMessage());
                }
            }
        });

     /*   //Set in user
        ParseQuery<ParseObject> queryUser = ParseQuery.getQuery("User");
        queryUser.getInBackground(accepter.objectId, new GetCallback<ParseObject>() {
            public void done(ParseObject user, ParseException e) {
                if (e == null) {
                    // Now let's update it with some new data. In this case, only cheatMode and score
                    // will get sent to your Parse Server. playerName hasn't changed.
                    user.put("acceptedTasks", ParseObject.createWithoutData("Task", task.ID));
                    user.saveInBackground();
                } else {
                    Log.e("ParseException", "Error: " + e.getMessage());
                }
            }
        });*/
    }

    //I dont know when to use GetOwner with task, so only half implemented. The owner contains only his id and name
    public User GetOwner(Task task) {
        final User owner;
        owner = new User();
        //Async request to db
        ParseQuery<ParseObject> query = ParseQuery.getQuery("User");
        query.getInBackground(task.getOwnerID(), new GetCallback<ParseObject>() {
            public void done(ParseObject object, ParseException e) {
                if (e == null) {
                    owner.setObjectId(object.getObjectId());
                    owner.setUsrname(object.getString("name"));
                    //if needed get all the tasks of the owner too.
                } else {
                    Log.e("ParseException", "Error: " + e.getMessage());
                }
            }
        });
        return owner;
    }

    public User GetOwner(String id) {
        final User owner;
        owner = new User();
        //Async request to db
        ParseQuery<ParseObject> query = ParseQuery.getQuery("User");
        query.getInBackground(id, new GetCallback<ParseObject>() {
            public void done(ParseObject object, ParseException e) {
                if (e == null) {
                    owner.setObjectId(object.getObjectId());
                    owner.setUsrname(object.getString("name"));
                    //if needed get all the tasks of the owner too.
                } else {
                    Log.e("ParseException", "Error: " + e.getMessage());
                }
            }
        });
        return owner;
    }

    public User GetAccepter(String taskID) {
        User usr = new User();
        usr.usrname = taskID + "Accepter";
        return usr;
    }

    public User GetAccepter(Task task) {
        User usr = new User();
        usr.usrname = task.taskName + "Accepter";
        return usr;
    }
}
