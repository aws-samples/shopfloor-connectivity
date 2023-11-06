
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.sql.config


// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0



import com.google.gson.annotations.SerializedName

enum class DbServerType(val driverClassName: String, val connectString: String) {

    @SerializedName("postgresql")
    POSTGRES("org.postgresql.Driver", "jdbc:postgresql://\$host:\$port/\$dbName"),

    @SerializedName("mariadb")
    MARIADB("org.mariadb.jdbc.Driver", "jdbc:mariadb://\$host:\$port/\$dbName"),

    @SerializedName("sqlserver")
    SQLSERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://\$host:\$port;database=\$dbName;encrypt=true;trustServerCertificate=true;"),

    @SerializedName("mysql")
    MYSQL("com.mysql.cj.jdbc.Driver", "jdbc:mysql://\$host:\$port/\$dbName"),

    @SerializedName("oracle")
    ORACLE("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@\$host:\$port:\$dbName");

    override fun toString(): String {
        return when (this) {
            POSTGRES -> "postgresql"
            MARIADB -> "mariadb"
            SQLSERVER -> "sqlserver"
            ORACLE -> "oracle"
            MYSQL -> "mysql"
        }
    }

}

