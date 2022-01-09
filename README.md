# TakeCare
![](app/src/main/ic_launcher-playstore.png)

TakeCare was a group course project developed with two other developers, which allowed me to publish the code here. The general idea was to help in the early stages of the pandemic.

Mobile App Development 2020 - University Of Bremen


# Description
TakeCare is an Android application for simplifying and supporting social engagement.
engagement. To do this, the app allows you to create and accept tasks in different
different categories. These categories include such items as children, animals and
Shopping. Users can filter and sort tasks according to various criteria.
If they find a task, it can be easily accepted. As soon as the creator
of a task confirms the fulfillment of this, the fulfiller gets some experience points.
experience points. The level of a user increases with enough experience points. Levels count as prestige on the one hand, on the other hand the number of tasks which can be accepted by a user at the same time is limited by the level. The higher the
the level, the more tasks can be accepted.

# Build
Build the apk within Android Studio.
The min. API level is 26.

(last tested Summer 2020)

# Server
The provided docker server configuration uses WiredTiger 3.3.0 (https://github.com/wiredtiger/wiredtiger), please update its version, when using it in production.

```
install docker on your system
start terminal in this folder
execute: 
docker-compose up

you can access the dashboard via http://localhost:4040/
the server will be reached via http://localhost:1337/

you can change the configuration in the docker-compose.yml
```

# Promo video
https://youtu.be/5GyrIGgYysE
