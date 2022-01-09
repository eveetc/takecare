package com.mad.takecare.model;

import java.util.List;

public interface IDatabase {

    public User GetUser(String username);
    public List<User> GetAllUsers();
    public List<Task> GetAllTasks();
    public Task GetTask(String taskID);
    public void SaveTask(Task task);
    public User GetOwner(Task task);
    public User GetAccepter(Task task);
    public User GetOwner(String taskID);
    public User GetAccepter(String taskID);

}
