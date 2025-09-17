package com.rasel.server.db;

import java.util.ArrayList;


/**
    manage database connection, retrieval, pools, data, etc.

    **note**: right now, for simplicity, the current implementation is not using a database, it store data inside variables ( memory ),
    I will implement database logic later,
*/
public class DatabaseManager {
    public static UserManager userManager = new UserManager();
    public static GroupManager groupManager = new GroupManager();
}